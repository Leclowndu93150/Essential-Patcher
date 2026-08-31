import asyncio
import time
from dataclasses import dataclass, field
from uuid import UUID


@dataclass
class Subscriber:
    uuid: UUID
    queue: asyncio.Queue
    last_heartbeat: float = field(default_factory=time.monotonic)


@dataclass
class Session:
    session_id: str
    members: dict[UUID, Subscriber] = field(default_factory=dict)


class SessionHub:
    def __init__(self, heartbeat_timeout_s: int, max_per_session: int):
        self.heartbeat_timeout_s = heartbeat_timeout_s
        self.max_per_session = max_per_session
        self._sessions: dict[str, Session] = {}
        self._uuid_to_session: dict[UUID, str] = {}
        self._lock = asyncio.Lock()

    async def join(self, session_id: str, uuid: UUID) -> tuple[Subscriber, str | None]:
        async with self._lock:
            session = self._sessions.get(session_id)
            current_sid = self._uuid_to_session.get(uuid)
            if current_sid != session_id and session is not None and len(session.members) >= self.max_per_session:
                raise ValueError("session full")
            previous_sid = await self._leave_no_lock(uuid)
            session = self._sessions.get(session_id)
            if session is None:
                session = Session(session_id=session_id)
                self._sessions[session_id] = session
            sub = Subscriber(uuid=uuid, queue=asyncio.Queue(maxsize=128))
            session.members[uuid] = sub
            self._uuid_to_session[uuid] = session_id
            return sub, previous_sid

    async def leave(self, uuid: UUID) -> str | None:
        async with self._lock:
            return await self._leave_no_lock(uuid)

    async def _leave_no_lock(self, uuid: UUID) -> str | None:
        prev = self._uuid_to_session.pop(uuid, None)
        if prev is None:
            return None
        session = self._sessions.get(prev)
        if session is None:
            return prev
        session.members.pop(uuid, None)
        if not session.members:
            self._sessions.pop(prev, None)
        return prev

    async def heartbeat(self, uuid: UUID) -> bool:
        async with self._lock:
            sid = self._uuid_to_session.get(uuid)
            if sid is None:
                return False
            session = self._sessions.get(sid)
            if session is None:
                return False
            sub = session.members.get(uuid)
            if sub is None:
                return False
            sub.last_heartbeat = time.monotonic()
            return True

    async def members(self, session_id: str) -> list[UUID]:
        async with self._lock:
            session = self._sessions.get(session_id)
            return list(session.members.keys()) if session else []

    async def session_of(self, uuid: UUID) -> str | None:
        async with self._lock:
            return self._uuid_to_session.get(uuid)

    async def subscriber(self, uuid: UUID) -> tuple[str, Subscriber] | None:
        async with self._lock:
            sid = self._uuid_to_session.get(uuid)
            if sid is None:
                return None
            session = self._sessions.get(sid)
            if session is None:
                return None
            sub = session.members.get(uuid)
            if sub is None:
                return None
            return sid, sub

    async def publish(self, session_id: str, event: dict, except_uuid: UUID | None = None) -> int:
        async with self._lock:
            session = self._sessions.get(session_id)
            if session is None:
                return 0
            targets = [sub for u, sub in session.members.items() if u != except_uuid]
        delivered = 0
        for sub in targets:
            try:
                sub.queue.put_nowait(event)
                delivered += 1
            except asyncio.QueueFull:
                pass
        return delivered

    async def sweep(self) -> list[tuple[str, UUID]]:
        cutoff = time.monotonic() - self.heartbeat_timeout_s
        evicted: list[tuple[str, UUID]] = []
        async with self._lock:
            stale = [
                (sid, u)
                for sid, session in self._sessions.items()
                for u, sub in session.members.items()
                if sub.last_heartbeat < cutoff
            ]
            for sid, u in stale:
                await self._leave_no_lock(u)
                evicted.append((sid, u))
        return evicted

    def stats(self) -> dict:
        return {
            "sessions": len(self._sessions),
            "subscribers": len(self._uuid_to_session),
        }
