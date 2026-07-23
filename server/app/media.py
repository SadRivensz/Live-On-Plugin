from __future__ import annotations

import base64
import binascii
import uuid
from pathlib import Path


MAX_SCREENSHOT_BYTES = 7 * 1024 * 1024


def save_screenshot(encoded: str | None, directory: str) -> str | None:
    if not encoded:
        return None
    value = encoded.split(",", 1)[-1]
    try:
        image = base64.b64decode(value, validate=True)
    except (binascii.Error, ValueError) as exception:
        raise ValueError("Screenshot inválida") from exception
    if not image or len(image) > MAX_SCREENSHOT_BYTES:
        raise ValueError("Screenshot excede o limite de 7 MB")
    if image.startswith(b"\x89PNG\r\n\x1a\n"):
        extension = ".png"
    elif image.startswith(b"\xff\xd8\xff"):
        extension = ".jpg"
    else:
        raise ValueError("Apenas screenshots PNG ou JPEG são aceitas")
    target_directory = Path(directory)
    target_directory.mkdir(parents=True, exist_ok=True)
    filename = f"{uuid.uuid4().hex}{extension}"
    (target_directory / filename).write_bytes(image)
    return filename


def screenshot_path(directory: str, filename: str) -> Path | None:
    if not filename or Path(filename).name != filename:
        return None
    target = Path(directory) / filename
    return target if target.is_file() else None
