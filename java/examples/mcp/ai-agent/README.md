# MCP AI-agent workflow

Run the automatic-login stdio bridge, then connect it using a supported
[host configuration](../../../../docs/mcp-hosts/README.md).

```bash
./mvnw -q -pl bridge -am package
java -jar bridge/target/dataconnect-mcp-bridge.jar --env-file ../.env
```
