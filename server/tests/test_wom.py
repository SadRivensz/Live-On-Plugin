import asyncio
import unittest
from datetime import datetime, timezone

import httpx

from app.config import Settings
from app.wom import WiseOldManClient
from app.wom import _score_entries


class WiseOldManProfileTest(unittest.TestCase):
    def test_score_entries_keep_wom_order_and_unranked_metrics(self):
        metrics = {
            f"boss_{index}": {"kills": index if index % 2 else -1, "rank": index}
            for index in range(70)
        }

        entries = _score_entries(metrics, "kills")

        self.assertEqual(70, len(entries))
        self.assertEqual("Boss 0", entries[0]["name"])
        self.assertEqual(0, entries[0]["value"])
        self.assertEqual("Boss 69", entries[-1]["name"])
        self.assertEqual(69, entries[-1]["value"])

    def test_monthly_ranking_reuses_cache_when_current_end_changes(self):
        requests = 0

        async def handler(request):
            nonlocal requests
            requests += 1
            return httpx.Response(
                200,
                json=[{"player": {"displayName": "Player"}, "data": {"gained": 42}}],
            )

        async def run():
            client = WiseOldManClient(Settings(wom_group_id=1945))
            await client.client.aclose()
            client.client = httpx.AsyncClient(
                base_url="https://api.wiseoldman.test/v2",
                transport=httpx.MockTransport(handler),
            )
            start = datetime(2026, 7, 1, tzinfo=timezone.utc)
            first = await client.rankings("xp", start, datetime(2026, 7, 23, 1, tzinfo=timezone.utc))
            second = await client.rankings("xp", start, datetime(2026, 7, 23, 2, tzinfo=timezone.utc))
            await client.close()
            return first, second

        first, second = asyncio.run(run())
        self.assertEqual(1, requests)
        self.assertIs(first, second)


if __name__ == "__main__":
    unittest.main()
