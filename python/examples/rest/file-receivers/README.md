# REST file receivers

Streams generated output to a file using the one permitted receiver. MCP does
not expose receivers.

```bash
uv run dataconnect-example rest file --receiver csv --output output/customers.csv
uv run dataconnect-example rest file --receiver json --output output/customers.json
uv run dataconnect-example rest file --receiver xml --output output/customers.xml
```

The client treats receiver responses as bytes and never decodes them as the
normal inline JSON envelope.
