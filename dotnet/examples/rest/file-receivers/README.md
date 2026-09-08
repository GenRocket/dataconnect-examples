# REST file receivers

Writes receiver output directly to CSV, JSON, or XML.

```bash
dotnet run --project src/GenRocket.DataConnect.Examples -- \
  rest file --receiver json --output output/customers.json
```

Exactly one REST receiver is allowed; MCP does not support receivers.
