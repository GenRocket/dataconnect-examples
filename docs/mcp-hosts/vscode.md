# VS Code

Build or install one [bridge implementation](README.md). Run **MCP: Open User
Configuration** from the Command Palette, or create `.vscode/mcp.json` for a
workspace-only setup.

```json
{
  "servers": {
    "genrocket-dataconnect": {
      "type": "stdio",
      "command": "/absolute/path/to/uv",
      "args": [
        "--directory",
        "/absolute/path/to/dataconnect-examples/python",
        "run",
        "--locked",
        "dataconnect-mcp-bridge",
        "--env-file",
        "/absolute/path/to/dataconnect-examples/.env"
      ]
    }
  }
}
```

Start the server from the inline action in `mcp.json`, confirm trust, then use
**MCP: List Servers** to inspect logs and discovered capabilities. Do not commit
a workspace configuration containing machine-specific absolute paths.

See [VS Code's official MCP server guide](https://code.visualstudio.com/docs/agent-customization/mcp-servers).
