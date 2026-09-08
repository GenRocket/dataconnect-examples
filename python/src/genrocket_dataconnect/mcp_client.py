"""Remote DataConnect MCP client using the official Python MCP SDK."""

from __future__ import annotations

import json
from typing import Any

import httpx2
from mcp import Client
from mcp.client.streamable_http import streamable_http_client
from mcp.types import BlobResourceContents, TextContent, TextResourceContents

from .errors import DataConnectError
from .models import GenerateRequest
from .rest import DataConnectRestClient


class DataConnectMcpClient:
    def __init__(self, rest_client: DataConnectRestClient) -> None:
        self._rest = rest_client

    async def generate(self, request: GenerateRequest) -> Any:
        arguments = request.to_dict(include_rest_only=False)
        for attempt in range(2):
            token = await self._rest.login(force=attempt == 1)
            try:
                async with httpx2.AsyncClient(
                    headers={"Authorization": f"Bearer {token}"},
                    timeout=30.0,
                ) as http_client:
                    transport = streamable_http_client(
                        self._rest.config.mcp_url, http_client=http_client
                    )
                    async with Client(transport) as client:
                        result = await client.call_tool("generate_data", arguments)
                if result.is_error:
                    message = self._tool_text(result.content) or "MCP tool returned an error"
                    raise DataConnectError(message, "MCP", "generate_data")
                if result.structured_content is not None:
                    return result.structured_content
                return self._parse_text(self._tool_text(result.content))
            except DataConnectError:
                raise
            except Exception as error:
                if attempt == 0 and self._looks_like_auth_failure(error):
                    self._rest.invalidate_token()
                    continue
                raise DataConnectError(
                    "Unable to call the DataConnect MCP generate_data tool",
                    "MCP",
                    "generate_data",
                ) from error
        raise AssertionError("authentication retry loop did not return")

    async def read_resource(self, uri: str) -> Any:
        for attempt in range(2):
            token = await self._rest.login(force=attempt == 1)
            try:
                async with httpx2.AsyncClient(
                    headers={"Authorization": f"Bearer {token}"},
                    timeout=30.0,
                ) as http_client:
                    transport = streamable_http_client(
                        self._rest.config.mcp_url, http_client=http_client
                    )
                    async with Client(transport) as client:
                        result = await client.read_resource(uri)
                values: list[Any] = []
                for content in result.contents:
                    if isinstance(content, TextResourceContents):
                        values.append(self._parse_text(content.text))
                    elif isinstance(content, BlobResourceContents):
                        values.append(content.blob)
                return values[0] if len(values) == 1 else values
            except Exception as error:
                if attempt == 0 and self._looks_like_auth_failure(error):
                    self._rest.invalidate_token()
                    continue
                raise DataConnectError(
                    f"Unable to read MCP resource {uri}",
                    "MCP",
                    "read_resource",
                ) from error
        raise AssertionError("authentication retry loop did not return")

    @staticmethod
    def _tool_text(content: list[Any]) -> str:
        return "\n".join(item.text for item in content if isinstance(item, TextContent))

    @staticmethod
    def _parse_text(value: str) -> Any:
        try:
            return json.loads(value)
        except json.JSONDecodeError:
            return value

    @staticmethod
    def _looks_like_auth_failure(error: Exception) -> bool:
        message = str(error).lower()
        return any(
            marker in message
            for marker in (
                "401",
                "403",
                "unauthorized",
                "forbidden",
                "token_expired",
                "token_invalid",
            )
        )
