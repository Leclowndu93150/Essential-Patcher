import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    host: str
    port: int
    db_path: str
    max_cosmetics_per_player: int
    max_cosmetic_id_len: int
    max_session_id_len: int
    max_players_per_session: int
    write_per_minute: int
    auth_per_minute: int
    player_inactive_days: int
    session_heartbeat_timeout_s: int
    sse_keepalive_s: int
    jwt_secret: str
    jwt_ttl_seconds: int
    panel_endpoint: str
    panel_hmac_key: str

    @classmethod
    def load(cls):
        return cls(
            host=os.getenv("COSMETICSYNC_HOST", "127.0.0.1"),
            port=int(os.getenv("COSMETICSYNC_PORT", "7100")),
            db_path=os.getenv("COSMETICSYNC_DB", "cosmetics.db"),
            max_cosmetics_per_player=int(os.getenv("MAX_COSMETICS_PER_PLAYER", "50")),
            max_cosmetic_id_len=int(os.getenv("MAX_COSMETIC_ID_LEN", "128")),
            max_session_id_len=int(os.getenv("MAX_SESSION_ID_LEN", "128")),
            max_players_per_session=int(os.getenv("MAX_PLAYERS_PER_SESSION", "200")),
            write_per_minute=int(os.getenv("WRITE_PER_MINUTE", "20")),
            auth_per_minute=int(os.getenv("AUTH_PER_MINUTE", "6")),
            player_inactive_days=int(os.getenv("PLAYER_INACTIVE_DAYS", "90")),
            session_heartbeat_timeout_s=int(os.getenv("SESSION_HEARTBEAT_TIMEOUT_S", "90")),
            sse_keepalive_s=int(os.getenv("SSE_KEEPALIVE_S", "20")),
            jwt_secret=os.environ["JWT_SECRET"],
            jwt_ttl_seconds=int(os.getenv("JWT_TTL_SECONDS", "72000")),
            panel_endpoint=os.getenv("PANEL_ENDPOINT", ""),
            panel_hmac_key=os.getenv("PANEL_HMAC_KEY", ""),
        )
