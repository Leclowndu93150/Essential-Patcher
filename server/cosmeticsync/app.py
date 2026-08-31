import asyncio
import json
import os
import time
from contextlib import asynccontextmanager
from pathlib import Path
from uuid import UUID

from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.responses import PlainTextResponse, StreamingResponse
from pydantic import BaseModel, Field

from .auth import caller_uuid, issue_jwt
from .config import Settings
from .metrics_client import Metrics
from .ratelimit import IpRateLimiter
from .sessions import SessionHub
from .slots import KNOWN_SLOTS
from .storage import Storage


def _client_ip(request: Request) -> str:
    fwd = request.headers.get("x-forwarded-for")
    if fwd:
        return fwd.split(",")[0].strip()
    return request.client.host if request.client else ""


@asynccontextmanager
async def lifespan(app: FastAPI):
    env_path = os.getenv("COSMETICSYNC_ENV", "/opt/cosmetics-sync/.env")
    if Path(env_path).exists():
        load_dotenv(env_path)
    cfg = Settings.load()
    storage = Storage(cfg.db_path)
    writes = IpRateLimiter(cfg.write_per_minute)
    auth_limiter = IpRateLimiter(cfg.auth_per_minute)
    hub = SessionHub(cfg.session_heartbeat_timeout_s, cfg.max_players_per_session)
    metrics = None
    if cfg.panel_endpoint and cfg.panel_hmac_key:
        metrics = Metrics(endpoint=cfg.panel_endpoint, service="cosmetics", hmac_key=cfg.panel_hmac_key)
        metrics.start()

    app.state.cfg = cfg
    app.state.storage = storage
    app.state.writes = writes
    app.state.auth_limiter = auth_limiter
    app.state.hub = hub
    app.state.metrics = metrics

    stop = asyncio.Event()

    async def session_sweep_loop():
        ticks = 0
        while not stop.is_set():
            try:
                evicted = await hub.sweep()
                for sid, uuid in evicted:
                    await hub.publish(sid, {"type": "player_left", "uuid": str(uuid)})
            except Exception as e:
                print(f"[session-sweep] {e}", flush=True)
            if ticks % 60 == 0:
                try:
                    cutoff = int(time.time()) - cfg.player_inactive_days * 86400
                    removed = storage.purge_inactive(cutoff)
                    if removed:
                        print(f"[player-purge] removed {removed} inactive players", flush=True)
                except Exception as e:
                    print(f"[player-purge] {e}", flush=True)
            ticks += 1
            try:
                await asyncio.wait_for(stop.wait(), timeout=60)
            except asyncio.TimeoutError:
                pass

    prune_task = asyncio.create_task(session_sweep_loop())
    try:
        yield
    finally:
        stop.set()
        prune_task.cancel()
        try:
            await prune_task
        except asyncio.CancelledError:
            pass
        if metrics:
            metrics.stop()


app = FastAPI(lifespan=lifespan, openapi_url=None, docs_url=None, redoc_url=None)


class AuthLoginRequest(BaseModel):
    uuid: str | None = Field(default=None, min_length=32, max_length=36)
    username: str | None = Field(default=None, min_length=1, max_length=32)


class SessionJoinRequest(BaseModel):
    session_id: str = Field(min_length=4, max_length=256)


class EquippedRequest(BaseModel):
    equipped: dict[str, str] = Field(default_factory=dict)
    settings: dict[str, list[str]] = Field(default_factory=dict)


class BatchRequest(BaseModel):
    uuids: list[str] = Field(default_factory=list)


class TriggerRequest(BaseModel):
    slot: str = Field(min_length=1, max_length=64)
    trigger: str = Field(min_length=1, max_length=64)


def _validate_equipped(payload: dict[str, str], cfg: Settings) -> dict[str, str]:
    if not isinstance(payload, dict):
        raise HTTPException(400, "equipped must be an object")
    if len(payload) > cfg.max_cosmetics_per_player:
        raise HTTPException(400, f"too many slots (max {cfg.max_cosmetics_per_player})")
    cleaned: dict[str, str] = {}
    for slot, cosmetic_id in payload.items():
        if not isinstance(slot, str) or not isinstance(cosmetic_id, str):
            raise HTTPException(400, "slot and id must be strings")
        if slot not in KNOWN_SLOTS:
            raise HTTPException(400, f"unknown slot: {slot}")
        if not cosmetic_id or len(cosmetic_id) > cfg.max_cosmetic_id_len:
            raise HTTPException(400, f"invalid cosmetic id for slot {slot}")
        cleaned[slot] = cosmetic_id
    return cleaned


def _validate_settings(payload: dict[str, list[str]], equipped: dict[str, str], cfg: Settings) -> dict[str, list[str]]:
    if not isinstance(payload, dict):
        raise HTTPException(400, "settings must be an object")
    if len(payload) > cfg.max_cosmetics_per_player:
        raise HTTPException(400, f"too many setting groups (max {cfg.max_cosmetics_per_player})")
    equipped_ids = set(equipped.values())
    cleaned: dict[str, list[str]] = {}
    for cosmetic_id, setting_list in payload.items():
        if not isinstance(cosmetic_id, str) or not cosmetic_id or len(cosmetic_id) > cfg.max_cosmetic_id_len:
            raise HTTPException(400, "invalid cosmetic settings id")
        if cosmetic_id not in equipped_ids:
            continue
        if not isinstance(setting_list, list) or len(setting_list) > 32:
            raise HTTPException(400, f"invalid settings for {cosmetic_id}")
        cleaned_settings: list[str] = []
        for setting in setting_list:
            if not isinstance(setting, str) or len(setting) > 8192:
                raise HTTPException(400, f"invalid setting for {cosmetic_id}")
            cleaned_settings.append(setting)
        cleaned[cosmetic_id] = cleaned_settings
    return cleaned


def _parse_uuid(s: str) -> UUID:
    try:
        return UUID(s)
    except (ValueError, AttributeError):
        raise HTTPException(400, "invalid uuid")


def _short(value) -> str:
    text = str(value)
    return text[:8]


# ---------- auth ----------

@app.post("/api/auth/login")
async def auth_login(request: Request, body: AuthLoginRequest):
    cfg: Settings = request.app.state.cfg
    limiter: IpRateLimiter = request.app.state.auth_limiter
    if not limiter.try_acquire(_client_ip(request)):
        raise HTTPException(429, "rate limited")

    username = body.username
    if body.uuid is None:
        raise HTTPException(400, "uuid required")
    uuid = _parse_uuid(body.uuid)

    token = issue_jwt(cfg.jwt_secret, uuid, cfg.jwt_ttl_seconds)
    print(f"[auth] login ok username={username} uuid={_short(uuid)}", flush=True)
    return {
        "token": token,
        "uuid": str(uuid),
        "username": username,
        "expires_in": cfg.jwt_ttl_seconds,
    }


# ---------- session lifecycle ----------

@app.post("/api/session/join")
async def session_join(request: Request, body: SessionJoinRequest):
    cfg: Settings = request.app.state.cfg
    hub: SessionHub = request.app.state.hub
    metrics: Metrics | None = request.app.state.metrics

    if len(body.session_id) > cfg.max_session_id_len:
        raise HTTPException(400, "session_id too long")

    uuid = caller_uuid(request)
    try:
        _, previous_sid = await hub.join(body.session_id, uuid)
    except ValueError as e:
        raise HTTPException(409, str(e))
    if previous_sid is not None and previous_sid != body.session_id:
        await hub.publish(previous_sid, {"type": "player_left", "uuid": str(uuid)})

    members = await hub.members(body.session_id)
    storage: Storage = request.app.state.storage
    snapshot = storage.get_many([u for u in members if u != uuid])

    await hub.publish(
        body.session_id,
        {"type": "player_joined", "uuid": str(uuid)},
        except_uuid=uuid,
    )
    if metrics:
        metrics.track("player_seen", user_key=str(uuid))

    print(
        f"[session] join uuid={_short(uuid)} sid={_short(body.session_id)} members={len(members)} snapshot={len(snapshot)} previous={_short(previous_sid) if previous_sid else '-'}",
        flush=True,
    )

    return {
        "session_id": body.session_id,
        "members": [str(u) for u in members],
        "snapshot": list(snapshot.values()),
    }


@app.post("/api/session/leave")
async def session_leave(request: Request):
    hub: SessionHub = request.app.state.hub
    uuid = caller_uuid(request)
    sid = await hub.leave(uuid)
    if sid is not None:
        await hub.publish(sid, {"type": "player_left", "uuid": str(uuid)})
    print(f"[session] leave uuid={_short(uuid)} sid={_short(sid) if sid else '-'}", flush=True)
    return {"ok": True}


@app.post("/api/session/heartbeat")
async def session_heartbeat(request: Request):
    hub: SessionHub = request.app.state.hub
    metrics: Metrics | None = request.app.state.metrics
    uuid = caller_uuid(request)
    if not await hub.heartbeat(uuid):
        print(f"[session] heartbeat missing uuid={_short(uuid)}", flush=True)
        raise HTTPException(404, "not in a session")
    if metrics:
        metrics.track("player_seen", user_key=str(uuid))
    return {"ok": True}


@app.get("/api/stream")
async def stream(request: Request):
    cfg: Settings = request.app.state.cfg
    hub: SessionHub = request.app.state.hub
    uuid = caller_uuid(request)

    pair = await hub.subscriber(uuid)
    if pair is None:
        raise HTTPException(409, "not in a session; call /api/session/join first")
    _, sub = pair

    keepalive_s = cfg.sse_keepalive_s

    async def gen():
        yield b": connected\n\n"
        while True:
            if await request.is_disconnected():
                break
            try:
                event = await asyncio.wait_for(sub.queue.get(), timeout=keepalive_s)
            except asyncio.TimeoutError:
                yield b": keepalive\n\n"
                continue
            line = f"event: {event.get('type', 'message')}\ndata: {json.dumps(event, separators=(',', ':'))}\n\n"
            yield line.encode("utf-8")

    headers = {
        "Cache-Control": "no-cache, no-transform",
        "X-Accel-Buffering": "no",
        "Connection": "keep-alive",
    }
    return StreamingResponse(gen(), media_type="text/event-stream", headers=headers)


# ---------- cosmetic state ----------

@app.put("/api/cosmetics")
async def put_cosmetics(request: Request, body: EquippedRequest):
    cfg: Settings = request.app.state.cfg
    storage: Storage = request.app.state.storage
    writes: IpRateLimiter = request.app.state.writes
    hub: SessionHub = request.app.state.hub
    metrics: Metrics | None = request.app.state.metrics

    if not writes.try_acquire(_client_ip(request)):
        raise HTTPException(429, "rate limited")

    uuid = caller_uuid(request)
    equipped = _validate_equipped(body.equipped, cfg)
    settings = _validate_settings(body.settings, equipped, cfg)
    now = int(time.time())
    storage.put(uuid, equipped, settings, now)

    sid = await hub.session_of(uuid)
    fanout = 0
    if sid is not None:
        fanout = await hub.publish(
            sid,
            {"type": "cosmetic_changed", "uuid": str(uuid), "equipped": equipped, "settings": settings, "updated_at": now},
            except_uuid=uuid,
        )

    print(f"[cosmetics] put uuid={_short(uuid)} sid={_short(sid) if sid else '-'} slots={len(equipped)} settings={len(settings)} fanout={fanout}", flush=True)

    if metrics:
        metrics.track(
            "cosmetic_sync_write",
            user_key=str(uuid),
            props={
                "slots": sorted(equipped.keys()),
                "cosmetic_ids": list(equipped.values()),
                "fanout": 0 if sid is None else (await hub.members(sid)).__len__() - 1,
            },
        )
        metrics.track("player_seen", user_key=str(uuid))

    return {"ok": True, "updated_at": now}


@app.post("/api/trigger")
async def trigger_emote(request: Request, body: TriggerRequest):
    writes: IpRateLimiter = request.app.state.writes
    hub: SessionHub = request.app.state.hub
    metrics: Metrics | None = request.app.state.metrics

    if not writes.try_acquire(_client_ip(request)):
        raise HTTPException(429, "rate limited")

    uuid = caller_uuid(request)
    if body.slot not in KNOWN_SLOTS:
        raise HTTPException(400, f"unknown slot: {body.slot}")

    sid = await hub.session_of(uuid)
    if sid is not None:
        await hub.publish(
            sid,
            {
                "type": "cosmetic_trigger",
                "uuid": str(uuid),
                "slot": body.slot,
                "trigger": body.trigger,
            },
            except_uuid=uuid,
        )
    if metrics:
        metrics.track(
            "cosmetic_trigger",
            user_key=str(uuid),
            props={"slot": body.slot, "trigger": body.trigger},
        )
    return {"ok": True}


@app.get("/api/cosmetics/{uuid_str}")
async def get_cosmetics(request: Request, uuid_str: str):
    storage: Storage = request.app.state.storage
    metrics: Metrics | None = request.app.state.metrics
    caller_uuid(request)  # require auth even for reads to keep things scoped

    uuid = _parse_uuid(uuid_str)
    rec = storage.get(uuid)
    if metrics:
        metrics.track("cosmetic_sync_read", user_key=str(uuid))
    if rec is None:
        return {"uuid": str(uuid), "equipped": {}, "settings": {}, "updated_at": 0, "seen_at": 0}
    return rec


@app.post("/api/cosmetics/batch")
async def batch_get_cosmetics(request: Request, body: BatchRequest):
    storage: Storage = request.app.state.storage
    writes: IpRateLimiter = request.app.state.writes
    metrics: Metrics | None = request.app.state.metrics
    caller_uuid(request)

    if not writes.try_acquire(_client_ip(request)):
        raise HTTPException(429, "rate limited")
    if len(body.uuids) > 100:
        raise HTTPException(400, "batch too large (max 100)")

    parsed = []
    for s in body.uuids:
        try:
            parsed.append(UUID(s))
        except (ValueError, AttributeError):
            continue
    rows = storage.get_many(parsed)
    if metrics:
        for u in parsed:
            metrics.track("cosmetic_sync_read", user_key=str(u))
    out = []
    for u in parsed:
        s = str(u)
        if s in rows:
            out.append(rows[s])
        else:
            out.append({"uuid": s, "equipped": {}, "settings": {}, "updated_at": 0, "seen_at": 0})
    return {"players": out}


# ---------- misc ----------

@app.get("/api/slots")
async def list_slots():
    return {"slots": sorted(KNOWN_SLOTS)}


@app.get("/api/stats")
async def stats(request: Request):
    storage: Storage = request.app.state.storage
    hub: SessionHub = request.app.state.hub
    now = int(time.time())
    return {
        "total_players": storage.total_players(),
        "active_1d": storage.active_since(now - 86400),
        "active_7d": storage.active_since(now - 7 * 86400),
        "active_30d": storage.active_since(now - 30 * 86400),
        "live": hub.stats(),
    }


@app.get("/", response_class=PlainTextResponse)
async def root():
    return (
        "cosmetics sync.\n\n"
        "auth (once per launcher session):\n"
        "  POST /api/auth/login   {uuid, username}            -> {token, uuid, username}\n"
        "    identity is verified against Mojang and bound to a signed session JWT\n\n"
        "session (mod opens these on game-server-join, closes on leave):\n"
        "  POST /api/session/join     {session_id}\n"
        "  POST /api/session/leave\n"
        "  POST /api/session/heartbeat\n"
        "  GET  /api/stream                 [SSE: cosmetic_changed / player_joined / player_left]\n\n"
        "cosmetic state:\n"
        "  PUT  /api/cosmetics        {equipped: {SLOT: id, ...}, settings: {id: [...]}}\n"
        "  GET  /api/cosmetics/{uuid}\n"
        "  POST /api/cosmetics/batch  {uuids: [...]}\n\n"
        "misc:\n"
        "  GET  /api/slots\n"
        "  GET  /api/stats\n\n"
        "all endpoints except /api/auth/* and / require Authorization: Bearer <jwt>.\n"
    )
