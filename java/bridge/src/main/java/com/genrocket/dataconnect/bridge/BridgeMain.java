package com.genrocket.dataconnect.bridge;

import com.genrocket.dataconnect.DataConnectConfig;
import com.genrocket.dataconnect.DataConnectMcpClient;
import com.genrocket.dataconnect.DataConnectRestClient;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import tools.jackson.databind.json.JsonMapper;

public final class BridgeMain {
  private BridgeMain() {}

  public static void main(String[] args) throws IOException, InterruptedException {
    Path envFile = parseEnvFile(args);
    DataConnectRestClient rest =
        new DataConnectRestClient(DataConnectConfig.fromEnvironment(envFile));
    DataConnectMcpClient upstream = new DataConnectMcpClient(rest);

    McpJsonMapper mapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());
    var transport = new StdioServerTransportProvider(mapper);

    var tool =
        McpSchema.Tool.builder("generate_data")
            .description("Generate synthetic records with GenRocket DataConnect")
            .inputSchema(inputSchema())
            .build();
    var toolSpec =
        McpServerFeatures.SyncToolSpecification.builder()
            .tool(tool)
            .callHandler((exchange, request) -> upstream.callTool(request.arguments()))
            .build();

    var industries = resource("daas://industries", "industries", upstream);
    var generators = resource("daas://generators", "generators", upstream);

    McpSyncServer server =
        McpServer.sync(transport)
            .serverInfo("genrocket-dataconnect-bridge", "0.1.0")
            .instructions("Generate synthetic data through GenRocket DataConnect.")
            .capabilities(
                McpSchema.ServerCapabilities.builder().tools(false).resources(false, false).build())
            .tools(toolSpec)
            .resources(industries, generators)
            .build();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  server.closeGracefully();
                  rest.close();
                }));
    new CountDownLatch(1).await();
  }

  private static McpServerFeatures.SyncResourceSpecification resource(
      String uri, String name, DataConnectMcpClient upstream) {
    var resource =
        McpSchema.Resource.builder(uri, name)
            .description("DataConnect " + name)
            .mimeType("application/json")
            .build();
    return new McpServerFeatures.SyncResourceSpecification(
        resource, (exchange, request) -> upstream.readResource(uri));
  }

  private static Map<String, Object> inputSchema() {
    Map<String, Object> attributeSchema =
        Map.of(
            "type",
            "object",
            "required",
            List.of("name"),
            "additionalProperties",
            true,
            "properties",
            Map.of("name", Map.of("type", "string")));
    return Map.of(
        "type",
        "object",
        "required",
        List.of("domain", "loopCount", "attributes"),
        "additionalProperties",
        false,
        "properties",
        Map.of(
            "domain", Map.of("type", "string"),
            "industry", Map.of("type", "string"),
            "loopCount", Map.of("type", "integer", "minimum", 1, "maximum", 10000),
            "attributes", Map.of("type", "array", "minItems", 1, "items", attributeSchema)));
  }

  private static Path parseEnvFile(String[] args) {
    if (args.length == 0) {
      return Path.of("../.env");
    }
    if (args.length == 2 && "--env-file".equals(args[0])) {
      return Path.of(args[1]);
    }
    throw new IllegalArgumentException("Usage: [--env-file PATH]");
  }
}
