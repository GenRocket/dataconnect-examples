using System.ComponentModel;
using GenRocket.DataConnect;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using ModelContextProtocol.Protocol;
using ModelContextProtocol.Server;

var envFile = ParseEnvFile(args);
var builder = Host.CreateApplicationBuilder();
builder.Logging.AddConsole(options => options.LogToStandardErrorThreshold = LogLevel.Trace);
builder.Services.AddSingleton(DataConnectConfig.FromEnvironment(envFile));
builder.Services.AddSingleton<DataConnectRestClient>();
builder.Services.AddSingleton<DataConnectMcpClient>();
builder.Services
    .AddMcpServer()
    .WithStdioServerTransport()
    .WithTools<DataConnectTools>()
    .WithResources<DataConnectResources>();
await builder.Build().RunAsync().ConfigureAwait(false);

static string ParseEnvFile(string[] values)
{
    if (values.Length == 0)
    {
        return "../.env";
    }

    if (values.Length == 2 && values[0] == "--env-file")
    {
        return values[1];
    }

    throw new ArgumentException("Usage: [--env-file PATH]");
}

internal sealed class DataConnectTools(DataConnectMcpClient upstream)
{
    [McpServerTool(Name = "generate_data", Destructive = false, Idempotent = false, OpenWorld = true)]
    [Description("Generate synthetic records using a DataConnect domain and attributes.")]
    public Task<CallToolResult> GenerateDataAsync(
        [Description("Business domain, such as Customer or Order.")] string domain,
        [Description("Number of rows from 1 through 10000.")] int loopCount,
        [Description("Non-empty list of generated attributes.")] IReadOnlyList<AttributeSpec> attributes,
        [Description("Optional industry context.")] string? industry = null,
        CancellationToken cancellationToken = default)
    {
        var request = new GenerateRequest(domain, loopCount, attributes, industry);
        return upstream.GenerateAsync(request, cancellationToken);
    }
}

internal sealed class DataConnectResources(DataConnectMcpClient upstream)
{
    [McpServerResource(UriTemplate = "daas://industries", Name = "industries", MimeType = "application/json")]
    [Description("DataConnect industries available for generation.")]
    public Task<ReadResourceResult> IndustriesAsync(CancellationToken cancellationToken) =>
        upstream.ReadResourceAsync("daas://industries", cancellationToken);

    [McpServerResource(UriTemplate = "daas://generators", Name = "generators", MimeType = "application/json")]
    [Description("DataConnect generators available for explicit selection.")]
    public Task<ReadResourceResult> GeneratorsAsync(CancellationToken cancellationToken) =>
        upstream.ReadResourceAsync("daas://generators", cancellationToken);
}
