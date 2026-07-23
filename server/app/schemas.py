from __future__ import annotations

from pydantic import BaseModel, Field, field_validator


class AuthRequest(BaseModel):
    rsn: str = Field(min_length=1, max_length=12)
    clanName: str = Field(min_length=1, max_length=40)
    clanRank: str = Field(default="member", max_length=40)
    accessCode: str = Field(default="", max_length=128)
    pluginVersion: str = Field(default="unknown", max_length=30)

    @field_validator("rsn", "clanName", "clanRank")
    @classmethod
    def strip_text(cls, value: str) -> str:
        return value.strip()


class ItemPayload(BaseModel):
    itemId: int = Field(ge=0)
    name: str = Field(min_length=1, max_length=120)
    quantity: int = Field(ge=1, le=2_147_483_647)
    unitPrice: int = Field(ge=0)
    totalPrice: int = Field(ge=0)


class DropPayload(BaseModel):
    source: str = Field(min_length=1, max_length=120)
    sourceType: str = Field(min_length=1, max_length=30)
    npcId: int | None = None
    killCount: int | None = Field(default=None, ge=0)
    totalValue: int = Field(ge=0)
    items: list[ItemPayload] = Field(min_length=1, max_length=80)
    screenshotBase64: str | None = Field(default=None, max_length=10_500_000)

    @field_validator("totalValue")
    @classmethod
    def sensible_total(cls, value: int) -> int:
        return min(value, 9_000_000_000_000_000)


class PetPayload(BaseModel):
    petName: str = Field(min_length=1, max_length=120)
    source: str = Field(default="game_message", max_length=80)
    gameMessage: str = Field(default="", max_length=500)


class CollectionLogPayload(BaseModel):
    itemName: str = Field(min_length=1, max_length=120)
    source: str = Field(default="collection_log_message", max_length=80)


class AnnouncementPayload(BaseModel):
    title: str = Field(min_length=1, max_length=100)
    message: str = Field(min_length=1, max_length=1200)
    kind: str = Field(default="clan", max_length=30)
    showOnLogin: bool = False


class ItemGoalPayload(BaseModel):
    itemId: int = Field(gt=0)
    itemName: str = Field(min_length=1, max_length=120)

    @field_validator("itemName")
    @classmethod
    def strip_item_name(cls, value: str) -> str:
        return value.strip()


class StatusResponse(BaseModel):
    ok: bool = True
    message: str = "ok"
    achievement: dict | None = None
