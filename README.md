# GenRocket DataConnect examples

[![CI](https://github.com/GenRocket/dataconnect-examples/actions/workflows/ci.yml/badge.svg)](https://github.com/GenRocket/dataconnect-examples/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

Runnable, production-minded examples for generating synthetic test data with
GenRocket DataConnect over REST and Model Context Protocol (MCP).

| Language | Runtime | REST | MCP | Auto-login bridge |
|---|---:|:---:|:---:|:---:|
| [Python](python/README.md) | 3.11+ | Yes | Yes | Yes |
| [Java](java/README.md) | 17+ | Yes | Yes | Yes |
| [.NET](dotnet/README.md) | 10 | Yes | Yes | Yes |

These are examples and a small local support library, not an officially
published GenRocket SDK. Copy the patterns that fit your application.

## Choose REST or MCP

- Use **REST** for services, automated tests, CI/CD pipelines, and file output.
- Use **MCP** for programmatic MCP clients and AI hosts. File receivers are not
  supported by the DataConnect MCP tool.
- Use a language's **stdio bridge** when an MCP host needs automatic login and
  token refresh. Host instructions cover Cursor and VS Code.

## Prerequisites

Your GenRocket organization must be enabled for DataConnect, and the selected
client and user (or server user) must have active quota. Obtain the four
integration identifiers from your GenRocket administrator.

```bash
cp .env.example .env
```

Fill all six values. No service URL is hardcoded. The REST value is the HTTPS
origin; the MCP value is the complete Streamable HTTP endpoint including
`/mcp`.

Never commit `.env`. The examples keep JWTs in memory, redact secrets from
errors, and default generation to three rows. Use `--dry-run` to inspect a
request without authenticating or consuming quota.

## Example catalog

| Capability | REST | MCP |
|---|:---:|:---:|
| Login and JWT reuse | Yes | Via REST login |
| Industries and generators | Anonymous endpoints | Authenticated resources |
| Explicit generators | Yes | Yes |
| LLM-inferred attributes | Yes | Yes |
| Enumerated values | Yes | Yes |
| Regex constraints | Yes | Yes |
| Mixed schema | With request context | Without REST-only context |
| CSV, JSON, and XML files | Yes | No |
| Native test fixture pattern | Yes | Yes |
| CI fixture pattern | Yes | No |
| AI-agent workflow | No | Yes |
| Typed error handling | Yes | Yes |

Canonical request and mock-response fixtures live in [`contracts`](contracts/README.md).
They support cross-language tests; they are not a replacement for the live API.

## Documentation

- [Getting started](docs/getting-started.md)
- [Authentication and errors](docs/authentication-and-errors.md)
- [Architecture](docs/architecture.md)
- [MCP host setup](docs/mcp-hosts/README.md)
- [Compatibility](docs/compatibility.md)
- [Request reference](docs/request-reference.md)
- [Repository settings](docs/repository-settings.md)
- [Contributing](CONTRIBUTING.md)
- [Support](SUPPORT.md)
- [Security](SECURITY.md)

Generated data consumes quota even when used only for testing. Start small and
do not automatically retry an uncertain generation request.

## License

Copyright GenRocket, Inc. Licensed under the [Apache License 2.0](LICENSE).
