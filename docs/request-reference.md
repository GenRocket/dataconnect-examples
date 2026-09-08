# Request reference

Use the [canonical JSON fixtures](../contracts/README.md) alongside this
summary. The language models validate the same required fields before sending
a request.

## Login

`POST /daas/login` accepts `orgExternalId`, `clientExternalId`,
`userExternalId`, and `orgApiKey`. The response JWT stays in memory only.

## Generation

`POST /daas/generate` and the MCP `generate_data` tool share these fields:

| Field | Required | Notes |
|---|:---:|---|
| `domain` | Yes | Non-empty business-domain name |
| `loopCount` | Yes | Integer from 1 through 10,000; examples default to 3 |
| `attributes` | Yes | Non-empty attribute array |
| `industry` | No | Use a value returned by discovery |

An attribute requires `name` and can add `dataType`, `description`, `size`,
`listOfOptions`, `regex`, or a `generator` array containing generator names.
Confirm generator names against discovery rather than treating example values
as a permanent catalog.

REST additionally accepts `context` and a `receivers` array containing exactly
one receiver. These REST-only receiver mappings are implemented:

| CLI value | Receiver | Response content type |
|---|---|---|
| `csv` | `DelimitedFileReceiver` | `text/plain` |
| `json` | `JSONFileReceiver` | `application/json` |
| `xml` | `XMLFileReceiver` | `application/xml` |

The MCP tool does not accept `context` or file receivers in this repository's
compatibility baseline.

## Discovery

REST exposes anonymous `GET /daas/industries` and `GET /daas/generators`
endpoints. MCP exposes authenticated `daas://industries` and
`daas://generators` resources.

## Retry boundary

The clients perform one retry only after an explicit expired/invalid-token
response. They do not retry timeouts, connection failures, quota errors, or
other generation failures because the service may already have generated data
and consumed quota.
