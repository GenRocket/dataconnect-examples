# REST file receivers

Writes the binary receiver response directly to CSV, JSON, or XML output.

```bash
java -jar examples/target/dataconnect-examples.jar rest file --receiver json --output output/customers.json
```

Exactly one REST receiver is allowed; MCP does not support receivers.
