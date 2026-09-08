"""Typed request models shared by the examples."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True, slots=True)
class Generator:
    name: str

    def to_dict(self) -> dict[str, str]:
        return {"name": self.name}


@dataclass(frozen=True, slots=True)
class Attribute:
    name: str
    data_type: str | None = None
    description: str | None = None
    size: int | None = None
    list_of_options: tuple[str, ...] = ()
    regex: str | None = None
    generators: tuple[Generator, ...] = ()

    def to_dict(self) -> dict[str, Any]:
        value: dict[str, Any] = {"name": self.name}
        optional: tuple[tuple[str, Any], ...] = (
            ("dataType", self.data_type),
            ("description", self.description),
            ("size", self.size),
            ("regex", self.regex),
        )
        value.update({key: item for key, item in optional if item is not None})
        if self.list_of_options:
            value["listOfOptions"] = list(self.list_of_options)
        if self.generators:
            value["generator"] = [generator.to_dict() for generator in self.generators]
        return value


@dataclass(frozen=True, slots=True)
class Receiver:
    name: str

    def to_dict(self) -> dict[str, str]:
        return {"name": self.name}


@dataclass(frozen=True, slots=True)
class GenerateRequest:
    domain: str
    loop_count: int
    attributes: tuple[Attribute, ...]
    industry: str | None = None
    context: str | None = None
    receivers: tuple[Receiver, ...] = ()

    def __post_init__(self) -> None:
        if not self.domain.strip():
            raise ValueError("domain must not be blank")
        if not 1 <= self.loop_count <= 10_000:
            raise ValueError("loop_count must be between 1 and 10000")
        if not self.attributes:
            raise ValueError("at least one attribute is required")
        if len(self.receivers) > 1:
            raise ValueError("DataConnect accepts at most one receiver")

    def to_dict(self, *, include_rest_only: bool = True) -> dict[str, Any]:
        value: dict[str, Any] = {
            "domain": self.domain,
            "loopCount": self.loop_count,
            "attributes": [attribute.to_dict() for attribute in self.attributes],
        }
        if self.industry:
            value["industry"] = self.industry
        if include_rest_only and self.context:
            value["context"] = self.context
        if include_rest_only and self.receivers:
            value["receivers"] = [receiver.to_dict() for receiver in self.receivers]
        return value


@dataclass(frozen=True, slots=True)
class GenerateResult:
    json_value: Any | None = None
    content: bytes | None = None
    content_type: str | None = None
