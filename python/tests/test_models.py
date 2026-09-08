import pytest

from genrocket_dataconnect.models import Attribute, GenerateRequest, Generator, Receiver
from genrocket_dataconnect.scenarios import mixed_request


def test_serializes_api_field_names_and_omits_empty_fields() -> None:
    request = GenerateRequest(
        domain="Customer",
        loop_count=3,
        attributes=(Attribute("id", generators=(Generator("UUIDGen"),)),),
    )

    assert request.to_dict() == {
        "domain": "Customer",
        "loopCount": 3,
        "attributes": [{"name": "id", "generator": [{"name": "UUIDGen"}]}],
    }


def test_mcp_payload_omits_rest_only_fields() -> None:
    value = mixed_request().to_dict(include_rest_only=False)

    assert "context" not in value
    assert "receivers" not in value


@pytest.mark.parametrize("count", (0, 10_001))
def test_validates_loop_count(count: int) -> None:
    with pytest.raises(ValueError, match="between 1 and 10000"):
        GenerateRequest("Customer", count, (Attribute("id"),))


def test_allows_only_one_receiver() -> None:
    with pytest.raises(ValueError, match="at most one receiver"):
        GenerateRequest(
            "Customer",
            3,
            (Attribute("id"),),
            receivers=(Receiver("JSONFileReceiver"), Receiver("XMLFileReceiver")),
        )
