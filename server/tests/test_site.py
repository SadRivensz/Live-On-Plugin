import asyncio
import unittest
from unittest import mock

import httpx

from app.config import Settings
from app.site import LiveOnMemberSiteClient


class LiveOnMemberSiteClientTest(unittest.TestCase):
    def test_missing_member_is_cached_on_a_fresh_runner(self):
        requests = 0

        async def handler(request):
            nonlocal requests
            requests += 1
            return httpx.Response(200, json={"players": []})

        async def run():
            client = LiveOnMemberSiteClient(
                Settings(member_site_base_url="https://liveon.test")
            )
            await client.client.aclose()
            client.client = httpx.AsyncClient(
                base_url="https://liveon.test",
                transport=httpx.MockTransport(handler),
            )
            first = await client.member("Test Player")
            second = await client.member("test_player")
            await client.close()
            return first, second

        with mock.patch("app.site.time.monotonic", return_value=1):
            first, second = asyncio.run(run())

        self.assertEqual(1, requests)
        self.assertIsNone(first)
        self.assertIsNone(second)


if __name__ == "__main__":
    unittest.main()
