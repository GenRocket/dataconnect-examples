package com.genrocket.dataconnect;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public final class DataConnectMcpClient {
  private final DataConnectRestClient rest;

  public DataConnectMcpClient(DataConnectRestClient rest) {
    this.rest = Objects.requireNonNull(rest);
  }

  public McpSchema.CallToolResult generate(GenerateRequest request) {
    return callTool(request.toMap(false));
  }

  public McpSchema.CallToolResult callTool(Map<String, Object> arguments) {
    for (int attempt = 0; attempt < 2; attempt++) {
      String token = rest.login(attempt == 1);
      try (McpSyncClient client = newClient(token)) {
        client.initialize();
        McpSchema.CallToolResult result =
            client.callTool(new McpSchema.CallToolRequest("generate_data", arguments));
        if (Boolean.TRUE.equals(result.isError())) {
          throw new DataConnectException(text(result), "MCP", "generate_data", null, null);
        }
        return result;
      } catch (DataConnectException exception) {
        throw exception;
      } catch (RuntimeException exception) {
        if (attempt == 0 && looksLikeAuthenticationFailure(exception)) {
          rest.invalidateToken();
          continue;
        }
        throw new DataConnectException(
            "Unable to call the DataConnect MCP generate_data tool",
            "MCP",
            "generate_data",
            null,
            null,
            exception);
      }
    }
    throw new IllegalStateException("Authentication retry loop did not return");
  }

  public McpSchema.ReadResourceResult readResource(String uri) {
    for (int attempt = 0; attempt < 2; attempt++) {
      String token = rest.login(attempt == 1);
      try (McpSyncClient client = newClient(token)) {
        client.initialize();
        return client.readResource(new McpSchema.ReadResourceRequest(uri));
      } catch (RuntimeException exception) {
        if (attempt == 0 && looksLikeAuthenticationFailure(exception)) {
          rest.invalidateToken();
          continue;
        }
        throw new DataConnectException(
            "Unable to read DataConnect MCP resource",
            "MCP",
            "read_resource",
            null,
            null,
            exception);
      }
    }
    throw new IllegalStateException("Authentication retry loop did not return");
  }

  private McpSyncClient newClient(String token) {
    URI endpoint = URI.create(rest.config().mcpUrl());
    String baseUrl = endpoint.getScheme() + "://" + endpoint.getRawAuthority();
    String path = endpoint.getRawPath();
    if (endpoint.getRawQuery() != null) {
      path += "?" + endpoint.getRawQuery();
    }
    var transport =
        HttpClientStreamableHttpTransport.builder(baseUrl)
            .endpoint(path)
            .connectTimeout(Duration.ofSeconds(30))
            .httpRequestCustomizer(
                (builder, method, uri, body, context) ->
                    builder.header("Authorization", "Bearer " + token))
            .build();
    return McpClient.sync(transport)
        .clientInfo(new McpSchema.Implementation("dataconnect-java-examples", "0.1.0"))
        .requestTimeout(Duration.ofSeconds(30))
        .build();
  }

  private static String text(McpSchema.CallToolResult result) {
    return result.content().stream()
        .filter(McpSchema.TextContent.class::isInstance)
        .map(McpSchema.TextContent.class::cast)
        .map(McpSchema.TextContent::text)
        .findFirst()
        .orElse("MCP tool returned an error");
  }

  private static boolean looksLikeAuthenticationFailure(RuntimeException exception) {
    String message = String.valueOf(exception.getMessage()).toLowerCase();
    return message.contains("401")
        || message.contains("403")
        || message.contains("unauthorized")
        || message.contains("forbidden")
        || message.contains("token_expired")
        || message.contains("token_invalid");
  }
}
