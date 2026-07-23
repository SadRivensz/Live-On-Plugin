from __future__ import annotations

import asyncio
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from pathlib import Path

import httpx
from fastapi import BackgroundTasks, Depends, FastAPI, Header, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse

from .config import settings
from .database import Database
from .discord import DiscordPublisher
from .schemas import (
    AnnouncementPayload,
    AuthRequest,
    CollectionLogPayload,
    DropPayload,
    ItemGoalPayload,
    PetPayload,
    StatusResponse,
)
from .media import save_screenshot, screenshot_path
from .runeprofile import RuneProfileClient
from .security import Session, TokenSigner, access_code_matches, normalize_rsn
from .site import LiveOnMemberSiteClient
from .wom import WiseOldManClient, profile_sections


settings.validate()
database = Database(settings)
tokens = TokenSigner(settings)
wom = WiseOldManClient(settings)
runeprofile = RuneProfileClient(settings)
discord = DiscordPublisher(
    settings.discord_webhook,
    settings.screenshot_directory,
    settings.public_base_url,
)
member_site = LiveOnMemberSiteClient(settings)


@asynccontextmanager
async def lifespan(_: FastAPI):
    database.initialize()
    warmup = asyncio.create_task(_warm_current_rankings())
    try:
        yield
    finally:
        warmup.cancel()
        await wom.close()
        await runeprofile.close()
        await discord.close()
        await member_site.close()


app = FastAPI(
    title="Live On Clan API",
    version="0.2.7",
    description="Companion API for the Live On RuneLite plugin.",
    lifespan=lifespan,
)


async def _warm_current_rankings() -> None:
    if settings.wom_group_id <= 0:
        return
    start, end = _month_range("current")
    try:
        await asyncio.gather(
            wom.rankings("xp", start, end),
            wom.rankings("ehp", start, end),
            wom.rankings("ehb", start, end),
            return_exceptions=True,
        )
    except (httpx.HTTPError, asyncio.CancelledError):
        return

if settings.cors_origins:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=list(settings.cors_origins),
        allow_credentials=False,
        allow_methods=["GET", "POST", "PUT", "DELETE"],
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
    return {"name": "Live On Clan API", "version": "0.2.7", "docs": "/docs"}


@app.get("/health")
async def health() -> dict:
    return {"status": "ok"}


@app.get("/assets/liveon-icon.png", include_in_schema=False)
async def clan_icon() -> FileResponse:
    path = Path(__file__).resolve().parents[2] / "icon.png"
    if not path.is_file():
        raise HTTPException(status_code=404, detail="Clan icon not found")
    return FileResponse(path, media_type="image/png")


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
    try:
        screenshot_name = save_screenshot(body.pop("screenshotBase64", None), settings.screenshot_directory)
    except ValueError as exception:
        raise HTTPException(status_code=422, detail=str(exception)) from exception
    drop_id = database.insert_drop(session.rsn, body, screenshot_name)
    goal = database.complete_matching_goal(session.rsn, body["items"], drop_id)
    body["screenshotPath"] = screenshot_name
    background.add_task(_safe_discord_drop, session.rsn, body)
    if goal:
        achievement = _goal_achievement(goal)
        announcement = {
            "title": "Objetivo conquistado!",
            "message": achievement["message"],
            "kind": "item_goal",
            "showOnLogin": False,
        }
        database.insert_announcement(session.rsn, announcement)
        background.add_task(_safe_discord_goal, session.rsn, achievement)
    else:
        achievement = None
    return StatusResponse(message="Drop registrado", achievement=achievement)


@app.get("/v1/drops")
async def list_drops(
    limit: int = Query(default=25, ge=1, le=50),
    page: int = Query(default=1, ge=1),
    _: Session = Depends(require_session),
) -> dict:
    total = database.count_drops()
    total_pages = max(1, (total + limit - 1) // limit)
    page = min(page, total_pages)
    rows = database.recent_drops(limit, offset=(page - 1) * limit)
    return {
        "drops": [_drop(row) for row in rows],
        "page": page,
        "pageSize": limit,
        "total": total,
        "totalPages": total_pages,
    }


@app.get("/v1/screenshots/{filename}", include_in_schema=False)
async def get_screenshot(filename: str) -> FileResponse:
    path = screenshot_path(settings.screenshot_directory, filename)
    if path is None:
        raise HTTPException(status_code=404, detail="Screenshot not found")
    return FileResponse(path, media_type="image/png" if path.suffix == ".png" else "image/jpeg")


@app.get("/v1/item-goal")
async def get_item_goal(session: Session = Depends(require_session)) -> dict:
    return {"goal": _goal(database.item_goal(session.rsn))}


@app.put("/v1/item-goal")
async def set_item_goal(payload: ItemGoalPayload, session: Session = Depends(require_session)) -> dict:
    return {"goal": _goal(database.set_item_goal(session.rsn, payload.itemId, payload.itemName))}


@app.delete("/v1/item-goal", response_model=StatusResponse)
async def clear_item_goal(session: Session = Depends(require_session)) -> StatusResponse:
    database.clear_item_goal(session.rsn)
    return StatusResponse(message="Objetivo removido")


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
    page: int = Query(default=1, ge=1),
    limit: int = Query(default=25, ge=1, le=50),
    _: Session = Depends(require_session),
) -> dict:
    members = await _all_members()
    key = normalize_rsn(query)
    filtered = [member for member in members if key in normalize_rsn(member["rsn"])]
    total = len(filtered)
    total_pages = max(1, (total + limit - 1) // limit)
    selected_page = min(page, total_pages)
    start = (selected_page - 1) * limit
    return {
        "members": filtered[start : start + limit],
        "page": selected_page,
        "pageSize": limit,
        "total": total,
        "totalPages": total_pages,
    }


@app.get("/v1/members/profile")
async def member_profile(
    rsn: str = Query(min_length=1, max_length=12),
    _: Session = Depends(require_session),
) -> dict:
    member = await _official_member(rsn)
    if member is None:
        raise HTTPException(status_code=404, detail="Member not found")

    wom_request = wom.player_profile(member["rsn"]) if settings.wom_group_id > 0 else asyncio.sleep(0, result=None)
    wom_result, site_result = await asyncio.gather(
        wom_request,
        member_site.member(member["rsn"]),
        return_exceptions=True,
    )

    skills: list[dict] = []
    bosses: list[dict] = []
    activities: list[dict] = []
    last_updated = None
    if isinstance(wom_result, dict):
        skills, bosses, activities, last_updated = profile_sections(wom_result)
    if isinstance(site_result, dict) and site_result:
        member = {**member, **site_result}

    return {
        "member": member,
        "lastUpdated": last_updated,
        "skills": skills,
        "bosses": bosses,
        "activities": activities,
        "recentDrops": [_drop(row) for row in database.recent_drops(10, member["rsn"])],
        "collectionLog": database.collection_entries(member["rsn"]),
    }


@app.get("/v1/members/runeprofile")
async def member_runeprofile(
    rsn: str = Query(min_length=1, max_length=12),
    _: Session = Depends(require_session),
) -> dict:
    member = await _official_member(rsn)
    if member is None:
        raise HTTPException(status_code=404, detail="Member not found")
    try:
        profile = await runeprofile.full_profile(member["rsn"])
    except httpx.HTTPError as exception:
        raise HTTPException(status_code=502, detail="RuneProfile unavailable") from exception
    if profile is None:
        raise HTTPException(status_code=404, detail="Perfil não encontrado no RuneProfile")
    return {"profile": profile}


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
            "accountType": "regular",
            "country": None,
            "avatarUrl": None,
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
        {
            "rsn": row["display_name"],
            "role": row["role"],
            "totalXp": 0,
            "ehp": 0,
            "ehb": 0,
            "accountType": "regular",
            "country": None,
            "avatarUrl": None,
        }
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
        "screenshotUrl": _screenshot_url(row.get("screenshot_path")),
    }


def _screenshot_url(filename: str | None) -> str | None:
    if not filename:
        return None
    path = f"/v1/screenshots/{filename}"
    return settings.screenshot_base_url.rstrip("/") + path if settings.screenshot_base_url else path


def _goal(row: dict | None) -> dict | None:
    if not row:
        return None
    return {
        "id": row["id"],
        "playerName": row["player_name"],
        "itemId": row["item_id"],
        "itemName": row["item_name"],
        "startedAt": row["started_at"],
    }


def _goal_achievement(row: dict) -> dict:
    started = datetime.fromisoformat(str(row["started_at"]).replace("Z", "+00:00")).replace(tzinfo=timezone.utc)
    achieved = datetime.fromisoformat(str(row["achieved_at"]).replace("Z", "+00:00")).replace(tzinfo=timezone.utc)
    days = max(0, (achieved - started).days)
    if days >= 60:
        months = max(1, round(days / 30))
        elapsed = f"{months} meses"
    elif days == 1:
        elapsed = "1 dia"
    else:
        elapsed = f"{days} dias"
    return {
        "itemId": row["item_id"],
        "itemName": row["item_name"],
        "startedAt": row["started_at"],
        "achievedAt": row["achieved_at"],
        "elapsedDays": days,
        "message": (
            f"Parabéns, {row['player_name']}! Dry encerrado: conseguiu "
            f"{row['item_name']} após {elapsed} de busca."
        ),
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


async def _safe_discord_goal(player: str, achievement: dict) -> None:
    try:
        await discord.goal(player, achievement)
    except httpx.HTTPError:
        pass
