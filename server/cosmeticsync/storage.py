import json
import sqlite3
import threading
import time
from pathlib import Path
from uuid import UUID


_SCHEMA = """
CREATE TABLE IF NOT EXISTS players (
    uuid       TEXT PRIMARY KEY,
    equipped   TEXT NOT NULL,
    settings   TEXT NOT NULL DEFAULT '{}',
    updated_at INTEGER NOT NULL,
    seen_at    INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS players_seen ON players(seen_at);
"""


class Storage:
    def __init__(self, db_path: str):
        Path(db_path).resolve().parent.mkdir(parents=True, exist_ok=True)
        self._conn = sqlite3.connect(db_path, check_same_thread=False, isolation_level=None)
        self._conn.execute("PRAGMA journal_mode=WAL")
        self._conn.execute("PRAGMA synchronous=NORMAL")
        self._conn.executescript(_SCHEMA)
        self._migrate()
        self._lock = threading.Lock()

    def _migrate(self) -> None:
        columns = {row[1] for row in self._conn.execute("PRAGMA table_info(players)").fetchall()}
        if "settings" not in columns:
            self._conn.execute("ALTER TABLE players ADD COLUMN settings TEXT NOT NULL DEFAULT '{}'")

    def get(self, uuid: UUID) -> dict | None:
        with self._lock:
            cur = self._conn.execute(
                "SELECT equipped, settings, updated_at, seen_at FROM players WHERE uuid = ?",
                (str(uuid),),
            )
            row = cur.fetchone()
        if row is None:
            return None
        try:
            equipped = json.loads(row[0])
            if not isinstance(equipped, dict):
                equipped = {}
        except json.JSONDecodeError:
            equipped = {}
        try:
            settings = json.loads(row[1])
            if not isinstance(settings, dict):
                settings = {}
        except json.JSONDecodeError:
            settings = {}
        return {
            "uuid": str(uuid),
            "equipped": equipped,
            "settings": settings,
            "updated_at": row[2],
            "seen_at": row[3],
        }

    def get_many(self, uuids: list[UUID]) -> dict[str, dict]:
        if not uuids:
            return {}
        placeholders = ",".join("?" * len(uuids))
        with self._lock:
            cur = self._conn.execute(
                f"SELECT uuid, equipped, settings, updated_at, seen_at FROM players WHERE uuid IN ({placeholders})",
                tuple(str(u) for u in uuids),
            )
            rows = cur.fetchall()
        out = {}
        for r in rows:
            try:
                equipped = json.loads(r[1])
                if not isinstance(equipped, dict):
                    equipped = {}
            except json.JSONDecodeError:
                equipped = {}
            try:
                settings = json.loads(r[2])
                if not isinstance(settings, dict):
                    settings = {}
            except json.JSONDecodeError:
                settings = {}
            out[r[0]] = {
                "uuid": r[0],
                "equipped": equipped,
                "settings": settings,
                "updated_at": r[3],
                "seen_at": r[4],
            }
        return out

    def put(self, uuid: UUID, equipped: dict[str, str], settings: dict[str, list[str]], now: int) -> None:
        equipped_body = json.dumps(equipped, separators=(",", ":"))
        settings_body = json.dumps(settings, separators=(",", ":"))
        with self._lock:
            self._conn.execute(
                "INSERT INTO players (uuid, equipped, settings, updated_at, seen_at) "
                "VALUES (?, ?, ?, ?, ?) "
                "ON CONFLICT(uuid) DO UPDATE SET "
                "equipped = excluded.equipped, "
                "settings = excluded.settings, "
                "updated_at = excluded.updated_at, "
                "seen_at = excluded.seen_at",
                (str(uuid), equipped_body, settings_body, now, now),
            )

    def touch(self, uuid: UUID, now: int) -> None:
        with self._lock:
            self._conn.execute(
                "UPDATE players SET seen_at = ? WHERE uuid = ?",
                (now, str(uuid)),
            )

    def purge_inactive(self, cutoff_ts: int) -> int:
        with self._lock:
            cur = self._conn.execute(
                "DELETE FROM players WHERE seen_at < ?",
                (cutoff_ts,),
            )
        return cur.rowcount

    def total_players(self) -> int:
        with self._lock:
            row = self._conn.execute("SELECT COUNT(*) FROM players").fetchone()
        return row[0] if row else 0

    def active_since(self, cutoff_ts: int) -> int:
        with self._lock:
            row = self._conn.execute(
                "SELECT COUNT(*) FROM players WHERE seen_at >= ?",
                (cutoff_ts,),
            ).fetchone()
        return row[0] if row else 0
