package com.genrocket.dataconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DataConnectRestClientTest {
  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void reusesTokenAcrossGenerationCalls() throws IOException {
    AtomicInteger logins = new AtomicInteger();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/daas/login",
        exchange -> {
          logins.incrementAndGet();
          respond(exchange, 200, "{\"success\":true,\"token\":\"jwt\"}");
        });
    server.createContext(
        "/daas/generate",
        exchange -> {
          assertEquals("jwt", exchange.getRequestHeaders().getFirst("X-Daas-Token"));
          respond(exchange, 200, "[{\"id\":\"1\"}]");
        });
    server.start();

    try (DataConnectRestClient client = new DataConnectRestClient(config())) {
      client.generate(Scenarios.explicit(1));
      client.generate(Scenarios.explicit(1));
    }

    assertEquals(1, logins.get());
  }

  @Test
  void detectsQuotaFailureInSuccessfulHttpResponse() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/daas/login", exchange -> respond(exchange, 200, "{\"success\":true,\"token\":\"jwt\"}"));
    server.createContext(
        "/daas/generate",
        exchange ->
            respond(
                exchange,
                200,
                "{\"success\":false,\"errorCode\":\"QUOTA_EXCEEDED\","
                    + "\"message\":\"No quota\"}"));
    server.start();

    try (DataConnectRestClient client = new DataConnectRestClient(config())) {
      DataConnectException exception =
          assertThrows(DataConnectException.class, () -> client.generate(Scenarios.explicit(1)));
      assertEquals("QUOTA_EXCEEDED", exception.errorCode());
    }
  }

  @Test
  void refreshesOnceOnExplicitAuthenticationFailure() throws IOException {
    AtomicInteger logins = new AtomicInteger();
    AtomicInteger generations = new AtomicInteger();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/daas/login",
        exchange -> {
          int count = logins.incrementAndGet();
          respond(exchange, 200, "{\"success\":true,\"token\":\"jwt-" + count + "\"}");
        });
    server.createContext(
        "/daas/generate",
        exchange -> {
          if (generations.incrementAndGet() == 1) {
            respond(
                exchange,
                401,
                "{\"success\":false,\"errorCode\":\"DAAS_TOKEN_EXPIRED\","
                    + "\"message\":\"expired\"}");
          } else {
            assertEquals("jwt-2", exchange.getRequestHeaders().getFirst("X-Daas-Token"));
            respond(exchange, 200, "[{\"id\":\"1\"}]");
          }
        });
    server.start();

    try (DataConnectRestClient client = new DataConnectRestClient(config())) {
      client.generate(Scenarios.explicit(1));
    }

    assertEquals(2, logins.get());
    assertEquals(2, generations.get());
  }

  private DataConnectConfig config() {
    return new DataConnectConfig(
        "org",
        "client",
        "user",
        "secret",
        "http://localhost:" + server.getAddress().getPort(),
        "https://mcp.example.test/mcp");
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] content = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, content.length);
    exchange.getResponseBody().write(content);
    exchange.close();
  }
}
