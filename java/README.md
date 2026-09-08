# Java examples

Requirement: JDK 17+. The checked-in wrapper downloads the verified Maven
3.9.9 distribution on first use.

```bash
cd java
./mvnw -B verify
cp ../.env.example ../.env  # first run only
java -jar examples/target/dataconnect-examples.jar rest explicit --dry-run
```

## Run the examples

```bash
java -jar examples/target/dataconnect-examples.jar rest discovery
java -jar examples/target/dataconnect-examples.jar rest explicit
java -jar examples/target/dataconnect-examples.jar rest inferred
java -jar examples/target/dataconnect-examples.jar rest enum
java -jar examples/target/dataconnect-examples.jar rest regex
java -jar examples/target/dataconnect-examples.jar rest mixed
java -jar examples/target/dataconnect-examples.jar rest file --receiver json --output output/customers.json
java -jar examples/target/dataconnect-examples.jar mcp discovery
java -jar examples/target/dataconnect-examples.jar mcp explicit
java -jar examples/target/dataconnect-examples.jar mcp mixed
```

Commands default to three records. `--dry-run` does not load credentials or
consume quota. `--env-file`, `--loop-count`, `--receiver`, and `--output` match
the Python examples.

## Project layout

- `client`: typed REST client, official MCP SDK client, errors, and scenarios
- `examples`: CLI runner and focused example guides
- `bridge`: automatic-login stdio MCP server

## MCP host bridge

```bash
./mvnw -q -pl bridge -am package
java -jar bridge/target/dataconnect-mcp-bridge.jar --env-file ../.env
```

Stdout is reserved for MCP messages. Configure the process using the
[host-specific guides](../docs/mcp-hosts/README.md).

## Tests

```bash
./mvnw -B verify
```

Unit tests use a local HTTP server and require no DataConnect credentials.
