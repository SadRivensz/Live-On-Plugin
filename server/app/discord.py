from __future__ import annotations

import httpx


class DiscordPublisher:
    GOLD = 0xFFC83D

    def __init__(self, webhook_url: str):
        self.webhook_url = webhook_url
        self.client = httpx.AsyncClient(timeout=15)

    async def close(self) -> None:
        await self.client.aclose()

    async def drop(self, player: str, payload: dict) -> None:
        if not self.webhook_url:
            return
        items = "\n".join(f"{item['quantity']}x {item['name']}" for item in payload["items"][:10])
        await self._send(
            title="Drop da Live On",
            description=f"**{player}** recebeu loot de **{payload['source']}**.",
            fields=[
                {"name": "Itens", "value": items or "--", "inline": False},
                {"name": "Valor", "value": f"{payload['totalValue']:,} gp", "inline": True},
            ],
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

    async def _send(self, title: str, description: str, fields: list[dict]) -> None:
        response = await self.client.post(
            self.webhook_url,
            json={
                "username": "Live On",
                "allowed_mentions": {"parse": []},
                "embeds": [{"title": title, "description": description, "color": self.GOLD, "fields": fields}],
            },
        )
        response.raise_for_status()
