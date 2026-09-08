# Architecture

The repository has three deliberately separate layers:

```text
example or test
      |
      v
language-local typed client
      |
      +---- REST login/discovery/generation
      |
      +---- official MCP SDK ---- remote Streamable HTTP MCP

MCP host ---- local stdio bridge ---- login + remote MCP client
```

The local clients remove repetitive configuration, login, redaction, and error
handling while leaving each example's request construction visible. They are
not published packages and do not claim to be complete SDKs.

The stdio bridges exist because a static bearer token expires after 60 minutes.
They expose only `generate_data`, `daas://industries`, and
`daas://generators`, and refresh the upstream token without persisting it.

Canonical JSON fixtures under `contracts` keep requests consistent across
languages. Live service compatibility remains a release gate because those
fixtures are examples, not an OpenAPI or MCP server specification.
