import asyncio
import json
import tempfile
import unittest
from pathlib import Path

import httpx

from app.discord import DiscordPublisher


class DiscordPublisherTest(unittest.TestCase):
    def test_drop_embed_links_screenshot_and_uses_clan_footer(self):
        captured = {}

        async def handler(request):
            captured["content_type"] = request.headers.get("content-type", "")
            captured["body"] = await request.aread()
            return httpx.Response(204)

        async def run():
            with tempfile.TemporaryDirectory() as temp:
                screenshot = Path(temp, "drop.png")
                screenshot.write_bytes(b"\x89PNG\r\n\x1a\nfixture")
                publisher = DiscordPublisher(
                    "https://discord.test/webhook",
                    temp,
                    "https://liveonplugin.discloud.app",
                )
                await publisher.client.aclose()
                publisher.client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
                await publisher.drop(
                    "Sad Riven",
                    {
                        "source": "The Corrupted Gauntlet",
                        "totalValue": 5_790_000,
                        "items": [
                            {
                                "name": "Crystal armour seed",
                                "quantity": 1,
                            }
                        ],
                        "screenshotPath": screenshot.name,
                    },
                )
                await publisher.close()

        asyncio.run(run())
        body = captured["body"].decode("utf-8", errors="replace")
        self.assertIn("multipart/form-data", captured["content_type"])
        self.assertIn("attachment://drop.png", body)
        self.assertIn(
            "https://liveonplugin.discloud.app/v1/screenshots/drop.png",
            body,
        )
        self.assertIn(
            "https://liveonplugin.discloud.app/assets/liveon-icon.png",
            body,
        )
        self.assertIn("Live On Clan", body)
        self.assertIn("Drops do Live On", body)

    def test_goal_embed_does_not_repeat_drop_screenshot(self):
        captured = {}

        async def handler(request):
            captured["content_type"] = request.headers.get("content-type", "")
            captured["body"] = await request.aread()
            return httpx.Response(204)

        async def run():
            publisher = DiscordPublisher("https://discord.test/webhook")
            await publisher.client.aclose()
            publisher.client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
            await publisher.goal(
                "Sad Riven",
                {
                    "message": "Parabéns, Sad Riven! Dry encerrado.",
                    "itemName": "Enhanced crystal weapon seed",
                },
            )
            await publisher.close()

        asyncio.run(run())
        body = json.loads(captured["body"].decode("utf-8"))
        self.assertIn("application/json", captured["content_type"])
        self.assertNotIn("image", body["embeds"][0])
        self.assertIn("Dry encerrado", body["embeds"][0]["description"])


if __name__ == "__main__":
    unittest.main()
