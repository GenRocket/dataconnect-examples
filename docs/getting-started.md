# Getting started

## 1. Obtain access

Ask your GenRocket administrator for an enabled organization, DataConnect
client, user or server-user identity, active quota, and the two production
service URLs.

## 2. Configure the environment

From the repository root:

```bash
cp .env.example .env
```

Fill all values. Keep `.env` local. The examples intentionally have no staging
or production URL fallback.

## 3. Choose a language

- [Python](../python/README.md)
- [Java](../java/README.md)
- [.NET](../dotnet/README.md)

Each language README contains native installation, dry-run, REST, MCP, and
bridge commands. Start with a dry run, then run discovery before generation.

## 4. Keep the first call small

Examples default to three records. Generated data consumes quota. Do not raise
the count until the request shape and output handling are correct.

## 5. Choose identity type

Both human DataConnect users and server users authenticate through
`DATACONNECT_USER_EXTERNAL_ID`. Prefer a server-user identity for unattended
CI or service integrations.
