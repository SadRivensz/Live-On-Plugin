import asyncio
import unittest
from unittest import mock

import httpx

from app.config import Settings
from app.runeprofile import RuneProfileClient


class RuneProfileClientTest(unittest.TestCase):
    def test_full_profile_is_cached_and_enriched(self):
        requests = 0

        async def handler(request):
            nonlocal requests
            requests += 1
            return httpx.Response(
                200,
                json={
                    "username": "Test Player",
                    "collectionLog": {"obtained": 1, "total": 2, "tabs": []},
                },
            )

        async def run():
            client = RuneProfileClient(Settings())
            await client.client.aclose()
            client.client = httpx.AsyncClient(
                base_url="https://api.runeprofile.test/v1",
                transport=httpx.MockTransport(handler),
            )
            first = await client.full_profile("Test Player")
            second = await client.full_profile("test_player")
            await client.close()
            return first, second

        # Fresh Linux runners can have a monotonic clock below the cache TTL.
        # A missing cache entry must never be mistaken for a recent entry.
        with mock.patch("app.runeprofile.time.monotonic", return_value=1):
            first, second = asyncio.run(run())
        self.assertEqual(1, requests)
        self.assertIs(first, second)
        self.assertEqual("https://runeprofile.com/Test%20Player", first["profileUrl"])


if __name__ == "__main__":
    unittest.main()
