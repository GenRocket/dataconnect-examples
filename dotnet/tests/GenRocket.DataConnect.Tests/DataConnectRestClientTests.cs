using System.Net;
using System.Text;
using GenRocket.DataConnect;
using Xunit;

namespace GenRocket.DataConnect.Tests;

public sealed class DataConnectRestClientTests
{
    [Fact]
    public async Task ReusesTokenAcrossGenerationCalls()
    {
        var logins = 0;
        using var http = new HttpClient(new DelegateHandler(request =>
        {
            if (request.RequestUri?.AbsolutePath == "/daas/login")
            {
                logins++;
                return Json(HttpStatusCode.OK, "{\"success\":true,\"token\":\"jwt\"}");
            }

            Assert.Equal("jwt", request.Headers.GetValues("X-Daas-Token").Single());
            return Json(HttpStatusCode.OK, "[{\"id\":\"1\"}]");
        }));
        using var client = new DataConnectRestClient(Config(), http);

        await client.GenerateAsync(Scenarios.Explicit(1));
        await client.GenerateAsync(Scenarios.Explicit(1));

        Assert.Equal(1, logins);
    }

    [Fact]
    public async Task RefreshesOnceAfterExplicitAuthenticationFailure()
    {
        var logins = 0;
        var generations = 0;
        using var http = new HttpClient(new DelegateHandler(request =>
        {
            if (request.RequestUri?.AbsolutePath == "/daas/login")
            {
                logins++;
                return Json(HttpStatusCode.OK, $"{{\"success\":true,\"token\":\"jwt-{logins}\"}}");
            }

            generations++;
            if (generations == 1)
            {
                return Json(
                    HttpStatusCode.Unauthorized,
                    "{\"success\":false,\"errorCode\":\"DAAS_TOKEN_EXPIRED\","
                    + "\"message\":\"expired\"}");
            }

            Assert.Equal("jwt-2", request.Headers.GetValues("X-Daas-Token").Single());
            return Json(HttpStatusCode.OK, "[{\"id\":\"1\"}]");
        }));
        using var client = new DataConnectRestClient(Config(), http);

        await client.GenerateAsync(Scenarios.Explicit(1));

        Assert.Equal(2, logins);
        Assert.Equal(2, generations);
    }

    [Fact]
    public async Task DetectsQuotaErrorInsideHttp200Response()
    {
        using var http = new HttpClient(new DelegateHandler(request =>
            request.RequestUri?.AbsolutePath == "/daas/login"
                ? Json(HttpStatusCode.OK, "{\"success\":true,\"token\":\"jwt\"}")
                : Json(
                    HttpStatusCode.OK,
                    "{\"success\":false,\"errorCode\":\"QUOTA_EXCEEDED\","
                    + "\"message\":\"No quota\"}")));
        using var client = new DataConnectRestClient(Config(), http);

        var exception = await Assert.ThrowsAsync<DataConnectException>(
            () => client.GenerateAsync(Scenarios.Explicit(1)));

        Assert.Equal("QUOTA_EXCEEDED", exception.ErrorCode);
    }

    [Fact]
    public void McpPayloadOmitsRestOnlyFields()
    {
        var value = Scenarios.Mixed().ToJson(includeRestOnly: false);

        Assert.False(value.ContainsKey("context"));
        Assert.False(value.ContainsKey("receivers"));
    }

    [Fact]
    public void RejectsMcpUrlWithoutEndpointPath()
    {
        var path = Path.GetTempFileName();
        try
        {
            File.WriteAllLines(path,
            [
                "DATACONNECT_ORG_EXTERNAL_ID=org",
                "DATACONNECT_CLIENT_EXTERNAL_ID=client",
                "DATACONNECT_USER_EXTERNAL_ID=user",
                "DATACONNECT_ORG_API_KEY=secret",
                "DATACONNECT_REST_BASE_URL=https://rest.example.test",
                "DATACONNECT_MCP_URL=https://mcp.example.test",
            ]);

            var error = Assert.Throws<InvalidOperationException>(
                () => DataConnectConfig.FromEnvironment(path));

            Assert.Contains("include the /mcp endpoint path", error.Message);
        }
        finally
        {
            File.Delete(path);
        }
    }

    [Fact]
    public void FilePayloadUsesReceiverNameAndFileNameParameter()
    {
        var receivers = Assert.IsType<System.Text.Json.Nodes.JsonArray>(
            Scenarios.File("XMLFileReceiver", 1).ToJson()["receivers"]);
        var receiver = Assert.IsType<System.Text.Json.Nodes.JsonObject>(receivers[0]);
        var parameters = Assert.IsType<System.Text.Json.Nodes.JsonObject>(receiver["parameters"]);

        Assert.Equal("XMLFileReceiver", receiver["receiverName"]?.GetValue<string>());
        Assert.Equal("dataconnect-output.xml", parameters["fileName"]?.GetValue<string>());
    }

    private static DataConnectConfig Config() => new(
        "org",
        "client",
        "user",
        "secret",
        new Uri("https://rest.example.test"),
        new Uri("https://mcp.example.test/mcp"));

    private static HttpResponseMessage Json(HttpStatusCode status, string body) => new(status)
    {
        Content = new StringContent(body, Encoding.UTF8, "application/json"),
    };

    private sealed class DelegateHandler(Func<HttpRequestMessage, HttpResponseMessage> handler)
        : HttpMessageHandler
    {
        protected override Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request,
            CancellationToken cancellationToken)
        {
            cancellationToken.ThrowIfCancellationRequested();
            return Task.FromResult(handler(request));
        }
    }
}
