from genrocket_dataconnect.mcp_client import DataConnectMcpClient


def test_parses_json_text_content() -> None:
    assert DataConnectMcpClient._parse_text('[{"id": "1"}]') == [{"id": "1"}]


def test_preserves_non_json_text_content() -> None:
    assert DataConnectMcpClient._parse_text("plain text") == "plain text"
