# MCP host setup

The DataConnect MCP service needs a JWT obtained from the REST login endpoint.
The local stdio bridges in this repository perform that login, keep the JWT in
memory, refresh it when needed, and proxy only the documented DataConnect tool
and resources.

Choose one bridge implementation:

- [Python bridge](#python-bridge) — shortest setup
- [Java bridge](#java-bridge) — self-contained shaded JAR
- [.NET bridge](#net-bridge) — published framework-dependent application

Then add it to [Cursor](cursor.md) or [VS Code](vscode.md).

All host configuration must use absolute paths. Do not copy credentials into a
host configuration; point the bridge at the repository-root `.env` file.

## Python bridge

```bash
cd /absolute/path/to/dataconnect-examples/python
uv sync --locked
uv run --locked dataconnect-mcp-bridge \
  --env-file /absolute/path/to/dataconnect-examples/.env
```

For host configuration, use the absolute path returned by `command -v uv` as
`command` and these arguments:

```json
[
  "--directory", "/absolute/path/to/dataconnect-examples/python",
  "run", "--locked", "dataconnect-mcp-bridge",
  "--env-file", "/absolute/path/to/dataconnect-examples/.env"
]
```

## Java bridge

```bash
cd /absolute/path/to/dataconnect-examples/java
./mvnw -q -pl bridge -am package
java -jar bridge/target/dataconnect-mcp-bridge.jar \
  --env-file /absolute/path/to/dataconnect-examples/.env
```

Use the absolute path returned by `command -v java` as `command`. The host
arguments start with `-jar`, followed by the absolute JAR and env-file paths.

## .NET bridge

```bash
cd /absolute/path/to/dataconnect-examples/dotnet
dotnet restore DataConnectExamples.sln --locked-mode
dotnet publish src/GenRocket.DataConnect.Bridge -c Release \
  --no-restore -o publish/bridge
dotnet publish/bridge/GenRocket.DataConnect.Bridge.dll \
  --env-file /absolute/path/to/dataconnect-examples/.env
```

Use the absolute path returned by `command -v dotnet` as `command`. The first
host argument is the absolute published DLL path.

## Security and troubleshooting

- Review and trust local MCP server configuration before enabling it.
- Keep `.env` outside host logs and source control.
- Never print to stdout from a stdio bridge; stdout carries MCP messages.
- If initialization fails, run the REST discovery example, verify both HTTPS
  URLs, and confirm the identity has DataConnect access.
- Generation calls consume quota. Resource discovery does not generate rows.
- After editing `.env`, restart the MCP server from the host.
