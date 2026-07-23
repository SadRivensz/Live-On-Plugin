from __future__ import annotations

import os
from dataclasses import dataclass


def _csv(name: str) -> tuple[str, ...]:
    return tuple(value.strip() for value in os.getenv(name, "").split(",") if value.strip())


@dataclass(frozen=True)
class Settings:
    environment: str = os.getenv("LIVE_ON_ENV", "development")
    database_path: str = os.getenv("LIVE_ON_DATABASE_PATH", "./data/live-on.db")
    token_secret: str = os.getenv("LIVE_ON_TOKEN_SECRET", "development-secret-change-me")
    access_code_sha256: str = os.getenv("LIVE_ON_ACCESS_CODE_SHA256", "").lower()
    wom_group_id: int = int(os.getenv("LIVE_ON_WOM_GROUP_ID", "0"))
    wom_api_key: str = os.getenv("LIVE_ON_WOM_API_KEY", "")
    bootstrap_members: tuple[str, ...] = _csv("LIVE_ON_BOOTSTRAP_MEMBERS")
    staff_rsns: tuple[str, ...] = _csv("LIVE_ON_STAFF_RSNS")
    discord_webhook: str = os.getenv("LIVE_ON_DISCORD_WEBHOOK", "")
    login_message: str = os.getenv(
        "LIVE_ON_LOGIN_MESSAGE",
        "Bem-vindo à Live On! Confira os anúncios no painel lateral.",
    )
    cors_origins: tuple[str, ...] = _csv("LIVE_ON_CORS_ORIGINS")
    token_lifetime_seconds: int = int(os.getenv("LIVE_ON_TOKEN_LIFETIME_SECONDS", "43200"))

    def validate(self) -> None:
        if self.environment == "production" and len(self.token_secret) < 32:
            raise RuntimeError("LIVE_ON_TOKEN_SECRET must contain at least 32 characters in production")
        if self.environment == "production" and self.wom_group_id <= 0:
            raise RuntimeError("LIVE_ON_WOM_GROUP_ID is required in production")


settings = Settings()
