# Cursor

Build or install one [bridge implementation](README.md). Create a project-level
`.cursor/mcp.json`, or use `~/.cursor/mcp.json` for a user-level configuration.

```json
{
  "mcpServers": {
    "genrocket-dataconnect": {
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

Open Cursor's MCP settings, enable `genrocket-dataconnect`, and inspect the
discovered resources and `generate_data` tool. Keep tool approval enabled for
generation because every call consumes quota.

See [Cursor's official MCP documentation](https://docs.cursor.com/context/model-context-protocol).
