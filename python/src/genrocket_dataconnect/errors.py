"""Safe, typed errors for both transports."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(eq=False)
class DataConnectError(Exception):
    message: str
    protocol: str
    operation: str
    error_code: str | None = None
    status_code: int | None = None

    def __str__(self) -> str:
        details = [f"{self.protocol} {self.operation} failed"]
        if self.error_code:
            details.append(f"code={self.error_code}")
        if self.status_code is not None:
            details.append(f"status={self.status_code}")
        return f"{', '.join(details)}: {self.message}"

    @property
    def is_authentication_error(self) -> bool:
        return self.status_code in {401, 403} or self.error_code in {
            "DAAS_TOKEN_EXPIRED",
            "DAAS_TOKEN_INVALID",
            "TOKEN_EXPIRED",
            "TOKEN_INVALID",
        }
