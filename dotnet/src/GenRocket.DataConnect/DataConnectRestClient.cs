using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace GenRocket.DataConnect;

public sealed class DataConnectRestClient : IDisposable
{
    private static readonly TimeSpan TokenLifetime = TimeSpan.FromMinutes(55);
    private readonly DataConnectConfig config;
    private readonly HttpClient http;
    private readonly bool ownsHttpClient;
    private readonly SemaphoreSlim tokenLock = new(1, 1);
    private string? token;
    private DateTimeOffset tokenValidUntil;

    public DataConnectRestClient(DataConnectConfig config, HttpClient? httpClient = null)
    {
        this.config = config;
        http = httpClient ?? new HttpClient { Timeout = TimeSpan.FromSeconds(30) };
        ownsHttpClient = httpClient is null;
    }

    public DataConnectConfig Config => config;

    public async Task<string> LoginAsync(
        bool force = false,
        CancellationToken cancellationToken = default)
    {
        if (!force && token is not null && DateTimeOffset.UtcNow < tokenValidUntil)
        {
            return token;
        }

        await tokenLock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            if (!force && token is not null && DateTimeOffset.UtcNow < tokenValidUntil)
            {
                return token;
            }

            using var response = await http.PostAsJsonAsync(
                Endpoint("/daas/login"), config.LoginPayload(), cancellationToken).ConfigureAwait(false);
            var value = await ParseJsonAsync(response, "login", cancellationToken).ConfigureAwait(false);
            ThrowIfServiceError(value, response.StatusCode, "login");
            token = value?["token"]?.GetValue<string>();
            if (string.IsNullOrWhiteSpace(token))
            {
                throw new DataConnectException(
                    "Login response did not contain a token", "REST", "login", statusCode: (int)response.StatusCode);
            }

            tokenValidUntil = DateTimeOffset.UtcNow.Add(TokenLifetime);
            return token;
        }
        catch (HttpRequestException exception)
        {
            throw new DataConnectException(
                "Unable to reach the DataConnect login endpoint", "REST", "login", innerException: exception);
        }
        finally
        {
            tokenLock.Release();
        }
    }

    public void InvalidateToken()
    {
        token = null;
        tokenValidUntil = default;
    }

    public Task<JsonNode?> IndustriesAsync(CancellationToken cancellationToken = default) =>
        AnonymousGetAsync("/daas/industries", "industries", cancellationToken);

    public Task<JsonNode?> GeneratorsAsync(CancellationToken cancellationToken = default) =>
        AnonymousGetAsync("/daas/generators", "generators", cancellationToken);

    public async Task<GenerateResult> GenerateAsync(
        GenerateRequest request,
        CancellationToken cancellationToken = default)
    {
        request.Validate();
        using var response = await AuthenticatedPostAsync(
            "/daas/generate", request.ToJson(), "generate", cancellationToken).ConfigureAwait(false);
        var contentType = response.Content.Headers.ContentType?.MediaType;
        if (request.Receivers is { Count: > 0 })
        {
            return new GenerateResult(
                null,
                await response.Content.ReadAsByteArrayAsync(cancellationToken).ConfigureAwait(false),
                contentType);
        }

        var value = await ParseJsonAsync(response, "generate", cancellationToken).ConfigureAwait(false);
        ThrowIfServiceError(value, response.StatusCode, "generate");
        return new GenerateResult(value, null, contentType ?? "application/json");
    }

    private async Task<JsonNode?> AnonymousGetAsync(
        string path,
        string operation,
        CancellationToken cancellationToken)
    {
        try
        {
            using var response = await http.GetAsync(Endpoint(path), cancellationToken).ConfigureAwait(false);
            var value = await ParseJsonAsync(response, operation, cancellationToken).ConfigureAwait(false);
            ThrowIfServiceError(value, response.StatusCode, operation);
            return value;
        }
        catch (HttpRequestException exception)
        {
            throw new DataConnectException(
                "Unable to reach DataConnect", "REST", operation, innerException: exception);
        }
    }

    private async Task<HttpResponseMessage> AuthenticatedPostAsync(
        string path,
        JsonNode payload,
        string operation,
        CancellationToken cancellationToken)
    {
        for (var attempt = 0; attempt < 2; attempt++)
        {
            var jwt = await LoginAsync(attempt == 1, cancellationToken).ConfigureAwait(false);
            using var request = new HttpRequestMessage(HttpMethod.Post, Endpoint(path))
            {
                Content = JsonContent.Create(payload),
            };
            request.Headers.Add("X-Daas-Token", jwt);
            HttpResponseMessage response;
            try
            {
                response = await http.SendAsync(request, cancellationToken).ConfigureAwait(false);
            }
            catch (HttpRequestException exception)
            {
                throw new DataConnectException(
                    "Unable to reach DataConnect", "REST", operation, innerException: exception);
            }

            var error = await ServiceErrorAsync(response, operation, cancellationToken).ConfigureAwait(false);
            if (error is { IsAuthenticationError: true } && attempt == 0)
            {
                response.Dispose();
                InvalidateToken();
                continue;
            }

            if (error is not null)
            {
                response.Dispose();
                throw error;
            }

            return response;
        }

        throw new InvalidOperationException("Authentication retry loop did not return");
    }

    private static async Task<DataConnectException?> ServiceErrorAsync(
        HttpResponseMessage response,
        string operation,
        CancellationToken cancellationToken)
    {
        JsonNode? value = null;
        try
        {
            value = JsonNode.Parse(await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false));
        }
        catch (JsonException)
        {
            // Successful receiver output can be non-JSON.
        }

        var valueObject = value as JsonObject;
        if (response.IsSuccessStatusCode && valueObject?["success"]?.GetValue<bool>() is not false)
        {
            return null;
        }

        return new DataConnectException(
            valueObject?["message"]?.GetValue<string>() ?? "DataConnect rejected the request",
            "REST",
            operation,
            valueObject?["errorCode"]?.GetValue<string>(),
            (int)response.StatusCode);
    }

    private static async Task<JsonNode?> ParseJsonAsync(
        HttpResponseMessage response,
        string operation,
        CancellationToken cancellationToken)
    {
        try
        {
            return JsonNode.Parse(
                await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false));
        }
        catch (JsonException exception)
        {
            throw new DataConnectException(
                "DataConnect returned invalid JSON",
                "REST",
                operation,
                statusCode: (int)response.StatusCode,
                innerException: exception);
        }
    }

    private static void ThrowIfServiceError(JsonNode? value, HttpStatusCode status, string operation)
    {
        if (value is JsonObject valueObject
            && valueObject["success"]?.GetValue<bool>() is false)
        {
            throw new DataConnectException(
                valueObject["message"]?.GetValue<string>() ?? "DataConnect rejected the request",
                "REST",
                operation,
                valueObject["errorCode"]?.GetValue<string>(),
                (int)status);
        }

        if ((int)status >= 400)
        {
            throw new DataConnectException(
                "DataConnect rejected the request", "REST", operation, statusCode: (int)status);
        }
    }

    private Uri Endpoint(string path) => new(config.RestBaseUrl, path);

    public void Dispose()
    {
        InvalidateToken();
        tokenLock.Dispose();
        if (ownsHttpClient)
        {
            http.Dispose();
        }
    }
}
