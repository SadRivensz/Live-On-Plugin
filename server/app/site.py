from __future__ import annotations

import time
from typing import Any

import httpx

from .config import Settings
from .security import normalize_rsn


class LiveOnMemberSiteClient:
    """Optional bridge to Discord links already maintained by the clan site."""

    def __init__(self, config: Settings):
        self.base_url = config.member_site_base_url.rstrip("/")
        self.client = httpx.AsyncClient(base_url=self.base_url, timeout=httpx.Timeout(12, connect=5))
        self.cache: dict[str, tuple[float, dict[str, Any] | None]] = {}

    async def close(self) -> None:
        await self.client.aclose()

    async def member(self, rsn: str) -> dict[str, Any] | None:
        if not self.base_url:
            return None
        key = normalize_rsn(rsn)
        cached_at, cached = self.cache.get(key, (0, None))
        if time.monotonic() - cached_at < 300:
            return cached

        response = await self.client.get("/achievement/players", params={"search": rsn})
        response.raise_for_status()
        players = response.json().get("players") or []
        matched = next(
            (
                player
                for player in players
                if normalize_rsn(player.get("display_name") or player.get("username") or "") == key
            ),
            None,
        )
        result = None
        if matched:
            result = {
                "avatarUrl": matched.get("discord_avatar_url"),
                "discordName": (
                    matched.get("discord_guild_nickname")
                    or matched.get("discord_global_name")
                    or matched.get("discord_username")
                ),
                "accountType": matched.get("account_type") or "regular",
                "country": matched.get("country"),
            }
        self.cache[key] = (time.monotonic(), result)
        return result
