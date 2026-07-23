import tempfile
import unittest
from pathlib import Path

from app.config import Settings
from app.database import Database


class DatabaseTest(unittest.TestCase):
    def test_bootstrap_member_and_drop(self):
        with tempfile.TemporaryDirectory() as temp:
            database = Database(
                Settings(
                    database_path=str(Path(temp) / "test.db"),
                    bootstrap_members=("Test Player:member",),
                )
            )
            database.initialize()
            self.assertEqual("member", database.local_member("test_player")["role"])
            database.insert_drop(
                "Test Player",
                {
                    "source": "Nex",
                    "sourceType": "npc",
                    "totalValue": 10,
                    "items": [{"itemId": 1, "name": "Item", "quantity": 1, "unitPrice": 10, "totalPrice": 10}],
                },
            )
            self.assertEqual(1, len(database.recent_drops(10)))


if __name__ == "__main__":
    unittest.main()
