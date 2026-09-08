## Summary

Describe the user problem and the example or infrastructure change.

## Validation

- [ ] Python: `uv run ruff check . && uv run mypy && uv run pytest`
- [ ] Java: `cd java && ./mvnw -B verify`
- [ ] .NET: `dotnet test DataConnectExamples.sln`
- [ ] REST and MCP dry runs still default to three rows
- [ ] No live API call was made, or the protected live-smoke result is linked

## Safety and documentation

- [ ] No credential, JWT, `.env`, private guide content, or identifying data is included
- [ ] The example has a focused README with setup, run, and quota guidance
- [ ] REST/MCP and Python/Java/.NET parity is preserved or the exception is explained
- [ ] Dependency changes update their lock files
