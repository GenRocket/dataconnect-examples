# ADR 0003: Auto-login stdio bridge

**Status:** Accepted

DataConnect MCP uses a JWT obtained from REST login, and that token expires.
Each language therefore provides a local stdio server that obtains and refreshes
the token in memory and forwards the known DataConnect tool and resources to
the remote MCP endpoint. Host configuration never contains a bearer token.
