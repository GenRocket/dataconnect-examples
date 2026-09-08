# .NET examples

Requirements: .NET SDK 10.

```bash
cd dotnet
dotnet restore DataConnectExamples.sln
dotnet build DataConnectExamples.sln --no-restore
cp ../.env.example ../.env  # first run only
dotnet run --project src/GenRocket.DataConnect.Examples -- \
  rest explicit --dry-run
```

## Run the examples

```bash
dotnet run --project src/GenRocket.DataConnect.Examples -- rest discovery
dotnet run --project src/GenRocket.DataConnect.Examples -- rest explicit
dotnet run --project src/GenRocket.DataConnect.Examples -- rest inferred
dotnet run --project src/GenRocket.DataConnect.Examples -- rest enum
dotnet run --project src/GenRocket.DataConnect.Examples -- rest regex
dotnet run --project src/GenRocket.DataConnect.Examples -- rest mixed
dotnet run --project src/GenRocket.DataConnect.Examples -- \
  rest file --receiver json --output output/customers.json
dotnet run --project src/GenRocket.DataConnect.Examples -- mcp discovery
dotnet run --project src/GenRocket.DataConnect.Examples -- mcp explicit
dotnet run --project src/GenRocket.DataConnect.Examples -- mcp mixed
```

Commands default to three records. `--dry-run` does not load credentials or
consume quota. The REST and MCP clients live in `GenRocket.DataConnect` so the
same integration code can be copied into a service or test project.

## MCP host bridge

```bash
dotnet run --project src/GenRocket.DataConnect.Bridge -- --env-file ../.env
```

Stdout is reserved for MCP messages. Configure this command using a
[host-specific guide](../docs/mcp-hosts/README.md).

## Tests

```bash
dotnet test DataConnectExamples.sln
```

Unit tests mock HTTP and require no credentials. This repository enables NuGet
lock files; restore once after an intentional dependency update and commit the
resulting `packages.lock.json` files.
