from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path

import httpx


class DiscordPublisher:
    GOLD = 0xFFC83D

    def __init__(
        self,
        webhook_url: str,
        screenshot_directory: str = "./data/screenshots",
        public_base_url: str = "",
    ):
        self.webhook_url = webhook_url
        self.screenshot_directory = Path(screenshot_directory)
        self.public_base_url = public_base_url.rstrip("/")
        self.client = httpx.AsyncClient(timeout=15)

    async def close(self) -> None:
        await self.client.aclose()

    async def drop(self, player: str, payload: dict) -> None:
        if not self.webhook_url:
            return
        items = "\n".join(f"{item['quantity']}x {item['name']}" for item in payload["items"][:10])
        await self._send(
            title="Drops do Live On",
            description=f"**{player}** recebeu loot de **{payload['source']}**.",
            fields=[
                {"name": "Itens", "value": items or "--", "inline": False},
                {"name": "Valor", "value": f"{payload['totalValue']:,} gp", "inline": True},
            ],
            screenshot_name=payload.get("screenshotPath"),
        )

    async def goal(self, player: str, achievement: dict) -> None:
        if not self.webhook_url:
            return
        await self._send(
            title="Objetivo conquistado!",
            description=achievement["message"],
            fields=[{"name": "Item desejado", "value": achievement["itemName"], "inline": True}],
        )

    async def pet(self, player: str, payload: dict) -> None:
        if not self.webhook_url:
            return
        await self._send(
            title="Novo pet!",
            description=f"**{player}** recebeu **{payload['petName']}**.",
            fields=[{"name": "Mensagem", "value": payload.get("gameMessage") or "--", "inline": False}],
        )

    async def announcement(self, author: str, payload: dict) -> None:
        if not self.webhook_url:
            return
        await self._send(
            title=payload["title"],
            description=payload["message"],
            fields=[{"name": "Staff", "value": author, "inline": True}],
        )

    async def _send(
        self,
        title: str,
        description: str,
        fields: list[dict],
        screenshot_name: str | None = None,
    ) -> None:
        payload = {
            "username": "Live On",
            "allowed_mentions": {"parse": []},
            "embeds": [
                {
                    "title": title,
                    "description": description,
                    "color": self.GOLD,
                    "fields": fields,
                    "footer": {
                        "text": "Live On Clan",
                        **(
                            {"icon_url": f"{self.public_base_url}/assets/liveon-icon.png"}
                            if self.public_base_url
                            else {}
                        ),
                    },
                    "timestamp": datetime.now(timezone.utc).isoformat(),
                }
            ],
        }
        screenshot = self.screenshot_directory / screenshot_name if screenshot_name else None
        if screenshot is not None and screenshot.is_file():
            public_screenshot = (
                f"{self.public_base_url}/v1/screenshots/{screenshot.name}"
                if self.public_base_url
                else None
            )
            payload["embeds"][0]["image"] = {"url": f"attachment://{screenshot.name}"}
            if public_screenshot:
                payload["embeds"][0]["url"] = public_screenshot
                payload["embeds"][0]["description"] += (
                    f"\n\n[🔎 Abrir screenshot em tamanho completo]({public_screenshot})"
                )
            response = await self.client.post(
                self.webhook_url,
                data={"payload_json": json.dumps(payload)},
                files={"files[0]": (screenshot.name, screenshot.read_bytes(), "image/png" if screenshot.suffix == ".png" else "image/jpeg")},
            )
        else:
            response = await self.client.post(self.webhook_url, json=payload)
        response.raise_for_status()
