"""Readable request builders used by both the CLI and individual examples."""

from __future__ import annotations

from .models import Attribute, GenerateRequest, Generator, Receiver


def explicit_request(loop_count: int = 3) -> GenerateRequest:
    return GenerateRequest(
        domain="Customer",
        industry="Financial Services",
        loop_count=loop_count,
        attributes=(
            Attribute("customerId", "String", generators=(Generator("UUIDGen"),)),
            Attribute("email", "String", generators=(Generator("EmailGen"),)),
        ),
    )


def inferred_request(loop_count: int = 3) -> GenerateRequest:
    return GenerateRequest(
        domain="Customer",
        loop_count=loop_count,
        attributes=(
            Attribute("fullName", "String", "A realistic full name for an adult customer"),
            Attribute("email", "String", "An email address derived from a person's name"),
        ),
    )


def enum_request(loop_count: int = 3) -> GenerateRequest:
    return GenerateRequest(
        domain="Order",
        loop_count=loop_count,
        attributes=(
            Attribute(
                "status",
                "String",
                list_of_options=("PENDING", "PAID", "SHIPPED"),
            ),
        ),
    )


def regex_request(loop_count: int = 3) -> GenerateRequest:
    return GenerateRequest(
        domain="Account",
        loop_count=loop_count,
        attributes=(Attribute("accountCode", "String", regex=r"ACCT-[0-9]{6}"),),
    )


def mixed_request(loop_count: int = 3) -> GenerateRequest:
    return GenerateRequest(
        domain="Patient",
        industry="Healthcare",
        context="Generate internally consistent outpatient registration data",
        loop_count=loop_count,
        attributes=(
            Attribute("patientId", "String", generators=(Generator("UUIDGen"),)),
            Attribute("fullName", "String", "An adult patient's full name"),
            Attribute("status", "String", list_of_options=("ACTIVE", "INACTIVE")),
            Attribute("medicalRecordNumber", "String", regex=r"MRN-[0-9]{8}"),
        ),
    )


def file_request(receiver_name: str, loop_count: int = 3) -> GenerateRequest:
    base = explicit_request(loop_count)
    return GenerateRequest(
        domain=base.domain,
        industry=base.industry,
        loop_count=base.loop_count,
        attributes=base.attributes,
        receivers=(Receiver(receiver_name),),
    )


SCENARIOS = {
    "explicit": explicit_request,
    "inferred": inferred_request,
    "enum": enum_request,
    "regex": regex_request,
    "mixed": mixed_request,
}
