from __future__ import annotations

import json
import sqlite3
from contextlib import contextmanager
from pathlib import Path
from typing import Iterator

from .config import Settings
from .security import normalize_rsn


SCHEMA = """
CREATE TABLE IF NOT EXISTS members (
    rsn_key TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'member',
    active INTEGER NOT NULL DEFAULT 1,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS drops (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_name TEXT NOT NULL,
    source TEXT NOT NULL,
    source_type TEXT NOT NULL,
    npc_id INTEGER,
    kill_count INTEGER,
    total_value INTEGER NOT NULL,
    items_json TEXT NOT NULL,
    screenshot_path TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS pets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_name TEXT NOT NULL,
    pet_name TEXT NOT NULL,
    source TEXT,
    game_message TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS collection_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_name TEXT NOT NULL,
    item_name TEXT NOT NULL,
    source TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS announcements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    kind TEXT NOT NULL DEFAULT 'clan',
    author TEXT NOT NULL,
    show_on_login INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS item_goals (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_key TEXT NOT NULL,
    player_name TEXT NOT NULL,
    item_id INTEGER NOT NULL,
    item_name TEXT NOT NULL,
    started_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    achieved_at TEXT,
    achieved_drop_id INTEGER,
    active INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_drops_created_at ON drops(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_collection_player ON collection_log(player_name, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_item_goals_active_player
ON item_goals(player_key) WHERE active=1;
"""


class Database:
    def __init__(self, config: Settings):
        self.path = Path(config.database_path)
        self.bootstrap_members = config.bootstrap_members

    def initialize(self) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self.connect() as connection:
            connection.executescript(SCHEMA)
            columns = {row["name"] for row in connection.execute("PRAGMA table_info(drops)")}
            if "screenshot_path" not in columns:
                connection.execute("ALTER TABLE drops ADD COLUMN screenshot_path TEXT")
            for entry in self.bootstrap_members:
                display_name, _, role = entry.partition(":")
                role = role or "member"
                connection.execute(
                    """INSERT INTO members(rsn_key, display_name, role)
                       VALUES (?, ?, ?)
                       ON CONFLICT(rsn_key) DO UPDATE SET display_name=excluded.display_name, role=excluded.role""",
                    (normalize_rsn(display_name), display_name.strip(), role.strip()),
                )

    @contextmanager
    def connect(self) -> Iterator[sqlite3.Connection]:
        connection = sqlite3.connect(self.path, timeout=10)
        connection.row_factory = sqlite3.Row
        try:
            yield connection
            connection.commit()
        finally:
            connection.close()

    def local_member(self, rsn: str) -> dict | None:
        with self.connect() as connection:
            row = connection.execute(
                "SELECT display_name, role FROM members WHERE rsn_key=? AND active=1",
                (normalize_rsn(rsn),),
            ).fetchone()
        return dict(row) if row else None

    def local_members(self, query: str = "") -> list[dict]:
        with self.connect() as connection:
            rows = connection.execute(
                """SELECT display_name, role FROM members
                   WHERE active=1 AND rsn_key LIKE ? ORDER BY display_name LIMIT 50""",
                (f"%{normalize_rsn(query)}%",),
            ).fetchall()
        return [dict(row) for row in rows]

    def insert_drop(self, player: str, payload: dict, screenshot_path: str | None = None) -> int:
        with self.connect() as connection:
            cursor = connection.execute(
                """INSERT INTO drops(player_name, source, source_type, npc_id, kill_count, total_value, items_json, screenshot_path)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                (
                    player,
                    payload["source"],
                    payload["sourceType"],
                    payload.get("npcId"),
                    payload.get("killCount"),
                    payload["totalValue"],
                    json.dumps(payload["items"], separators=(",", ":")),
                    screenshot_path,
                ),
            )
            return int(cursor.lastrowid)

    def item_goal(self, player: str) -> dict | None:
        with self.connect() as connection:
            row = connection.execute(
                "SELECT * FROM item_goals WHERE player_key=? AND active=1 ORDER BY id DESC LIMIT 1",
                (normalize_rsn(player),),
            ).fetchone()
        return dict(row) if row else None

    def set_item_goal(self, player: str, item_id: int, item_name: str) -> dict:
        with self.connect() as connection:
            connection.execute(
                "UPDATE item_goals SET active=0 WHERE player_key=? AND active=1",
                (normalize_rsn(player),),
            )
            cursor = connection.execute(
                """INSERT INTO item_goals(player_key, player_name, item_id, item_name)
                   VALUES (?, ?, ?, ?)""",
                (normalize_rsn(player), player, item_id, item_name),
            )
            row = connection.execute("SELECT * FROM item_goals WHERE id=?", (cursor.lastrowid,)).fetchone()
        return dict(row)

    def clear_item_goal(self, player: str) -> bool:
        with self.connect() as connection:
            cursor = connection.execute(
                "UPDATE item_goals SET active=0 WHERE player_key=? AND active=1",
                (normalize_rsn(player),),
            )
        return cursor.rowcount > 0

    def complete_matching_goal(self, player: str, items: list[dict], drop_id: int) -> dict | None:
        ids = {int(item.get("itemId") or 0) for item in items}
        names = {str(item.get("name") or "").strip().casefold() for item in items}
        with self.connect() as connection:
            row = connection.execute(
                "SELECT * FROM item_goals WHERE player_key=? AND active=1 ORDER BY id DESC LIMIT 1",
                (normalize_rsn(player),),
            ).fetchone()
            if row is None or (int(row["item_id"]) not in ids and str(row["item_name"]).casefold() not in names):
                return None
            connection.execute(
                """UPDATE item_goals SET active=0, achieved_at=CURRENT_TIMESTAMP, achieved_drop_id=?
                   WHERE id=? AND active=1""",
                (drop_id, row["id"]),
            )
            completed = connection.execute("SELECT * FROM item_goals WHERE id=?", (row["id"],)).fetchone()
        return dict(completed)

    def recent_drops(self, limit: int, player: str | None = None, offset: int = 0) -> list[dict]:
        query = "SELECT * FROM drops"
        params: list[object] = []
        if player:
            query += " WHERE lower(player_name)=?"
            params.append(player.lower())
        query += " ORDER BY id DESC LIMIT ? OFFSET ?"
        params.extend((limit, offset))
        with self.connect() as connection:
            rows = connection.execute(query, params).fetchall()
        result = []
        for row in rows:
            value = dict(row)
            value["items"] = json.loads(value.pop("items_json"))
            result.append(value)
        return result

    def count_drops(self, player: str | None = None) -> int:
        query = "SELECT COUNT(*) AS total FROM drops"
        params: list[object] = []
        if player:
            query += " WHERE lower(player_name)=?"
            params.append(player.lower())
        with self.connect() as connection:
            row = connection.execute(query, params).fetchone()
        return int(row["total"])

    def insert_pet(self, player: str, payload: dict) -> int:
        with self.connect() as connection:
            cursor = connection.execute(
                "INSERT INTO pets(player_name, pet_name, source, game_message) VALUES (?, ?, ?, ?)",
                (player, payload["petName"], payload.get("source"), payload.get("gameMessage")),
            )
            return int(cursor.lastrowid)

    def insert_collection_log(self, player: str, payload: dict) -> int:
        with self.connect() as connection:
            cursor = connection.execute(
                "INSERT INTO collection_log(player_name, item_name, source) VALUES (?, ?, ?)",
                (player, payload["itemName"], payload.get("source")),
            )
            return int(cursor.lastrowid)

    def collection_entries(self, player: str, limit: int = 20) -> list[str]:
        with self.connect() as connection:
            rows = connection.execute(
                """SELECT item_name FROM collection_log
                   WHERE lower(player_name)=? ORDER BY id DESC LIMIT ?""",
                (player.lower(), limit),
            ).fetchall()
        return [str(row["item_name"]) for row in rows]

    def announcements(self, limit: int = 10) -> list[dict]:
        with self.connect() as connection:
            rows = connection.execute(
                "SELECT * FROM announcements WHERE active=1 ORDER BY id DESC LIMIT ?",
                (limit,),
            ).fetchall()
        return [dict(row) for row in rows]

    def insert_announcement(self, author: str, payload: dict) -> dict:
        with self.connect() as connection:
            if payload.get("showOnLogin"):
                connection.execute("UPDATE announcements SET show_on_login=0")
            cursor = connection.execute(
                """INSERT INTO announcements(title, message, kind, author, show_on_login)
                   VALUES (?, ?, ?, ?, ?)""",
                (
                    payload["title"],
                    payload["message"],
                    payload.get("kind", "clan"),
                    author,
                    1 if payload.get("showOnLogin") else 0,
                ),
            )
            row = connection.execute("SELECT * FROM announcements WHERE id=?", (cursor.lastrowid,)).fetchone()
        return dict(row)

    def login_announcement(self) -> str | None:
        with self.connect() as connection:
            row = connection.execute(
                """SELECT message FROM announcements
                   WHERE active=1 AND show_on_login=1 ORDER BY id DESC LIMIT 1"""
            ).fetchone()
        return str(row["message"]) if row else None
