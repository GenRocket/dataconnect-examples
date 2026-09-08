"""Command-line runner for all Python examples."""

from __future__ import annotations

import argparse
import asyncio
import json
import sys
from collections.abc import Sequence
from pathlib import Path
from typing import Any

from .config import DataConnectConfig
from .errors import DataConnectError
from .mcp_client import DataConnectMcpClient
from .rest import DataConnectRestClient
from .scenarios import SCENARIOS, explicit_request, file_request

RECEIVERS = {
    "csv": ("DelimitedFileReceiver", "text/plain"),
    "json": ("JSONFileReceiver", "application/json"),
    "xml": ("XMLFileReceiver", "application/xml"),
}


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Run GenRocket DataConnect examples")
    parser.add_argument("protocol", choices=("rest", "mcp"))
    parser.add_argument(
        "scenario",
        choices=(
            "login",
            "discovery",
            "explicit",
            "inferred",
            "enum",
            "regex",
            "mixed",
            "file",
            "fixture",
            "ai-agent",
        ),
    )
    parser.add_argument("--env-file", type=Path, default=Path("../.env"))
    parser.add_argument("--loop-count", type=int, default=3)
    parser.add_argument("--receiver", choices=tuple(RECEIVERS), default="json")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--dry-run", action="store_true")
    return parser


async def run(arguments: argparse.Namespace) -> None:
    request = None
    if arguments.scenario in SCENARIOS:
        request = SCENARIOS[arguments.scenario](arguments.loop_count)
    elif arguments.scenario in {"fixture", "ai-agent"}:
        request = explicit_request(arguments.loop_count)
    elif arguments.scenario == "file":
        request = file_request(RECEIVERS[arguments.receiver][0], arguments.loop_count)

    if arguments.dry_run:
        operation: dict[str, Any] = {
            "protocol": arguments.protocol,
            "scenario": arguments.scenario,
        }
        if request:
            operation["request"] = request.to_dict(include_rest_only=arguments.protocol == "rest")
        print(json.dumps(operation, indent=2))
        return

    config = DataConnectConfig.from_env(arguments.env_file)
    async with DataConnectRestClient(config) as rest:
        if arguments.protocol == "rest":
            await _run_rest(arguments, rest, request)
        else:
            await _run_mcp(arguments, DataConnectMcpClient(rest), request)


async def _run_rest(
    arguments: argparse.Namespace,
    client: DataConnectRestClient,
    request: Any,
) -> None:
    if arguments.scenario == "login":
        await client.login()
        print(json.dumps({"success": True, "message": "JWT acquired and cached in memory"}))
        return
    if arguments.scenario == "discovery":
        print(
            json.dumps(
                {
                    "industries": await client.industries(),
                    "generators": await client.generators(),
                },
                indent=2,
            )
        )
        return
    if request is None:
        raise ValueError(f"The {arguments.scenario} scenario is not available over REST")
    result = await client.generate(request)
    if result.content is not None:
        output = arguments.output or Path(f"output/dataconnect.{arguments.receiver}")
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_bytes(result.content)
        print(json.dumps({"output": str(output), "contentType": result.content_type}))
        return
    if arguments.scenario == "fixture" and arguments.output:
        arguments.output.parent.mkdir(parents=True, exist_ok=True)
        arguments.output.write_text(
            json.dumps(result.json_value, indent=2) + "\n",
            encoding="utf-8",
        )
        print(json.dumps({"output": str(arguments.output)}))
        return
    print(json.dumps(result.json_value, indent=2))


async def _run_mcp(
    arguments: argparse.Namespace,
    client: DataConnectMcpClient,
    request: Any,
) -> None:
    if arguments.scenario == "discovery":
        print(
            json.dumps(
                {
                    "industries": await client.read_resource("daas://industries"),
                    "generators": await client.read_resource("daas://generators"),
                },
                indent=2,
            )
        )
        return
    if arguments.scenario in {"login", "file"}:
        raise ValueError(f"The {arguments.scenario} scenario is REST-only")
    if request is None:
        raise ValueError(f"Unknown MCP scenario: {arguments.scenario}")
    print(json.dumps(await client.generate(request), indent=2))


def main(argv: Sequence[str] | None = None) -> None:
    try:
        asyncio.run(run(build_parser().parse_args(argv)))
    except (DataConnectError, ValueError) as error:
        print(str(error), file=sys.stderr)
        raise SystemExit(1) from error


if __name__ == "__main__":
    main()
