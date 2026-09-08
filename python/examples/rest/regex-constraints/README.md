# REST regex constraints

Generates account codes that match `ACCT-[0-9]{6}`.

```bash
uv run dataconnect-example rest regex --dry-run
uv run dataconnect-example rest regex
```

Keep patterns bounded; test downstream validation with both expected and edge
values.
