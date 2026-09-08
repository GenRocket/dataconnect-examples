using System.Text.Json;
using GenRocket.DataConnect;

try
{
    var options = Options.Parse(args);
    var request = BuildRequest(options);
    var jsonOptions = new JsonSerializerOptions { WriteIndented = true };

    if (options.DryRun)
    {
        Console.WriteLine(JsonSerializer.Serialize(new
        {
            protocol = options.Protocol,
            scenario = options.Scenario,
            request = request?.ToJson(options.Protocol == "rest"),
        }, jsonOptions));
        return;
    }

    var config = DataConnectConfig.FromEnvironment(options.EnvFile);
    using var rest = new DataConnectRestClient(config);
    if (options.Protocol == "rest")
    {
        await RunRestAsync(options, rest, request, jsonOptions).ConfigureAwait(false);
    }
    else
    {
        await RunMcpAsync(options, new DataConnectMcpClient(rest), request, jsonOptions)
            .ConfigureAwait(false);
    }
}
catch (Exception exception) when (exception is DataConnectException or ArgumentException or InvalidOperationException)
{
    Console.Error.WriteLine(exception.Message);
    Environment.ExitCode = 1;
}

static async Task RunRestAsync(
    Options options,
    DataConnectRestClient client,
    GenerateRequest? request,
    JsonSerializerOptions jsonOptions)
{
    if (options.Scenario == "login")
    {
        await client.LoginAsync().ConfigureAwait(false);
        Console.WriteLine("{\"success\":true,\"message\":\"JWT acquired and cached in memory\"}");
        return;
    }

    if (options.Scenario == "discovery")
    {
        Console.WriteLine(JsonSerializer.Serialize(new
        {
            industries = await client.IndustriesAsync().ConfigureAwait(false),
            generators = await client.GeneratorsAsync().ConfigureAwait(false),
        }, jsonOptions));
        return;
    }

    if (request is null)
    {
        throw new ArgumentException($"Scenario is not available over REST: {options.Scenario}");
    }

    var result = await client.GenerateAsync(request).ConfigureAwait(false);
    if (result.Content is not null)
    {
        var path = options.Output ?? Path.Combine("output", $"dataconnect.{options.Receiver}");
        Directory.CreateDirectory(Path.GetDirectoryName(path) ?? ".");
        await File.WriteAllBytesAsync(path, result.Content).ConfigureAwait(false);
        Console.WriteLine(JsonSerializer.Serialize(
            new { output = path, contentType = result.ContentType }, jsonOptions));
    }
    else if (options.Scenario == "fixture" && options.Output is not null)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(options.Output) ?? ".");
        await File.WriteAllTextAsync(
            options.Output,
            result.JsonValue?.ToJsonString(jsonOptions) + Environment.NewLine).ConfigureAwait(false);
        Console.WriteLine(JsonSerializer.Serialize(new { output = options.Output }, jsonOptions));
    }
    else
    {
        Console.WriteLine(result.JsonValue?.ToJsonString(jsonOptions));
    }
}

static async Task RunMcpAsync(
    Options options,
    DataConnectMcpClient client,
    GenerateRequest? request,
    JsonSerializerOptions jsonOptions)
{
    if (options.Scenario == "discovery")
    {
        Console.WriteLine(JsonSerializer.Serialize(new
        {
            industries = (await client.ReadResourceAsync("daas://industries").ConfigureAwait(false)).Contents,
            generators = (await client.ReadResourceAsync("daas://generators").ConfigureAwait(false)).Contents,
        }, jsonOptions));
        return;
    }

    if (options.Scenario is "login" or "file")
    {
        throw new ArgumentException($"Scenario is REST-only: {options.Scenario}");
    }

    if (request is null)
    {
        throw new ArgumentException($"Unknown MCP scenario: {options.Scenario}");
    }

    var result = await client.GenerateAsync(request).ConfigureAwait(false);
    Console.WriteLine(JsonSerializer.Serialize(
        result.StructuredContent ?? (object)result.Content, jsonOptions));
}

static GenerateRequest? BuildRequest(Options options) => options.Scenario switch
{
    "explicit" or "fixture" or "ai-agent" => Scenarios.Explicit(options.LoopCount),
    "inferred" => Scenarios.Inferred(options.LoopCount),
    "enum" => Scenarios.Enumerated(options.LoopCount),
    "regex" => Scenarios.Regex(options.LoopCount),
    "mixed" => Scenarios.Mixed(options.LoopCount),
    "file" => Scenarios.File(Options.Receivers[options.Receiver], options.LoopCount),
    _ => null,
};

internal sealed record Options(
    string Protocol,
    string Scenario,
    string EnvFile,
    int LoopCount,
    string Receiver,
    string? Output,
    bool DryRun)
{
    internal static readonly IReadOnlyDictionary<string, string> Receivers =
        new Dictionary<string, string>
        {
            ["csv"] = "DelimitedFileReceiver",
            ["json"] = "JSONFileReceiver",
            ["xml"] = "XMLFileReceiver",
        };

    internal static Options Parse(string[] values)
    {
        if (values.Length < 2)
        {
            throw new ArgumentException(
                "Usage: <rest|mcp> <scenario> [--dry-run] [--loop-count N] "
                + "[--env-file PATH] [--receiver csv|json|xml] [--output PATH]");
        }

        if (values[0] is not ("rest" or "mcp"))
        {
            throw new ArgumentException("Protocol must be rest or mcp");
        }

        var envFile = "../.env";
        var loopCount = 3;
        var receiver = "json";
        string? output = null;
        var dryRun = false;
        for (var index = 2; index < values.Length; index++)
        {
            switch (values[index])
            {
                case "--dry-run":
                    dryRun = true;
                    break;
                case "--env-file":
                    envFile = RequireValue(values, ref index, "--env-file");
                    break;
                case "--loop-count":
                    loopCount = int.Parse(
                        RequireValue(values, ref index, "--loop-count"),
                        System.Globalization.CultureInfo.InvariantCulture);
                    break;
                case "--receiver":
                    receiver = RequireValue(values, ref index, "--receiver");
                    break;
                case "--output":
                    output = RequireValue(values, ref index, "--output");
                    break;
                default:
                    throw new ArgumentException($"Unknown option: {values[index]}");
            }
        }

        if (!Receivers.ContainsKey(receiver))
        {
            throw new ArgumentException("Receiver must be csv, json, or xml");
        }

        return new Options(values[0], values[1], envFile, loopCount, receiver, output, dryRun);
    }

    private static string RequireValue(string[] values, ref int index, string option)
    {
        index++;
        if (index >= values.Length)
        {
            throw new ArgumentException($"{option} requires a value");
        }

        return values[index];
    }
}
