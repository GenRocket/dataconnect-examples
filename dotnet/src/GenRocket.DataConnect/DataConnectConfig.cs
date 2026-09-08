namespace GenRocket.DataConnect;

public sealed record DataConnectConfig(
    string OrgExternalId,
    string ClientExternalId,
    string UserExternalId,
    string OrgApiKey,
    Uri RestBaseUrl,
    Uri McpUrl)
{
    private static readonly string[] Names =
    [
        "DATACONNECT_ORG_EXTERNAL_ID",
        "DATACONNECT_CLIENT_EXTERNAL_ID",
        "DATACONNECT_USER_EXTERNAL_ID",
        "DATACONNECT_ORG_API_KEY",
        "DATACONNECT_REST_BASE_URL",
        "DATACONNECT_MCP_URL",
    ];

    public static DataConnectConfig FromEnvironment(string envFile)
    {
        var fileValues = ReadEnv(envFile);
        var values = Names.ToDictionary(
            name => name,
            name => (Environment.GetEnvironmentVariable(name)
                     ?? fileValues.GetValueOrDefault(name)
                     ?? string.Empty).Trim());
        var missing = values.Where(pair => pair.Value.Length == 0).Select(pair => pair.Key).ToArray();
        if (missing.Length > 0)
        {
            throw new InvalidOperationException(
                $"Missing required configuration: {string.Join(", ", missing)}");
        }

        var rest = RequireHttps(values["DATACONNECT_REST_BASE_URL"], "DATACONNECT_REST_BASE_URL");
        var mcp = RequireHttps(values["DATACONNECT_MCP_URL"], "DATACONNECT_MCP_URL");
        return new DataConnectConfig(
            values["DATACONNECT_ORG_EXTERNAL_ID"],
            values["DATACONNECT_CLIENT_EXTERNAL_ID"],
            values["DATACONNECT_USER_EXTERNAL_ID"],
            values["DATACONNECT_ORG_API_KEY"],
            rest,
            mcp);
    }

    internal Dictionary<string, string> LoginPayload() => new()
    {
        ["orgExternalId"] = OrgExternalId,
        ["clientExternalId"] = ClientExternalId,
        ["userExternalId"] = UserExternalId,
        ["orgApiKey"] = OrgApiKey,
    };

    private static Dictionary<string, string> ReadEnv(string path)
    {
        var values = new Dictionary<string, string>(StringComparer.Ordinal);
        if (!File.Exists(path))
        {
            return values;
        }

        var number = 0;
        foreach (var rawLine in File.ReadLines(path))
        {
            number++;
            var line = rawLine.Trim();
            if (line.Length == 0 || line.StartsWith('#'))
            {
                continue;
            }

            var separator = line.IndexOf('=');
            if (separator < 1)
            {
                throw new InvalidOperationException($"Invalid environment entry on line {number}");
            }

            values[line[..separator].Trim()] = line[(separator + 1)..].Trim().Trim('"', '\'');
        }

        return values;
    }

    private static Uri RequireHttps(string value, string name)
    {
        if (!Uri.TryCreate(value.TrimEnd('/'), UriKind.Absolute, out var uri)
            || uri.Scheme != Uri.UriSchemeHttps)
        {
            throw new InvalidOperationException($"{name} must be an absolute HTTPS URL");
        }

        return uri;
    }
}
