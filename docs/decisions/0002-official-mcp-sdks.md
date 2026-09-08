# ADR 0002: Official MCP SDKs only

**Status:** Accepted

Python, Java, and .NET use their official Model Context Protocol SDKs for both
client and stdio-server behavior. The repository does not implement the MCP
wire protocol by hand. SDK versions are locked and updated by reviewed weekly
dependency pull requests.
