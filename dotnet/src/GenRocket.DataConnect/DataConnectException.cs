namespace GenRocket.DataConnect;

public sealed class DataConnectException : Exception
{
    public DataConnectException(
        string message,
        string protocol,
        string operation,
        string? errorCode = null,
        int? statusCode = null,
        Exception? innerException = null)
        : base(message, innerException)
    {
        Protocol = protocol;
        Operation = operation;
        ErrorCode = errorCode;
        StatusCode = statusCode;
    }

    public string Protocol { get; }

    public string Operation { get; }

    public string? ErrorCode { get; }

    public int? StatusCode { get; }

    public bool IsAuthenticationError =>
        StatusCode is 401 or 403
        || ErrorCode is "DAAS_TOKEN_EXPIRED" or "DAAS_TOKEN_INVALID" or "TOKEN_EXPIRED" or "TOKEN_INVALID";

    public override string Message
    {
        get
        {
            var code = ErrorCode is null ? string.Empty : $", code={ErrorCode}";
            var status = StatusCode is null ? string.Empty : $", status={StatusCode}";
            return $"{Protocol} {Operation} failed{code}{status}: {base.Message}";
        }
    }
}
