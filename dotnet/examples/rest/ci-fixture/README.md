# REST CI fixture

Use a dedicated server user and an explicitly enabled protected CI job because
generation consumes quota.

```bash
dotnet run --project src/GenRocket.DataConnect.Examples -- \
  rest fixture --loop-count 3 --output output/ci-fixture.json
```
