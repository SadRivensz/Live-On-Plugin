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
        headers = {"User-Agent": "Live-On-Clan/0.2.6"}
        if config.wom_api_key:
            headers["x-api-key"] = config.wom_api_key
        self.client = httpx.AsyncClient(
            base_url=self.BASE_URL,
            headers=headers,
            timeout=httpx.Timeout(35, connect=10),
        )
        self._member_cache: tuple[float, list[dict[str, Any]]] = (0, [])
        self._profile_cache: dict[str, tuple[float, dict[str, Any]]] = {}
        self._ranking_cache: dict[str, tuple[float, list[dict[str, Any]]]] = {}
        self._profile_inflight: dict[str, asyncio.Task[dict[str, Any]]] = {}
        self._ranking_inflight: dict[str, asyncio.Task[list[dict[str, Any]]]] = {}
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
                        "accountType": player.get("type") or "regular",
                        "country": player.get("country"),
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
        key = normalize_rsn(rsn)
        cached_at, cached = self._profile_cache.get(key, (0, {}))
        if time.monotonic() - cached_at < 180 and cached:
            return cached
        async with self._cache_lock:
            cached_at, cached = self._profile_cache.get(key, (0, {}))
            if time.monotonic() - cached_at < 180 and cached:
                return cached
            task = self._profile_inflight.get(key)
            if task is None:
                task = asyncio.create_task(self._fetch_player_profile(rsn))
                self._profile_inflight[key] = task
        try:
            profile = await task
            self._profile_cache[key] = (time.monotonic(), profile)
            return profile
        finally:
            async with self._cache_lock:
                if self._profile_inflight.get(key) is task:
                    self._profile_inflight.pop(key, None)

    async def _fetch_player_profile(self, rsn: str) -> dict[str, Any]:
        response = await self.client.get(f"/players/{quote(rsn, safe='')}")
        response.raise_for_status()
        return response.json()

    async def rankings(self, metric: str, start: datetime, end: datetime) -> list[dict[str, Any]]:
        if self.group_id <= 0:
            return []
        wom_metric = {"xp": "overall", "ehp": "ehp", "ehb": "ehb"}[metric]
        # The current-period end is "now", so including its seconds would make
        # every request a different cache key. Month start uniquely identifies
        # both current and previous monthly races.
        cache_key = f"{wom_metric}:{start.date().isoformat()}"
        cached_at, cached_entries = self._ranking_cache.get(cache_key, (0, []))
        if time.monotonic() - cached_at < 600 and cached_entries:
            return cached_entries
        async with self._cache_lock:
            cached_at, cached_entries = self._ranking_cache.get(cache_key, (0, []))
            if time.monotonic() - cached_at < 600 and cached_entries:
                return cached_entries
            task = self._ranking_inflight.get(cache_key)
            if task is None:
                task = asyncio.create_task(self._fetch_rankings(wom_metric, start, end))
                self._ranking_inflight[cache_key] = task
        try:
            entries = await task
            self._ranking_cache[cache_key] = (time.monotonic(), entries)
            return entries
        finally:
            async with self._cache_lock:
                if self._ranking_inflight.get(cache_key) is task:
                    self._ranking_inflight.pop(cache_key, None)

    async def _fetch_rankings(
        self,
        wom_metric: str,
        start: datetime,
        end: datetime,
    ) -> list[dict[str, Any]]:
        response = None
        for attempt in range(3):
            try:
                response = await self.client.get(
                    f"/groups/{self.group_id}/gained",
                    params={
                        "metric": wom_metric,
                        "startDate": start.astimezone(timezone.utc).isoformat(),
                        "endDate": end.astimezone(timezone.utc).isoformat(),
                    },
                )
                response.raise_for_status()
                break
            except (httpx.TimeoutException, httpx.NetworkError, httpx.RemoteProtocolError):
                if attempt == 2:
                    raise
                await asyncio.sleep(0.4 * (attempt + 1))
            except httpx.HTTPStatusError as exception:
                if exception.response.status_code not in {429, 500, 502, 503, 504} or attempt == 2:
                    raise
                await asyncio.sleep(0.5 * (attempt + 1))
        if response is None:
            return []
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
        value = max(0, int(raw.get(field) or raw.get("value") or 0))
        entries.append({"name": _title(name), "value": value, "level": 0, "rank": int(raw.get("rank") or -1), "ehp": 0})
    return entries


def _title(value: str) -> str:
    return value.replace("_", " ").title()
