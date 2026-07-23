from __future__ import annotations

from contextlib import asynccontextmanager
from datetime import datetime, timezone

import httpx
from fastapi import BackgroundTasks, Depends, FastAPI, Header, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware

from .config import settings
from .database import Database
from .discord import DiscordPublisher
from .schemas import (
    AnnouncementPayload,
    AuthRequest,
    CollectionLogPayload,
    DropPayload,
    PetPayload,
    StatusResponse,
)
from .security import Session, TokenSigner, access_code_matches, normalize_rsn
from .wom import WiseOldManClient, profile_sections


settings.validate()
database = Database(settings)
tokens = TokenSigner(settings)
wom = WiseOldManClient(settings)
discord = DiscordPublisher(settings.discord_webhook)


@asynccontextmanager
async def lifespan(_: FastAPI):
    database.initialize()
    yield
    await wom.close()
    await discord.close()


app = FastAPI(
    title="Live On Clan API",
    version="0.1.0",
    description="Companion API for the Live On RuneLite plugin.",
    lifespan=lifespan,
)

if settings.cors_origins:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=list(settings.cors_origins),
        allow_credentials=False,
        allow_methods=["GET", "POST"],
        allow_headers=["Authorization", "Content-Type"],
    )


def require_session(authorization: str | None = Header(default=None)) -> Session:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing bearer token")
    session = tokens.verify(authorization[7:].strip())
    if session is None:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    return session


def require_staff(session: Session = Depends(require_session)) -> Session:
    if not session.staff:
        raise HTTPException(status_code=403, detail="Staff access required")
    return session


@app.get("/")
async def root() -> dict:
    return {"name": "Live On Clan API", "version": "0.1.0", "docs": "/docs"}


@app.get("/health")
async def health() -> dict:
    return {"status": "ok"}


@app.post("/v1/auth/verify")
async def verify_access(request: AuthRequest) -> dict:
    if request.clanName.casefold() != "live on":
        return {"authorized": False, "staff": False, "reason": "O personagem não está no clã Live On"}
    if not access_code_matches(request.accessCode, settings.access_code_sha256):
        return {"authorized": False, "staff": False, "reason": "Código de acesso inválido"}

    member = await _official_member(request.rsn)
    if member is None:
        return {"authorized": False, "staff": False, "reason": "RSN não encontrado na lista oficial da Live On"}

    role = str(member.get("role") or request.clanRank or "member")
    staff = _is_staff(request.rsn, role)
    token, expires_at = tokens.issue(member.get("rsn") or request.rsn, role, staff)
    return {
        "authorized": True,
        "staff": staff,
        "token": token,
        "role": role,
        "loginMessage": database.login_announcement() or settings.login_message,
        "reason": None,
        "expiresAt": datetime.fromtimestamp(expires_at, timezone.utc).isoformat(),
    }


@app.get("/v1/announcements")
async def list_announcements(_: Session = Depends(require_session)) -> dict:
    return {"announcements": [_announcement(row) for row in database.announcements()]}


@app.post("/v1/announcements")
async def create_announcement(
    payload: AnnouncementPayload,
    background: BackgroundTasks,
    session: Session = Depends(require_staff),
) -> dict:
    body = payload.model_dump()
    row = database.insert_announcement(session.rsn, body)
    background.add_task(_safe_discord_announcement, session.rsn, body)
    return _announcement(row)


@app.post("/v1/drops", response_model=StatusResponse)
async def create_drop(
    payload: DropPayload,
    background: BackgroundTasks,
    session: Session = Depends(require_session),
) -> StatusResponse:
    body = payload.model_dump()
    calculated_total = sum(item["unitPrice"] * item["quantity"] for item in body["items"])
    body["totalValue"] = calculated_total
    database.insert_drop(session.rsn, body)
    background.add_task(_safe_discord_drop, session.rsn, body)
    return StatusResponse(message="Drop registrado")


@app.get("/v1/drops")
async def list_drops(
    limit: int = Query(default=25, ge=1, le=100),
    _: Session = Depends(require_session),
) -> dict:
    return {"drops": [_drop(row) for row in database.recent_drops(limit)]}


@app.post("/v1/pets", response_model=StatusResponse)
async def create_pet(
    payload: PetPayload,
    background: BackgroundTasks,
    session: Session = Depends(require_session),
) -> StatusResponse:
    body = payload.model_dump()
    database.insert_pet(session.rsn, body)
    background.add_task(_safe_discord_pet, session.rsn, body)
    return StatusResponse(message="Pet registrado")


@app.post("/v1/collection-log", response_model=StatusResponse)
async def create_collection_log(
    payload: CollectionLogPayload,
    session: Session = Depends(require_session),
) -> StatusResponse:
    database.insert_collection_log(session.rsn, payload.model_dump())
    return StatusResponse(message="Collection log registrado")


@app.get("/v1/members")
async def search_members(
    query: str = Query(default="", max_length=12),
    _: Session = Depends(require_session),
) -> dict:
    members = await _all_members()
    key = normalize_rsn(query)
    filtered = [member for member in members if key in normalize_rsn(member["rsn"])][:50]
    return {"members": filtered}


@app.get("/v1/members/profile")
async def member_profile(
    rsn: str = Query(min_length=1, max_length=12),
    _: Session = Depends(require_session),
) -> dict:
    member = await _official_member(rsn)
    if member is None:
        raise HTTPException(status_code=404, detail="Member not found")

    skills: list[dict] = []
    bosses: list[dict] = []
    activities: list[dict] = []
    last_updated = None
    if settings.wom_group_id > 0:
        try:
            profile = await wom.player_profile(member["rsn"])
            skills, bosses, activities, last_updated = profile_sections(profile)
        except httpx.HTTPError:
            pass

    return {
        "member": member,
        "lastUpdated": last_updated,
        "skills": skills,
        "bosses": bosses,
        "activities": activities,
        "recentDrops": [_drop(row) for row in database.recent_drops(10, member["rsn"])],
        "collectionLog": database.collection_entries(member["rsn"]),
    }


@app.get("/v1/rankings")
async def rankings(
    metric: str = Query(pattern="^(xp|ehp|ehb)$"),
    period: str = Query(pattern="^(current|previous)$"),
    _: Session = Depends(require_session),
) -> dict:
    start, end = _month_range(period)
    try:
        entries = await wom.rankings(metric, start, end)
    except httpx.HTTPError as exception:
        raise HTTPException(status_code=502, detail="Wise Old Man unavailable") from exception
    return {
        "metric": metric,
        "period": period,
        "startsAt": start.isoformat(),
        "endsAt": end.isoformat(),
        "entries": [
            {"rank": index, "rsn": entry["rsn"], "value": entry["value"]}
            for index, entry in enumerate(entries[:50], start=1)
        ],
    }


async def _official_member(rsn: str) -> dict | None:
    if settings.wom_group_id > 0:
        try:
            member = await wom.member(rsn)
            if member is not None:
                return member
        except httpx.HTTPError:
            if settings.environment == "production":
                return None
    local = database.local_member(rsn)
    if local:
        return {
            "rsn": local["display_name"],
            "role": local["role"],
            "totalXp": 0,
            "ehp": 0,
            "ehb": 0,
        }
    return None


async def _all_members() -> list[dict]:
    if settings.wom_group_id > 0:
        try:
            members = await wom.members()
            if members:
                return members
        except httpx.HTTPError:
            pass
    return [
        {"rsn": row["display_name"], "role": row["role"], "totalXp": 0, "ehp": 0, "ehb": 0}
        for row in database.local_members()
    ]


def _is_staff(rsn: str, role: str) -> bool:
    configured = {normalize_rsn(value) for value in settings.staff_rsns}
    staff_roles = {"moderator", "administrator", "deputy owner", "owner", "deputy_owner"}
    return normalize_rsn(rsn) in configured or role.casefold().replace("_", " ") in staff_roles


def _month_range(period: str) -> tuple[datetime, datetime]:
    now = datetime.now(timezone.utc)
    current_start = datetime(now.year, now.month, 1, tzinfo=timezone.utc)
    if period == "current":
        return current_start, now
    previous_end = current_start
    if now.month == 1:
        previous_start = datetime(now.year - 1, 12, 1, tzinfo=timezone.utc)
    else:
        previous_start = datetime(now.year, now.month - 1, 1, tzinfo=timezone.utc)
    return previous_start, previous_end


def _announcement(row: dict) -> dict:
    return {
        "id": row["id"],
        "title": row["title"],
        "message": row["message"],
        "kind": row["kind"],
        "author": row["author"],
        "showOnLogin": bool(row["show_on_login"]),
        "createdAt": row["created_at"],
    }


def _drop(row: dict) -> dict:
    return {
        "id": row["id"],
        "playerName": row["player_name"],
        "source": row["source"],
        "totalValue": row["total_value"],
        "createdAt": row["created_at"],
        "items": row["items"],
    }


async def _safe_discord_drop(player: str, payload: dict) -> None:
    try:
        await discord.drop(player, payload)
    except httpx.HTTPError:
        pass


async def _safe_discord_pet(player: str, payload: dict) -> None:
    try:
        await discord.pet(player, payload)
    except httpx.HTTPError:
        pass


async def _safe_discord_announcement(author: str, payload: dict) -> None:
    try:
        await discord.announcement(author, payload)
    except httpx.HTTPError:
        pass
