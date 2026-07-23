import time
import unittest

from app.config import Settings
from app.security import TokenSigner, access_code_matches, normalize_rsn


class SecurityTest(unittest.TestCase):
    def test_token_round_trip(self):
        signer = TokenSigner(Settings(token_secret="x" * 32, token_lifetime_seconds=60))
        token, _ = signer.issue("Sad Rivensz", "administrator", True)
        session = signer.verify(token)
        self.assertIsNotNone(session)
        self.assertEqual("Sad Rivensz", session.rsn)
        self.assertTrue(session.staff)

    def test_normalize_rsn(self):
        self.assertEqual("live on", normalize_rsn(" Live__On "))

    def test_access_code_hash(self):
        self.assertTrue(access_code_matches("abc", "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"))


if __name__ == "__main__":
    unittest.main()
