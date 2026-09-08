package com.genrocket.dataconnect.examples;

import com.genrocket.dataconnect.DataConnectConfig;
import com.genrocket.dataconnect.DataConnectException;
import com.genrocket.dataconnect.DataConnectMcpClient;
import com.genrocket.dataconnect.DataConnectRestClient;
import com.genrocket.dataconnect.GenerateRequest;
import com.genrocket.dataconnect.GenerateResult;
import com.genrocket.dataconnect.Scenarios;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ExampleRunner {
  private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Map<String, String> RECEIVERS =
      Map.of("csv", "DelimitedFileReceiver", "json", "JSONFileReceiver", "xml", "XMLFileReceiver");

  private ExampleRunner() {}

  public static void main(String[] args) {
    try {
      Options options = Options.parse(args);
      run(options);
    } catch (DataConnectException | IllegalArgumentException | IOException exception) {
      System.err.println(exception.getMessage());
      System.exit(1);
    }
  }

  static void run(Options options) throws IOException {
    GenerateRequest request = request(options);
    if (options.dryRun()) {
      Map<String, Object> output = new LinkedHashMap<>();
      output.put("protocol", options.protocol());
      output.put("scenario", options.scenario());
      if (request != null) {
        output.put("request", request.toMap("rest".equals(options.protocol())));
      }
      System.out.println(JSON.toJson(output));
      return;
    }

    DataConnectConfig config = DataConnectConfig.fromEnvironment(options.envFile());
    try (DataConnectRestClient rest = new DataConnectRestClient(config)) {
      if ("rest".equals(options.protocol())) {
        runRest(options, rest, request);
      } else {
        runMcp(options, new DataConnectMcpClient(rest), request);
      }
    }
  }

  private static void runRest(
      Options options, DataConnectRestClient client, GenerateRequest request) throws IOException {
    switch (options.scenario()) {
      case "login" -> {
        client.login();
        System.out.println("{\"success\":true,\"message\":\"JWT acquired and cached in memory\"}");
      }
      case "discovery" ->
          System.out.println(
              JSON.toJson(
                  Map.of("industries", client.industries(), "generators", client.generators())));
      default -> {
        if (request == null) {
          throw new IllegalArgumentException("Scenario is not available over REST");
        }
        GenerateResult result = client.generate(request);
        if (result.content() != null) {
          Path output =
              options.output() == null
                  ? Path.of("output", "dataconnect." + options.receiver())
                  : options.output();
          if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
          }
          Files.write(output, result.content());
          System.out.println(
              JSON.toJson(
                  Map.of("output", output.toString(), "contentType", result.contentType())));
        } else if ("fixture".equals(options.scenario()) && options.output() != null) {
          if (options.output().getParent() != null) {
            Files.createDirectories(options.output().getParent());
          }
          Files.writeString(
              options.output(),
              JSON.toJson(result.jsonValue()) + System.lineSeparator(),
              StandardCharsets.UTF_8);
          System.out.println(JSON.toJson(Map.of("output", options.output().toString())));
        } else {
          System.out.println(JSON.toJson(result.jsonValue()));
        }
      }
    }
  }

  private static void runMcp(
      Options options, DataConnectMcpClient client, GenerateRequest request) {
    if ("discovery".equals(options.scenario())) {
      System.out.println(
          JSON.toJson(
              Map.of(
                  "industries", client.readResource("daas://industries").contents(),
                  "generators", client.readResource("daas://generators").contents())));
      return;
    }
    if (Arrays.asList("login", "file").contains(options.scenario())) {
      throw new IllegalArgumentException("Scenario is REST-only: " + options.scenario());
    }
    if (request == null) {
      throw new IllegalArgumentException("Unknown MCP scenario: " + options.scenario());
    }
    McpSchema.CallToolResult result = client.generate(request);
    Object output =
        result.structuredContent() != null ? result.structuredContent() : result.content();
    System.out.println(JSON.toJson(output));
  }

  private static GenerateRequest request(Options options) {
    return switch (options.scenario()) {
      case "explicit", "fixture", "ai-agent" -> Scenarios.explicit(options.loopCount());
      case "inferred" -> Scenarios.inferred(options.loopCount());
      case "enum" -> Scenarios.enumerated(options.loopCount());
      case "regex" -> Scenarios.regex(options.loopCount());
      case "mixed" -> Scenarios.mixed(options.loopCount());
      case "file" -> Scenarios.file(RECEIVERS.get(options.receiver()), options.loopCount());
      default -> null;
    };
  }

  record Options(
      String protocol,
      String scenario,
      Path envFile,
      int loopCount,
      String receiver,
      Path output,
      boolean dryRun) {

    static Options parse(String[] args) {
      if (args.length < 2) {
        throw new IllegalArgumentException(
            "Usage: <rest|mcp> <scenario> [--dry-run] [--loop-count N]"
                + " [--env-file PATH] [--receiver csv|json|xml] [--output PATH]");
      }
      String protocol = args[0];
      String scenario = args[1];
      if (!protocol.equals("rest") && !protocol.equals("mcp")) {
        throw new IllegalArgumentException("Protocol must be rest or mcp");
      }
      Path envFile = Path.of("../.env");
      int loopCount = 3;
      String receiver = "json";
      Path output = null;
      boolean dryRun = false;
      for (int index = 2; index < args.length; index++) {
        switch (args[index]) {
          case "--dry-run" -> dryRun = true;
          case "--env-file" -> envFile = Path.of(requireValue(args, ++index, "--env-file"));
          case "--loop-count" ->
              loopCount = Integer.parseInt(requireValue(args, ++index, "--loop-count"));
          case "--receiver" -> receiver = requireValue(args, ++index, "--receiver");
          case "--output" -> output = Path.of(requireValue(args, ++index, "--output"));
          default -> throw new IllegalArgumentException("Unknown option: " + args[index]);
        }
      }
      if (!RECEIVERS.containsKey(receiver)) {
        throw new IllegalArgumentException("Receiver must be csv, json, or xml");
      }
      return new Options(protocol, scenario, envFile, loopCount, receiver, output, dryRun);
    }

    private static String requireValue(String[] args, int index, String option) {
      if (index >= args.length) {
        throw new IllegalArgumentException(option + " requires a value");
      }
      return args[index];
    }
  }
}
