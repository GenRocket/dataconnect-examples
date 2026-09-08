# Compatibility

| Component | Initial baseline |
|---|---|
| DataConnect REST | Production endpoint supplied by user |
| DataConnect MCP | Streamable HTTP endpoint supplied by user |
| Python | 3.11+, MCP SDK 2.2.0 |
| Java | 17+, MCP SDK 2.0.1 |
| .NET | .NET 10, MCP SDK 2.2.0 |

The repository has not yet recorded a successful protected production smoke
run. Before a `v1.0.0` release, update this file with the endpoint environment,
date, SDK versions, and results—never with credentials or JWTs.

If a current SDK cannot negotiate with the production MCP server, use the
newest maintained compatible official SDK line and document the exception.
Do not introduce a handwritten JSON-RPC fallback.
