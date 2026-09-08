# REST CI fixture

Use a dedicated server user and repository secrets to create short-lived test
data in CI. Generation consumes quota, so this pattern belongs in an explicitly
enabled job—not every pull request.

```bash
uv run dataconnect-example rest fixture --loop-count 3 --output output/ci-fixture.json
```

See the manual `live-smoke.yml` workflow for protected-environment controls.
