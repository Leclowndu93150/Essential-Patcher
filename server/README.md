# cosmetics-sync

Live cosmetic-sync backend for the Essential Patcher mod. FastAPI + SQLite + in-memory SSE hub. Pushes a `cosmetic_changed` event to every nearby player within ~100 ms of a wardrobe change, without per-client polling.

## What this service handles

It stores, per player UUID:

- the equipped-cosmetic map (`{"HAT": "<cosmetic id>", ...}`) and its per-cosmetic settings
- `updated_at` / `seen_at` timestamps

That is the entire `players` table — see `storage.py`. In memory, for the duration of a play session, it also holds which session_id hash you are in and an open SSE queue.

It does **not** receive, request, store, or forward Minecraft or Microsoft access tokens, refresh tokens, passwords, or any other authentication material. The mod never reads them; the service has no field to put them in. Usernames are logged to stdout on login and not persisted. Rows go away after `PLAYER_INACTIVE_DAYS` of not being seen.

## Wire format

From decompiling `essential.jar` (`gg.essential.network.connectionmanager.cosmetics.InfraEquippedOutfitsManager$InfraOutfit`):

```
public data class InfraOutfit(
    cosmetics: Map<CosmeticSlot, String>,
    settings:  Map<String, List<CosmeticSetting>>,
    skin:      Skin?
)
```

The patcher uses the first map only (`Map<String, String>` because `CosmeticSlot` serializes to its `id`), so this service stores and broadcasts that exact shape:

```json
{"HAT": "5f7e...", "CAPE": "61f8...", "BACK": "..."}
```

Slot whitelist (from `gg.essential.mod.cosmetics.CosmeticSlot`):
`BACK, EARS, FACE, FULL_BODY, HAT, PET, TAIL, ARMS, SHOULDERS, SUITS, SHOES, PANTS, WINGS, EFFECT, CAPE, EMOTE, ICON, TOP, ACCESSORY, HEAD, SKIRT`.

## How it works

The mod groups itself with other players using a **session_id** that it computes locally — a truncated SHA-256 of the server IP + port for online play, of the Essential SPS host UUID for SPS, of (host UUID + world name) for LAN, of the world name for singleplayer. Only the hash is sent, never the address itself. Everyone with the same session_id is in the same broadcast group.

When player A changes cosmetics, the mod `PUT`s the new equipped map. The server stores it in SQLite **and** publishes a `cosmetic_changed` SSE event to every other member of A's session. Players B/C/D, who are on the same server and have an open SSE stream, get the event and feed it to `InfraEquippedOutfitsManager.update(uuid, new InfraOutfit(map, emptyMap, null))` to render it.

The mod identifies itself once per launcher session with its UUID and username and gets back a JWT it uses on every subsequent request.

**This identification is not verified against Mojang.** Anyone can ask the service for a JWT for any UUID, and use it to overwrite that UUID's stored cosmetics or read the members of a session. The intended fix is the standard `joinServer`/`hasJoined` handshake, which proves the UUID without the service ever seeing an access token; it is not implemented yet.

## HTTP API

All responses JSON. Every endpoint except `/`, `/api/auth/login`, `/api/slots`, `/api/stats` requires `Authorization: Bearer <jwt>`.

### Auth (once per launcher session)

| Method | Path                | Body                     | Returns                                    |
|--------|---------------------|--------------------------|--------------------------------------------|
| POST   | `/api/auth/login`   | `{"uuid","username"}`    | `{"token","uuid","username","expires_in"}` |

Rate limited per IP (`AUTH_PER_MINUTE`). The JWT carries the UUID as its subject and nothing else; every later request derives "who" from it, so no endpoint takes a caller UUID as a parameter.

### Session (mod keeps these open for the duration of being on a server)

| Method | Path                       | Body                  | Notes |
|--------|----------------------------|-----------------------|-------|
| POST   | `/api/session/join`        | `{"session_id"}`      | Returns current members + a snapshot of their equipped maps. Also broadcasts `player_joined` to existing members. |
| POST   | `/api/session/leave`       | -                     | Broadcasts `player_left`. |
| POST   | `/api/session/heartbeat`   | -                     | Mod should call every ~30 s so the server doesn't garbage-collect stale members. |
| GET    | `/api/stream`              | -                     | **Server-Sent Events** stream. Pushes `cosmetic_changed`, `player_joined`, `player_left`. Keeps itself alive with `: keepalive` comments every 20 s. |

### Cosmetic state

| Method | Path                       | Body                                     | Notes |
|--------|----------------------------|------------------------------------------|-------|
| PUT    | `/api/cosmetics`           | `{"equipped": {"HAT":"id", ...}}`        | Writes the caller's own equipped map (caller is from the JWT), then broadcasts `cosmetic_changed` to the rest of their session. |
| GET    | `/api/cosmetics/{uuid}`    | -                                        | One-shot read. Used on first join to populate not-yet-cached players. |
| POST   | `/api/cosmetics/batch`     | `{"uuids":[...]}`                        | Up to 100 UUIDs at once. |

### Misc

| Method | Path           | Notes |
|--------|----------------|-------|
| GET    | `/api/slots`   | Slot whitelist. |
| GET    | `/api/stats`   | `{total_players, active_1d/7d/30d, live: {sessions, subscribers}}` |

## SSE event shapes

```
event: cosmetic_changed
data: {"type":"cosmetic_changed","uuid":"...","equipped":{"HAT":"..."},"updated_at":1234567890}

event: player_joined
data: {"type":"player_joined","uuid":"..."}

event: player_left
data: {"type":"player_left","uuid":"..."}
```

The server also writes a leading `: connected` comment on stream open and `: keepalive` every 20 s. Any line starting with `:` is just a comment per the SSE spec; mod-side EventSource libraries handle it transparently.

## Mod-side wiring

```
on game-server-join (or LAN open / SP start):
    sid = sha256(server_address)                   # or host_uuid for SPS/LAN, world name for SP
    {token, uuid} = POST /api/auth/login {uuid, username}
    POST /api/session/join {session_id: sid}
        -> remember the returned snapshot, apply to each player
    open SSE GET /api/stream                       # background thread
    every 30s: POST /api/session/heartbeat

on local wardrobe change:
    PUT /api/cosmetics {equipped: CosmeticSaver.loadEquippedCosmetics()}

on SSE event:
    cosmetic_changed -> InfraEquippedOutfitsManager.update(uuid, new InfraOutfit(map, emptyMap, null))
    player_joined     -> GET /api/cosmetics/{uuid}, then update()
    player_left       -> optional cleanup

on game-server-leave:
    POST /api/session/leave
    close SSE stream
```

## Scale

Designed for ~1–2 k concurrent SSE clients on the current tiny VPS:

- Sessions live in a process-local `dict` keyed by session_id. No DB lookups per event.
- Each subscriber has a bounded `asyncio.Queue(128)`; if a client is too slow it just drops events on the floor instead of blocking the publisher.
- SQLite WAL handles persistent equipped maps. Writes are infrequent (only on actual cosmetic changes), so single-writer contention is fine.
- A JWT is minted once per launcher session, not per write.

If you ever blow past ~2 k concurrent, the migration path is: move sessions to Redis pub/sub (drop-in replacement for `SessionHub.publish`) and switch SQLite to Postgres. The API stays the same.

## Panel metrics

If `PANEL_ENDPOINT` and `PANEL_HMAC_KEY` are set, the service emits:

- `cosmetic_sync_write` with `{slots, cosmetic_ids, fanout}` on every PUT
- `cosmetic_sync_read` on every GET / batch GET (one per uuid)
- `player_seen` on join + heartbeat + PUT

Driving the panel's Cosmetics tab.

## Deploy

```
cp conf/cosmeticsync.example.env conf/cosmeticsync.env
# fill JWT_SECRET (openssl rand -base64 48) and PANEL_HMAC_KEY (same as the panel's INGEST_HMAC_KEY)
COSMETICS_VPS_PASS='…' ./ops/deploy.sh
```

After deploy, the script reminds you to add `cosmetics-sync.service` to:
1. `PANEL_SERVICES` in `/opt/panel/.env`,
2. `/etc/sudoers.d/panel` (start/stop/restart entries),
3. then `sudo systemctl restart panel`.

## Local dev

```
pip install --break-system-packages -r requirements.txt
JWT_SECRET=dev-secret-dev-secret-dev-secret \
  COSMETICSYNC_DB=/tmp/cosmetics.db \
  python -m uvicorn cosmeticsync.app:app --reload --port 7100
```
