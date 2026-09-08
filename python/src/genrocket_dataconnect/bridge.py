"""Local stdio MCP bridge with transparent DataConnect login."""

from __future__ import annotations

import argparse
import asyncio
import json
from collections.abc import Sequence
from pathlib import Path
from typing import Any

from mcp.server import MCPServer

from .config import DataConnectConfig
from .mcp_client import DataConnectMcpClient
from .models import Attribute, GenerateRequest, Generator
from .rest import DataConnectRestClient

server = MCPServer(
    "genrocket-dataconnect-bridge",
    instructions="Generate synthetic data through GenRocket DataConnect.",
)
_runtime: DataConnectMcpClient | None = None


def _attribute(value: dict[str, Any]) -> Attribute:
    generators = tuple(
        Generator(str(item["name"]))
        for item in value.get("generator", [])
        if isinstance(item, dict) and item.get("name")
    )
    return Attribute(
        name=str(value["name"]),
        data_type=value.get("dataType"),
        description=value.get("description"),
        size=value.get("size"),
        list_of_options=tuple(value.get("listOfOptions", [])),
        regex=value.get("regex"),
        generators=generators,
    )


def _client() -> DataConnectMcpClient:
    if _runtime is None:
        raise RuntimeError("Bridge runtime has not been configured")
    return _runtime


async def _run_server(rest: DataConnectRestClient) -> None:
    try:
        await server.run_stdio_async()
    finally:
        await rest.aclose()


@server.tool()
async def generate_data(
    domain: str,
    loopCount: int,
    attributes: list[dict[str, Any]],
    industry: str | None = None,
) -> Any:
    """Generate synthetic records using a DataConnect domain and attributes."""
    request = GenerateRequest(
        domain=domain,
        industry=industry,
        loop_count=loopCount,
        attributes=tuple(_attribute(attribute) for attribute in attributes),
    )
    return await _client().generate(request)


@server.resource("daas://industries", mime_type="application/json")
async def industries() -> str:
    """Return DataConnect industries available for generation."""
    return json.dumps(await _client().read_resource("daas://industries"))


@server.resource("daas://generators", mime_type="application/json")
async def generators() -> str:
    """Return DataConnect generators available for explicit selection."""
    return json.dumps(await _client().read_resource("daas://generators"))


def main(argv: Sequence[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description="DataConnect auto-login MCP bridge")
    parser.add_argument("--env-file", type=Path, default=Path("../.env"))
    arguments = parser.parse_args(argv)
    config = DataConnectConfig.from_env(arguments.env_file)
    rest = DataConnectRestClient(config)
    global _runtime
    _runtime = DataConnectMcpClient(rest)
    asyncio.run(_run_server(rest))


if __name__ == "__main__":
    main()
