# REST explicit generators

Selects `UUIDGen` and `EmailGen` explicitly for predictable generator choice.

```bash
uv run dataconnect-example rest explicit --dry-run
uv run dataconnect-example rest explicit
```

Confirm names against discovery; available generators can evolve.
