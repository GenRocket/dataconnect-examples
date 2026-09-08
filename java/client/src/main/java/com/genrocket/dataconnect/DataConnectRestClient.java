package com.genrocket.dataconnect;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class DataConnectRestClient implements AutoCloseable {
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final long TOKEN_LIFETIME_NANOS = Duration.ofMinutes(55).toNanos();

  private final DataConnectConfig config;
  private final HttpClient http;
  private final Gson gson = new Gson();
  private String token;
  private long tokenValidUntil;

  public DataConnectRestClient(DataConnectConfig config) {
    this(config, HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build());
  }

  DataConnectRestClient(DataConnectConfig config, HttpClient http) {
    this.config = config;
    this.http = http;
  }

  public DataConnectConfig config() {
    return config;
  }

  public synchronized String login() {
    return login(false);
  }

  public synchronized String login(boolean force) {
    if (!force && token != null && System.nanoTime() < tokenValidUntil) {
      return token;
    }
    HttpRequest request =
        jsonRequest("/daas/login", gson.toJson(config.loginPayload()))
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(config.loginPayload())))
            .build();
    HttpResponse<byte[]> response = send(request, "login");
    JsonElement value = parseJson(response, "login");
    throwIfServiceError(value, response.statusCode(), "login");
    if (!value.isJsonObject()
        || !value.getAsJsonObject().has("token")
        || value.getAsJsonObject().get("token").getAsString().isBlank()) {
      throw new DataConnectException(
          "Login response did not contain a token", "REST", "login", null, response.statusCode());
    }
    token = value.getAsJsonObject().get("token").getAsString();
    tokenValidUntil = System.nanoTime() + TOKEN_LIFETIME_NANOS;
    return token;
  }

  public synchronized void invalidateToken() {
    token = null;
    tokenValidUntil = 0;
  }

  public JsonElement industries() {
    return anonymousGet("/daas/industries", "industries");
  }

  public JsonElement generators() {
    return anonymousGet("/daas/generators", "generators");
  }

  public GenerateResult generate(GenerateRequest request) {
    HttpResponse<byte[]> response =
        authenticatedPost("/daas/generate", gson.toJson(request.toMap(true)), "generate");
    String contentType =
        response
            .headers()
            .firstValue("content-type")
            .map(value -> value.split(";", 2)[0].toLowerCase())
            .orElse(null);
    if (!request.receivers().isEmpty()) {
      return GenerateResult.file(response.body(), contentType);
    }
    JsonElement value = parseJson(response, "generate");
    throwIfServiceError(value, response.statusCode(), "generate");
    return GenerateResult.json(value, contentType == null ? "application/json" : contentType);
  }

  private JsonElement anonymousGet(String path, String operation) {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(config.restBaseUrl() + path))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
    HttpResponse<byte[]> response = send(request, operation);
    JsonElement value = parseJson(response, operation);
    throwIfServiceError(value, response.statusCode(), operation);
    return value;
  }

  private HttpResponse<byte[]> authenticatedPost(String path, String body, String operation) {
    for (int attempt = 0; attempt < 2; attempt++) {
      String jwt = login(attempt == 1);
      HttpRequest request =
          jsonRequest(path, body)
              .header("X-Daas-Token", jwt)
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<byte[]> response = send(request, operation);
      DataConnectException error = serviceError(response, operation);
      if (error != null && error.isAuthenticationError() && attempt == 0) {
        invalidateToken();
        continue;
      }
      if (error != null) {
        throw error;
      }
      return response;
    }
    throw new IllegalStateException("Authentication retry loop did not return");
  }

  private HttpRequest.Builder jsonRequest(String path, String body) {
    return HttpRequest.newBuilder(URI.create(config.restBaseUrl() + path))
        .timeout(REQUEST_TIMEOUT)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json");
  }

  private HttpResponse<byte[]> send(HttpRequest request, String operation) {
    try {
      return http.send(request, HttpResponse.BodyHandlers.ofByteArray());
    } catch (IOException exception) {
      throw new DataConnectException(
          "Unable to reach DataConnect", "REST", operation, null, null, exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new DataConnectException(
          "DataConnect request was interrupted", "REST", operation, null, null, exception);
    }
  }

  private DataConnectException serviceError(HttpResponse<byte[]> response, String operation) {
    JsonElement value = null;
    try {
      value = JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8));
    } catch (RuntimeException ignored) {
      // A successful receiver response is binary, and may not be JSON.
    }
    if (response.statusCode() < 400 && !isFailureEnvelope(value)) {
      return null;
    }
    String message = "DataConnect rejected the request";
    String code = null;
    if (value != null && value.isJsonObject()) {
      JsonObject object = value.getAsJsonObject();
      if (object.has("message")) {
        message = object.get("message").getAsString();
      }
      if (object.has("errorCode")) {
        code = object.get("errorCode").getAsString();
      }
    }
    return new DataConnectException(message, "REST", operation, code, response.statusCode());
  }

  private static boolean isFailureEnvelope(JsonElement value) {
    return value != null
        && value.isJsonObject()
        && value.getAsJsonObject().has("success")
        && !value.getAsJsonObject().get("success").getAsBoolean();
  }

  private static JsonElement parseJson(HttpResponse<byte[]> response, String operation) {
    try {
      return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8));
    } catch (RuntimeException exception) {
      throw new DataConnectException(
          "DataConnect returned invalid JSON",
          "REST",
          operation,
          null,
          response.statusCode(),
          exception);
    }
  }

  private static void throwIfServiceError(JsonElement value, int statusCode, String operation) {
    if (isFailureEnvelope(value)) {
      JsonObject object = value.getAsJsonObject();
      throw new DataConnectException(
          object.has("message") ? object.get("message").getAsString() : "Request rejected",
          "REST",
          operation,
          object.has("errorCode") ? object.get("errorCode").getAsString() : null,
          statusCode);
    }
    if (statusCode >= 400) {
      throw new DataConnectException(
          "DataConnect rejected the request", "REST", operation, null, statusCode);
    }
  }

  @Override
  public void close() {
    invalidateToken();
  }
}
