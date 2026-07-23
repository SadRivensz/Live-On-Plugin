from __future__ import annotations

import base64
import hashlib
import hmac
import json
import time
from dataclasses import dataclass

from .config import Settings


@dataclass(frozen=True)
class Session:
    rsn: str
    role: str
    staff: bool
    expires_at: int


class TokenSigner:
    """Small HMAC token format; avoids putting a JWT dependency in this service."""

    def __init__(self, config: Settings):
        self._secret = config.token_secret.encode("utf-8")
        self._lifetime = config.token_lifetime_seconds

    def issue(self, rsn: str, role: str, staff: bool) -> tuple[str, int]:
        expires_at = int(time.time()) + self._lifetime
        payload = {"rsn": rsn, "role": role, "staff": staff, "exp": expires_at}
        encoded = _b64(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
        signature = _b64(hmac.new(self._secret, encoded.encode("ascii"), hashlib.sha256).digest())
        return f"{encoded}.{signature}", expires_at

    def verify(self, token: str) -> Session | None:
        try:
            encoded, supplied_signature = token.split(".", 1)
            expected = _b64(hmac.new(self._secret, encoded.encode("ascii"), hashlib.sha256).digest())
            if not hmac.compare_digest(supplied_signature, expected):
                return None
            payload = json.loads(_unb64(encoded))
            if int(payload["exp"]) <= int(time.time()):
                return None
            return Session(
                rsn=str(payload["rsn"]),
                role=str(payload.get("role", "member")),
                staff=bool(payload.get("staff", False)),
                expires_at=int(payload["exp"]),
            )
        except (ValueError, KeyError, TypeError, json.JSONDecodeError):
            return None


def access_code_matches(candidate: str, expected_sha256: str) -> bool:
    if not expected_sha256:
        return True
    actual = hashlib.sha256(candidate.encode("utf-8")).hexdigest()
    return hmac.compare_digest(actual, expected_sha256)


def normalize_rsn(value: str) -> str:
    return " ".join(value.replace("_", " ").strip().lower().split())


def _b64(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


def _unb64(value: str) -> bytes:
    padding = "=" * (-len(value) % 4)
    return base64.urlsafe_b64decode(value + padding)
