package com.genrocket.dataconnect;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataConnectConfigTest {
  @Test
  void rejectsMcpUrlWithoutEndpointPath(@TempDir Path directory) throws IOException {
    Path envFile = directory.resolve(".env");
    Files.writeString(
        envFile,
        String.join(
            "\n",
            "DATACONNECT_ORG_EXTERNAL_ID=org",
            "DATACONNECT_CLIENT_EXTERNAL_ID=client",
            "DATACONNECT_USER_EXTERNAL_ID=user",
            "DATACONNECT_ORG_API_KEY=secret",
            "DATACONNECT_REST_BASE_URL=https://rest.example.test",
            "DATACONNECT_MCP_URL=https://mcp.example.test"));

    assertThrows(IllegalArgumentException.class, () -> DataConnectConfig.fromEnvironment(envFile));
  }
}
