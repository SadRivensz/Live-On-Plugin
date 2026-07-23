from __future__ import annotations

import asyncio
import time
from datetime import datetime, timezone
from typing import Any
from urllib.parse import quote

import httpx

from .config import Settings
from .security import normalize_rsn


class WiseOldManClient:
    BASE_URL = "https://api.wiseoldman.net/v2"

    def __init__(self, config: Settings):
        self.group_id = config.wom_group_id
        headers = {"User-Agent": "Live-On-Clan/0.1"}
        if config.wom_api_key:
            headers["x-api-key"] = config.wom_api_key
        self.client = httpx.AsyncClient(base_url=self.BASE_URL, headers=headers, timeout=15)
        self._member_cache: tuple[float, list[dict[str, Any]]] = (0, [])
        self._cache_lock = asyncio.Lock()

    async def close(self) -> None:
        await self.client.aclose()

    async def members(self) -> list[dict[str, Any]]:
        if self.group_id <= 0:
            return []
        cached_at, members = self._member_cache
        if time.monotonic() - cached_at < 300 and members:
            return members
        async with self._cache_lock:
            cached_at, members = self._member_cache
            if time.monotonic() - cached_at < 300 and members:
                return members
            response = await self.client.get(f"/groups/{self.group_id}")
            response.raise_for_status()
            payload = response.json()
            memberships = payload.get("memberships") or payload.get("members") or []
            parsed: list[dict[str, Any]] = []
            for membership in memberships:
                player = membership.get("player", membership)
                display_name = player.get("displayName") or player.get("username")
                if not display_name:
                    continue
                parsed.append(
                    {
                        "rsn": display_name,
                        "role": membership.get("role", "member"),
                        "totalXp": int(player.get("exp") or 0),
                        "ehp": float(player.get("ehp") or 0),
                        "ehb": float(player.get("ehb") or 0),
                    }
                )
            self._member_cache = (time.monotonic(), parsed)
            return parsed

    async def member(self, rsn: str) -> dict[str, Any] | None:
        key = normalize_rsn(rsn)
        return next((member for member in await self.members() if normalize_rsn(member["rsn"]) == key), None)

    async def player_profile(self, rsn: str) -> dict[str, Any]:
        response = await self.client.get(f"/players/{quote(rsn, safe='')}")
        response.raise_for_status()
        return response.json()

    async def rankings(self, metric: str, start: datetime, end: datetime) -> list[dict[str, Any]]:
        if self.group_id <= 0:
            return []
        wom_metric = {"xp": "overall", "ehp": "ehp", "ehb": "ehb"}[metric]
        response = await self.client.get(
            f"/groups/{self.group_id}/gained",
            params={
                "metric": wom_metric,
                "startDate": start.astimezone(timezone.utc).isoformat(),
                "endDate": end.astimezone(timezone.utc).isoformat(),
            },
        )
        response.raise_for_status()
        entries = []
        for raw in response.json():
            player = raw.get("player", {})
            data = raw.get("data", {})
            value = data.get("gained", data.get("value", 0))
            entries.append(
                {
                    "rsn": player.get("displayName") or player.get("username") or "Unknown",
                    "value": float(value or 0),
                }
            )
        entries.sort(key=lambda entry: entry["value"], reverse=True)
        return entries


def profile_sections(payload: dict[str, Any]) -> tuple[list[dict], list[dict], list[dict], str | None]:
    snapshot = payload.get("latestSnapshot") or payload.get("snapshot") or {}
    data = snapshot.get("data", snapshot)
    skills = _skill_entries(data.get("skills", {}))
    bosses = _score_entries(data.get("bosses", {}), "kills")
    activities = _score_entries(data.get("activities", {}), "score")
    last_updated = payload.get("updatedAt") or snapshot.get("createdAt")
    return skills, bosses, activities, last_updated


def _skill_entries(values: dict[str, Any]) -> list[dict]:
    entries = []
    for name, raw in values.items():
        if name == "overall" or not isinstance(raw, dict):
            continue
        entries.append(
            {
                "name": _title(name),
                "value": int(raw.get("experience") or 0),
                "level": int(raw.get("level") or 0),
                "rank": int(raw.get("rank") or -1),
                "ehp": float(raw.get("ehp") or 0),
            }
        )
    return entries


def _score_entries(values: dict[str, Any], field: str) -> list[dict]:
    entries = []
    for name, raw in values.items():
        if not isinstance(raw, dict):
            continue
        value = int(raw.get(field) or raw.get("value") or 0)
        if value <= 0:
            continue
        entries.append({"name": _title(name), "value": value, "level": 0, "rank": int(raw.get("rank") or -1), "ehp": 0})
    entries.sort(key=lambda entry: entry["value"], reverse=True)
    return entries[:30]


def _title(value: str) -> str:
    return value.replace("_", " ").title()
