using System.Net;
using ModelContextProtocol.Client;
using ModelContextProtocol.Protocol;

namespace GenRocket.DataConnect;

public sealed class DataConnectMcpClient(DataConnectRestClient rest)
{
    public Task<CallToolResult> GenerateAsync(
        GenerateRequest request,
        CancellationToken cancellationToken = default)
    {
        request.Validate();
        return CallToolAsync(request.ToDictionary(includeRestOnly: false), cancellationToken);
    }

    public async Task<CallToolResult> CallToolAsync(
        IReadOnlyDictionary<string, object?> arguments,
        CancellationToken cancellationToken = default)
    {
        for (var attempt = 0; attempt < 2; attempt++)
        {
            var token = await rest.LoginAsync(attempt == 1, cancellationToken).ConfigureAwait(false);
            try
            {
                await using var client = await CreateClientAsync(token, cancellationToken).ConfigureAwait(false);
                var result = await client.CallToolAsync(
                    "generate_data",
                    arguments,
                    progress: null,
                    options: null,
                    cancellationToken).ConfigureAwait(false);
                if (result.IsError is true)
                {
                    throw new DataConnectException(
                        ToolText(result) ?? "MCP tool returned an error", "MCP", "generate_data");
                }

                return result;
            }
            catch (DataConnectException)
            {
                throw;
            }
            catch (Exception exception) when (attempt == 0 && LooksLikeAuthenticationFailure(exception))
            {
                rest.InvalidateToken();
            }
            catch (Exception exception)
            {
                throw new DataConnectException(
                    "Unable to call the DataConnect MCP generate_data tool",
                    "MCP",
                    "generate_data",
                    innerException: exception);
            }
        }

        throw new InvalidOperationException("Authentication retry loop did not return");
    }

    public async Task<ReadResourceResult> ReadResourceAsync(
        string uri,
        CancellationToken cancellationToken = default)
    {
        for (var attempt = 0; attempt < 2; attempt++)
        {
            var token = await rest.LoginAsync(attempt == 1, cancellationToken).ConfigureAwait(false);
            try
            {
                await using var client = await CreateClientAsync(token, cancellationToken).ConfigureAwait(false);
                return await client.ReadResourceAsync(
                    uri, options: null, cancellationToken).ConfigureAwait(false);
            }
            catch (Exception exception) when (attempt == 0 && LooksLikeAuthenticationFailure(exception))
            {
                rest.InvalidateToken();
            }
            catch (Exception exception)
            {
                throw new DataConnectException(
                    $"Unable to read MCP resource {uri}",
                    "MCP",
                    "read_resource",
                    innerException: exception);
            }
        }

        throw new InvalidOperationException("Authentication retry loop did not return");
    }

    private async Task<McpClient> CreateClientAsync(
        string token,
        CancellationToken cancellationToken)
    {
        var transport = new HttpClientTransport(new HttpClientTransportOptions
        {
            Endpoint = rest.Config.McpUrl,
            TransportMode = HttpTransportMode.StreamableHttp,
            Name = "GenRocket DataConnect",
            ConnectionTimeout = TimeSpan.FromSeconds(30),
            AdditionalHeaders = new Dictionary<string, string>
            {
                ["Authorization"] = $"Bearer {token}",
            },
        });
        return await McpClient.CreateAsync(
            transport, cancellationToken: cancellationToken).ConfigureAwait(false);
    }

    private static string? ToolText(CallToolResult result) =>
        result.Content.OfType<TextContentBlock>().Select(content => content.Text).FirstOrDefault();

    private static bool LooksLikeAuthenticationFailure(Exception exception)
    {
        if (exception is HttpRequestException
            {
                StatusCode: HttpStatusCode.Unauthorized or HttpStatusCode.Forbidden,
            })
        {
            return true;
        }

        var message = exception.ToString().ToLowerInvariant();
        return message.Contains("401", StringComparison.Ordinal)
               || message.Contains("403", StringComparison.Ordinal)
               || message.Contains("unauthorized", StringComparison.Ordinal)
               || message.Contains("forbidden", StringComparison.Ordinal)
               || message.Contains("token_expired", StringComparison.Ordinal)
               || message.Contains("token_invalid", StringComparison.Ordinal);
    }
}
