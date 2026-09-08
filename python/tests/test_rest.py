from __future__ import annotations

import json

import httpx
import pytest

from genrocket_dataconnect.config import DataConnectConfig
from genrocket_dataconnect.errors import DataConnectError
from genrocket_dataconnect.rest import DataConnectRestClient
from genrocket_dataconnect.scenarios import explicit_request, file_request


def config() -> DataConnectConfig:
    return DataConnectConfig(
        "org",
        "client",
        "user",
        "secret",
        "https://rest.example.test",
        "https://mcp.example.test/mcp",
    )


@pytest.mark.asyncio
async def test_reuses_token() -> None:
    login_calls = 0

    def handler(request: httpx.Request) -> httpx.Response:
        nonlocal login_calls
        if request.url.path == "/daas/login":
            login_calls += 1
            return httpx.Response(200, json={"success": True, "token": "jwt"})
        assert request.headers["X-Daas-Token"] == "jwt"
        return httpx.Response(200, json=[{"id": "1"}])

    http = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    async with DataConnectRestClient(config(), http_client=http) as client:
        await client.generate(explicit_request())
        await client.generate(explicit_request())
    await http.aclose()

    assert login_calls == 1


@pytest.mark.asyncio
async def test_refreshes_once_after_explicit_authentication_failure() -> None:
    login_calls = 0
    generation_calls = 0

    def handler(request: httpx.Request) -> httpx.Response:
        nonlocal login_calls, generation_calls
        if request.url.path == "/daas/login":
            login_calls += 1
            return httpx.Response(200, json={"success": True, "token": f"jwt-{login_calls}"})
        generation_calls += 1
        if generation_calls == 1:
            return httpx.Response(
                401,
                json={"success": False, "errorCode": "DAAS_TOKEN_EXPIRED", "message": "expired"},
            )
        assert request.headers["X-Daas-Token"] == "jwt-2"
        return httpx.Response(200, json=[{"id": "1"}])

    http = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    async with DataConnectRestClient(config(), http_client=http) as client:
        result = await client.generate(explicit_request())
    await http.aclose()

    assert result.json_value == [{"id": "1"}]
    assert (login_calls, generation_calls) == (2, 2)


@pytest.mark.asyncio
async def test_detects_quota_error_in_http_200_response() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/daas/login":
            return httpx.Response(200, json={"success": True, "token": "jwt"})
        return httpx.Response(
            200,
            json={"success": False, "errorCode": "QUOTA_EXCEEDED", "message": "No quota"},
        )

    http = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    async with DataConnectRestClient(config(), http_client=http) as client:
        with pytest.raises(DataConnectError, match="QUOTA_EXCEEDED"):
            await client.generate(explicit_request())
    await http.aclose()


@pytest.mark.asyncio
async def test_returns_file_bytes_without_json_decoding() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/daas/login":
            return httpx.Response(200, json={"success": True, "token": "jwt"})
        payload = json.loads(request.content)
        assert payload["receivers"] == [
            {
                "receiverName": "DelimitedFileReceiver",
                "parameters": {"fileName": "dataconnect-output.csv"},
            }
        ]
        return httpx.Response(200, content=b"id\n1\n", headers={"content-type": "text/plain"})

    http = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    async with DataConnectRestClient(config(), http_client=http) as client:
        result = await client.generate(file_request("DelimitedFileReceiver"))
    await http.aclose()

    assert result.content == b"id\n1\n"
    assert result.content_type == "text/plain"
