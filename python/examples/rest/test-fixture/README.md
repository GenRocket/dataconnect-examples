# REST test fixture

Generates a small JSON fixture for a local integration test. Prefer an ephemeral
test location and avoid committing data that resembles sensitive production
schemas.

```bash
uv run dataconnect-example rest fixture --output output/customer-fixture.json
```
