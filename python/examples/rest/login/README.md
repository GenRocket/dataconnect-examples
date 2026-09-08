# REST login and token reuse

Calls `/daas/login`, confirms success without printing the JWT, and holds the
token in memory for later calls.

```bash
uv run dataconnect-example rest login
```

See [`rest.py`](../../../src/genrocket_dataconnect/rest.py) for the synchronized
cache, refresh margin, and one-time authentication retry.
