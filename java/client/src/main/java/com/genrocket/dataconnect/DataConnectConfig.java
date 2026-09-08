package com.genrocket.dataconnect;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record DataConnectConfig(
    String orgExternalId,
    String clientExternalId,
    String userExternalId,
    String orgApiKey,
    String restBaseUrl,
    String mcpUrl) {

  private static final List<String> NAMES =
      List.of(
          "DATACONNECT_ORG_EXTERNAL_ID",
          "DATACONNECT_CLIENT_EXTERNAL_ID",
          "DATACONNECT_USER_EXTERNAL_ID",
          "DATACONNECT_ORG_API_KEY",
          "DATACONNECT_REST_BASE_URL",
          "DATACONNECT_MCP_URL");

  public static DataConnectConfig fromEnvironment(Path envFile) throws IOException {
    Map<String, String> fileValues = readEnv(envFile);
    Map<String, String> values = new HashMap<>();
    for (String name : NAMES) {
      String value = System.getenv().getOrDefault(name, fileValues.getOrDefault(name, "")).trim();
      if (value.isEmpty()) {
        throw new IllegalArgumentException("Missing required configuration: " + name);
      }
      values.put(name, value);
    }
    requireHttps(values.get("DATACONNECT_REST_BASE_URL"), "DATACONNECT_REST_BASE_URL");
    requireHttps(values.get("DATACONNECT_MCP_URL"), "DATACONNECT_MCP_URL");
    requireMcpPath(values.get("DATACONNECT_MCP_URL"));
    return new DataConnectConfig(
        values.get("DATACONNECT_ORG_EXTERNAL_ID"),
        values.get("DATACONNECT_CLIENT_EXTERNAL_ID"),
        values.get("DATACONNECT_USER_EXTERNAL_ID"),
        values.get("DATACONNECT_ORG_API_KEY"),
        stripTrailingSlash(values.get("DATACONNECT_REST_BASE_URL")),
        values.get("DATACONNECT_MCP_URL"));
  }

  public Map<String, String> loginPayload() {
    return Map.of(
        "orgExternalId", orgExternalId,
        "clientExternalId", clientExternalId,
        "userExternalId", userExternalId,
        "orgApiKey", orgApiKey);
  }

  private static Map<String, String> readEnv(Path envFile) throws IOException {
    Map<String, String> values = new HashMap<>();
    if (!Files.exists(envFile)) {
      return values;
    }
    int number = 0;
    for (String rawLine : Files.readAllLines(envFile)) {
      number++;
      String line = rawLine.trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      int separator = line.indexOf('=');
      if (separator < 1) {
        throw new IllegalArgumentException("Invalid environment entry on line " + number);
      }
      String value = line.substring(separator + 1).trim();
      if ((value.startsWith("\"") && value.endsWith("\""))
          || (value.startsWith("'") && value.endsWith("'"))) {
        value = value.substring(1, value.length() - 1);
      }
      values.put(line.substring(0, separator).trim(), value);
    }
    return values;
  }

  private static void requireHttps(String value, String name) {
    URI uri = URI.create(value);
    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
      throw new IllegalArgumentException(name + " must be an absolute HTTPS URL");
    }
  }

  private static void requireMcpPath(String value) {
    String path = URI.create(value).getPath().replaceFirst("/+$", "");
    if (!path.endsWith("/mcp")) {
      throw new IllegalArgumentException("DATACONNECT_MCP_URL must include the /mcp endpoint path");
    }
  }

  private static String stripTrailingSlash(String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}
