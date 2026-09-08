# Authentication and errors

All integrations first post the four identity values to `/daas/login`. A
successful response contains a JWT valid for approximately 60 minutes.

- REST sends it as `X-Daas-Token`.
- MCP sends it as `Authorization: Bearer <token>`.
- The examples refresh in memory after 55 minutes.
- An expired or invalid token triggers one re-login and one retry.

No other generation request is automatically retried. A timeout or connection
failure can happen after data was generated and quota was consumed.

## Error shape

DataConnect errors use this logical shape:

```json
{
  "success": false,
  "errorCode": "DAAS_TOKEN_EXPIRED",
  "message": "Authentication token expired"
}
```

Always inspect `success`; quota errors intentionally can use HTTP 200. The
thin clients normalize REST and MCP failures into a language-specific
`DataConnectException` while retaining the safe error code and HTTP status.

Never log the API key or token. When troubleshooting, report only the language,
protocol, operation, HTTP status, and sanitized DataConnect error code.
