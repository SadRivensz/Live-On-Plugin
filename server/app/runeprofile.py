from __future__ import annotations

import asyncio
import time
from typing import Any
from urllib.parse import quote

import httpx

from .config import Settings
from .security import normalize_rsn


class RuneProfileClient:
    """Small cached client for RuneProfile's public, read-only API."""

    CACHE_SECONDS = 120

    def __init__(self, config: Settings):
        headers = {"User-Agent": "Live-On-Clan/0.2.6"}
        if config.runeprofile_api_key:
            headers["X-API-Key"] = config.runeprofile_api_key
        self.client = httpx.AsyncClient(
            base_url=config.runeprofile_base_url.rstrip("/"),
            headers=headers,
            timeout=httpx.Timeout(25, connect=8),
        )
        self._cache: dict[str, tuple[float, dict[str, Any] | None]] = {}
        self._inflight: dict[str, asyncio.Task[dict[str, Any] | None]] = {}
        self._lock = asyncio.Lock()

    async def close(self) -> None:
        await self.client.aclose()

    async def full_profile(self, rsn: str) -> dict[str, Any] | None:
        key = normalize_rsn(rsn)
        cached_at, cached = self._cache.get(key, (0, None))
        if time.monotonic() - cached_at < self.CACHE_SECONDS:
            return cached

        async with self._lock:
            cached_at, cached = self._cache.get(key, (0, None))
            if time.monotonic() - cached_at < self.CACHE_SECONDS:
                return cached
            task = self._inflight.get(key)
            if task is None:
                task = asyncio.create_task(self._fetch_full_profile(rsn))
                self._inflight[key] = task

        try:
            profile = await task
            self._cache[key] = (time.monotonic(), profile)
            return profile
        finally:
            async with self._lock:
                if self._inflight.get(key) is task:
                    self._inflight.pop(key, None)

    async def _fetch_full_profile(self, rsn: str) -> dict[str, Any] | None:
        response = await self.client.get(f"/accounts/{quote(rsn, safe='')}/full")
        if response.status_code == 404:
            return None
        response.raise_for_status()
        payload = response.json()
        payload["profileUrl"] = f"https://runeprofile.com/{quote(str(payload.get('username') or rsn), safe='')}"
        return payload
