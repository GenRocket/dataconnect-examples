# MCP test fixture

Generate a small in-memory fixture inside a test setup step. Keep the call count
low and mock the network boundary in normal unit tests.

```bash
uv run dataconnect-example mcp fixture --loop-count 3
```
