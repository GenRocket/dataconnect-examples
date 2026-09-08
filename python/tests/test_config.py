from pathlib import Path

import pytest

from genrocket_dataconnect.config import DataConnectConfig


def test_loads_dotenv_and_process_environment_wins(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    env_file = tmp_path / ".env"
    env_file.write_text(
        "\n".join(
            (
                "DATACONNECT_ORG_EXTERNAL_ID=file-org",
                "DATACONNECT_CLIENT_EXTERNAL_ID=client",
                "DATACONNECT_USER_EXTERNAL_ID=user",
                "DATACONNECT_ORG_API_KEY=secret",
                "DATACONNECT_REST_BASE_URL=https://rest.example.test",
                "DATACONNECT_MCP_URL=https://mcp.example.test/mcp",
            )
        ),
        encoding="utf-8",
    )
    monkeypatch.setenv("DATACONNECT_ORG_EXTERNAL_ID", "process-org")

    config = DataConnectConfig.from_env(env_file)

    assert config.org_external_id == "process-org"
    assert config.rest_base_url == "https://rest.example.test"


def test_rejects_missing_values(tmp_path: Path) -> None:
    with pytest.raises(ValueError, match="DATACONNECT_ORG_EXTERNAL_ID"):
        DataConnectConfig.from_env(tmp_path / "missing")


def test_rejects_non_https_service_url(tmp_path: Path) -> None:
    env_file = tmp_path / ".env"
    env_file.write_text(
        "\n".join(
            (
                "DATACONNECT_ORG_EXTERNAL_ID=org",
                "DATACONNECT_CLIENT_EXTERNAL_ID=client",
                "DATACONNECT_USER_EXTERNAL_ID=user",
                "DATACONNECT_ORG_API_KEY=secret",
                "DATACONNECT_REST_BASE_URL=http://rest.example.test",
                "DATACONNECT_MCP_URL=https://mcp.example.test/mcp",
            )
        ),
        encoding="utf-8",
    )

    with pytest.raises(ValueError, match="absolute HTTPS"):
        DataConnectConfig.from_env(env_file)


def test_rejects_mcp_url_without_endpoint_path(tmp_path: Path) -> None:
    env_file = tmp_path / ".env"
    env_file.write_text(
        "\n".join(
            (
                "DATACONNECT_ORG_EXTERNAL_ID=org",
                "DATACONNECT_CLIENT_EXTERNAL_ID=client",
                "DATACONNECT_USER_EXTERNAL_ID=user",
                "DATACONNECT_ORG_API_KEY=secret",
                "DATACONNECT_REST_BASE_URL=https://rest.example.test",
                "DATACONNECT_MCP_URL=https://mcp.example.test",
            )
        ),
        encoding="utf-8",
    )

    with pytest.raises(ValueError, match="include the /mcp endpoint path"):
        DataConnectConfig.from_env(env_file)
