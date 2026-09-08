"""Environment-only configuration for the examples."""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import urlparse

ENV_NAMES = (
    "DATACONNECT_ORG_EXTERNAL_ID",
    "DATACONNECT_CLIENT_EXTERNAL_ID",
    "DATACONNECT_USER_EXTERNAL_ID",
    "DATACONNECT_ORG_API_KEY",
    "DATACONNECT_REST_BASE_URL",
    "DATACONNECT_MCP_URL",
)


def _read_env(path: Path) -> dict[str, str]:
    if not path.exists():
        return {}
    values: dict[str, str] = {}
    for number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise ValueError(f"Invalid environment entry on line {number} of {path}")
        name, value = line.split("=", 1)
        values[name.strip()] = value.strip().strip('"').strip("'")
    return values


@dataclass(frozen=True, slots=True)
class DataConnectConfig:
    org_external_id: str
    client_external_id: str
    user_external_id: str
    org_api_key: str
    rest_base_url: str
    mcp_url: str

    @classmethod
    def from_env(cls, env_file: Path | None = None) -> DataConnectConfig:
        """Load a dotenv file, then let process environment values override it."""
        file_values = _read_env(env_file) if env_file else {}
        values = {name: os.getenv(name, file_values.get(name, "")).strip() for name in ENV_NAMES}
        missing = [name for name, value in values.items() if not value]
        if missing:
            raise ValueError("Missing required configuration: " + ", ".join(missing))
        cls._require_https(values["DATACONNECT_REST_BASE_URL"], "DATACONNECT_REST_BASE_URL")
        cls._require_https(values["DATACONNECT_MCP_URL"], "DATACONNECT_MCP_URL")
        return cls(
            org_external_id=values["DATACONNECT_ORG_EXTERNAL_ID"],
            client_external_id=values["DATACONNECT_CLIENT_EXTERNAL_ID"],
            user_external_id=values["DATACONNECT_USER_EXTERNAL_ID"],
            org_api_key=values["DATACONNECT_ORG_API_KEY"],
            rest_base_url=values["DATACONNECT_REST_BASE_URL"].rstrip("/"),
            mcp_url=values["DATACONNECT_MCP_URL"],
        )

    @staticmethod
    def _require_https(value: str, name: str) -> None:
        parsed = urlparse(value)
        if parsed.scheme != "https" or not parsed.netloc:
            raise ValueError(f"{name} must be an absolute HTTPS URL")

    def login_payload(self) -> dict[str, str]:
        return {
            "orgExternalId": self.org_external_id,
            "clientExternalId": self.client_external_id,
            "userExternalId": self.user_external_id,
            "orgApiKey": self.org_api_key,
        }
