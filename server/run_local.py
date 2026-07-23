"""Start the Live On API with the active Python environment."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path


SERVER_DIRECTORY = Path(__file__).resolve().parent
ENV_FILE = SERVER_DIRECTORY / ".env"


if __name__ == "__main__":
    if not ENV_FILE.is_file():
        raise SystemExit(
            "Missing server/.env. Copy .env.example to .env and configure it first."
        )

    raise SystemExit(
        subprocess.call(
            [
                sys.executable,
                "-m",
                "uvicorn",
                "app.main:app",
                "--host",
                "127.0.0.1",
                "--port",
                "8080",
                "--env-file",
                str(ENV_FILE),
            ],
            cwd=SERVER_DIRECTORY,
        )
    )
