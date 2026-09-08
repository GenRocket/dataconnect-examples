# MCP AI-agent workflow

The stdio bridge lets an MCP host discover resources and call `generate_data`
without storing a short-lived JWT in host configuration.

```bash
uv run dataconnect-example mcp ai-agent --dry-run
uv run dataconnect-mcp-bridge --env-file ../.env
```

Connect the second command through a supported [host configuration](../../../../docs/mcp-hosts/README.md).
