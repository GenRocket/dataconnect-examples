# Compatibility

| Component | Initial baseline |
|---|---|
| DataConnect REST | Production endpoint supplied by user |
| DataConnect MCP | Streamable HTTP endpoint supplied by user |
| Python | 3.11+, MCP SDK 2.2.0 |
| Java | 17+, MCP SDK 2.0.1 |
| .NET | .NET 10, MCP SDK 2.2.0 |

## Live verification

On 2026-09-08, the production endpoints were verified locally with Python
3.12, Java 21 compiling to the Java 17 baseline, and .NET 10. The following
checks passed in all three implementations:

- REST login, discovery, inline generation, fixture generation, and JSON, CSV,
  and XML file receivers
- MCP industry and generator resources plus explicit, inferred, enum, regex,
  mixed, fixture, and agent-oriented generation
- Local stdio bridge discovery, live resource reads, live generation, and clean
  shutdown

Each generation request used a single row. No credentials, JWTs, generated
records, or endpoint values are retained in the repository.

If a current SDK cannot negotiate with the production MCP server, use the
newest maintained compatible official SDK line and document the exception.
Do not introduce a handwritten JSON-RPC fallback.
