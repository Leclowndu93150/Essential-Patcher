import time
from uuid import UUID

import jwt
from fastapi import HTTPException, Request


def issue_jwt(secret: str, uuid: UUID, ttl_seconds: int) -> str:
    now = int(time.time())
    payload = {
        "sub": str(uuid),
        "iat": now,
        "exp": now + ttl_seconds,
    }
    return jwt.encode(payload, secret, algorithm="HS256")


def verify_jwt(secret: str, token: str) -> UUID:
    try:
        data = jwt.decode(token, secret, algorithms=["HS256"])
    except jwt.InvalidTokenError:
        raise HTTPException(401, "invalid token")
    try:
        return UUID(data["sub"])
    except (KeyError, ValueError):
        raise HTTPException(401, "invalid token subject")


def caller_uuid(request: Request) -> UUID:
    cfg = request.app.state.cfg
    h = request.headers.get("authorization", "")
    if not h.lower().startswith("bearer "):
        raise HTTPException(401, "missing bearer token")
    return verify_jwt(cfg.jwt_secret, h[7:].strip())
