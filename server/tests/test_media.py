import base64
import tempfile
import unittest
from pathlib import Path

from app.media import save_screenshot, screenshot_path


class MediaTest(unittest.TestCase):
    def test_saves_valid_png_and_rejects_other_data(self):
        with tempfile.TemporaryDirectory() as temp:
            tiny_png = b"\x89PNG\r\n\x1a\n" + b"test"
            filename = save_screenshot(base64.b64encode(tiny_png).decode(), temp)
            self.assertTrue(filename.endswith(".png"))
            self.assertEqual(tiny_png, Path(temp, filename).read_bytes())
            self.assertEqual(Path(temp, filename), screenshot_path(temp, filename))
            with self.assertRaises(ValueError):
                save_screenshot(base64.b64encode(b"not an image").decode(), temp)


if __name__ == "__main__":
    unittest.main()
