# MCP test fixture

Uses `generate_data` for a small in-memory fixture; mock the network during
normal unit tests.

```bash
dotnet run --project src/GenRocket.DataConnect.Examples -- mcp fixture --loop-count 3
```
