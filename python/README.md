# Python examples

Requirements: Python 3.11+ and [uv](https://docs.astral.sh/uv/).

```bash
cd python
uv sync --extra dev --locked
cp ../.env.example ../.env  # first run only
uv run dataconnect-example rest explicit --dry-run
```

Commands load `../.env` by default when run from this directory. Override it
with `--env-file`. URLs are required configuration and have no built-in value.

## Run the examples

```bash
# Anonymous REST discovery
uv run dataconnect-example rest discovery

# Login, then generate three inline records
uv run dataconnect-example rest explicit
uv run dataconnect-example rest inferred
uv run dataconnect-example rest enum
uv run dataconnect-example rest regex
uv run dataconnect-example rest mixed

# Stream generated files to disk
uv run dataconnect-example rest file --receiver json --output output/customers.json
uv run dataconnect-example rest file --receiver csv --output output/customers.csv
uv run dataconnect-example rest file --receiver xml --output output/customers.xml

# Authenticated MCP resources and tool
uv run dataconnect-example mcp discovery
uv run dataconnect-example mcp explicit
uv run dataconnect-example mcp inferred
uv run dataconnect-example mcp enum
uv run dataconnect-example mcp regex
uv run dataconnect-example mcp mixed
```

Every generation command defaults to three rows and accepts `--loop-count`.
Use `--dry-run` first; it does not read credentials, contact DataConnect, or
consume quota.

## Use the client in an application

```python
from pathlib import Path

from genrocket_dataconnect.config import DataConnectConfig
from genrocket_dataconnect.rest import DataConnectRestClient
from genrocket_dataconnect.scenarios import explicit_request

config = DataConnectConfig.from_env(Path("../.env"))
async with DataConnectRestClient(config) as client:
    result = await client.generate(explicit_request(loop_count=3))
    print(result.json_value)
```

## MCP host bridge

The bridge logs in through REST, keeps the JWT only in memory, connects to the
remote Streamable HTTP MCP endpoint, and exposes a local stdio MCP server:

```bash
uv run dataconnect-mcp-bridge --env-file ../.env
```

Do not run this command directly when testing JSON-RPC: stdout is reserved for
MCP framing. See [host-specific configuration](../docs/mcp-hosts/README.md).

## Tests

```bash
uv run ruff check .
uv run mypy
uv run pytest
```

Tests mock network boundaries and do not need credentials. Live checks are
manually dispatched in GitHub Actions and consume quota.
