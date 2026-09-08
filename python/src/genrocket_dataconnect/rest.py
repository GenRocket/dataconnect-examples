"""Thin asynchronous REST client with in-memory token reuse."""

from __future__ import annotations

import asyncio
import time
from typing import Any

import httpx

from .config import DataConnectConfig
from .errors import DataConnectError
from .models import GenerateRequest, GenerateResult


class DataConnectRestClient:
    """Authenticate and call DataConnect without persisting credentials or JWTs."""

    def __init__(
        self,
        config: DataConnectConfig,
        *,
        http_client: httpx.AsyncClient | None = None,
    ) -> None:
        self.config = config
        self._http = http_client or httpx.AsyncClient(timeout=30.0)
        self._owns_http = http_client is None
        self._token: str | None = None
        self._token_valid_until = 0.0
        self._token_lock = asyncio.Lock()

    async def __aenter__(self) -> DataConnectRestClient:
        return self

    async def __aexit__(self, *_: object) -> None:
        await self.aclose()

    async def aclose(self) -> None:
        if self._owns_http:
            await self._http.aclose()

    async def login(self, *, force: bool = False) -> str:
        if not force and self._token and time.monotonic() < self._token_valid_until:
            return self._token
        async with self._token_lock:
            if not force and self._token and time.monotonic() < self._token_valid_until:
                return self._token
            try:
                response = await self._http.post(
                    f"{self.config.rest_base_url}/daas/login",
                    json=self.config.login_payload(),
                )
            except httpx.HTTPError as error:
                raise DataConnectError(
                    "Unable to reach the DataConnect login endpoint",
                    "REST",
                    "login",
                ) from error
            value = self._json_or_error(response, "login")
            self._raise_service_error(value, response.status_code, "login")
            token = value.get("token") if isinstance(value, dict) else None
            if not isinstance(token, str) or not token:
                raise DataConnectError("Login response did not contain a token", "REST", "login")
            self._token = token
            self._token_valid_until = time.monotonic() + 55 * 60
            return token

    def invalidate_token(self) -> None:
        self._token = None
        self._token_valid_until = 0.0

    async def industries(self) -> Any:
        return await self._anonymous_json("/daas/industries", "industries")

    async def generators(self) -> Any:
        return await self._anonymous_json("/daas/generators", "generators")

    async def generate(self, request: GenerateRequest) -> GenerateResult:
        response = await self._authenticated_post("/daas/generate", request.to_dict(), "generate")
        content_type = response.headers.get("content-type", "").split(";", 1)[0].lower()
        if request.receivers:
            return GenerateResult(content=response.content, content_type=content_type or None)
        value = self._json_or_error(response, "generate")
        self._raise_service_error(value, response.status_code, "generate")
        return GenerateResult(json_value=value, content_type=content_type or "application/json")

    async def _anonymous_json(self, path: str, operation: str) -> Any:
        try:
            response = await self._http.get(f"{self.config.rest_base_url}{path}")
        except httpx.HTTPError as error:
            raise DataConnectError(
                f"Unable to reach the DataConnect {operation} endpoint",
                "REST",
                operation,
            ) from error
        value = self._json_or_error(response, operation)
        self._raise_service_error(value, response.status_code, operation)
        return value

    async def _authenticated_post(
        self,
        path: str,
        payload: dict[str, Any],
        operation: str,
    ) -> httpx.Response:
        for attempt in range(2):
            token = await self.login(force=attempt == 1)
            try:
                response = await self._http.post(
                    f"{self.config.rest_base_url}{path}",
                    json=payload,
                    headers={"X-Daas-Token": token},
                )
            except httpx.HTTPError as http_error:
                raise DataConnectError(
                    f"Unable to reach the DataConnect {operation} endpoint",
                    "REST",
                    operation,
                ) from http_error
            service_error = self._service_error(response, operation)
            if service_error and service_error.is_authentication_error and attempt == 0:
                self.invalidate_token()
                continue
            if service_error:
                raise service_error
            return response
        raise AssertionError("authentication retry loop did not return")

    def _service_error(self, response: httpx.Response, operation: str) -> DataConnectError | None:
        value: Any = None
        try:
            value = response.json()
        except ValueError:
            pass
        if response.is_success and not (isinstance(value, dict) and value.get("success") is False):
            return None
        message = "DataConnect rejected the request"
        code = None
        if isinstance(value, dict):
            if isinstance(value.get("message"), str):
                message = value["message"]
            if isinstance(value.get("errorCode"), str):
                code = value["errorCode"]
        return DataConnectError(message, "REST", operation, code, response.status_code)

    def _json_or_error(self, response: httpx.Response, operation: str) -> Any:
        try:
            return response.json()
        except ValueError as error:
            if not response.is_success:
                raise DataConnectError(
                    "DataConnect returned a non-JSON error",
                    "REST",
                    operation,
                    status_code=response.status_code,
                ) from error
            raise DataConnectError(
                "DataConnect returned invalid JSON",
                "REST",
                operation,
                status_code=response.status_code,
            ) from error

    @staticmethod
    def _raise_service_error(value: Any, status_code: int, operation: str) -> None:
        if isinstance(value, dict) and value.get("success") is False:
            raise DataConnectError(
                str(value.get("message", "DataConnect rejected the request")),
                "REST",
                operation,
                str(value["errorCode"]) if value.get("errorCode") else None,
                status_code,
            )
        if status_code >= 400:
            raise DataConnectError(
                "DataConnect rejected the request",
                "REST",
                operation,
                status_code=status_code,
            )
