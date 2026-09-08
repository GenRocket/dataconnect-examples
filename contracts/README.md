# Canonical contracts

These JSON files are shared examples for all language implementations. They
capture the request and response shapes documented by DataConnect; they are not
an OpenAPI definition and are not guaranteed to include every server field.

Use the request fixtures for dry runs and documentation. Unit tests may use the
response fixtures to exercise parsing without consuming DataConnect quota.

Never replace the placeholder token or identifiers with real credentials.

## Requests

- `requests/login.json`: programmatic login
- `requests/generate-explicit.json`: explicit generator selection
- `requests/generate-inferred.json`: description-driven selection
- `requests/generate-enum.json`: enumerated values
- `requests/generate-regex.json`: regular-expression constraint
- `requests/generate-mixed.json`: several attribute styles and context
- `requests/generate-file.json`: REST file receiver
- `requests/mcp-generate.json`: `generate_data` tool arguments

## Responses

- `responses/login-success.json`: successful login envelope
- `responses/industries.json` and `responses/generators.json`: discovery data
- `responses/generate-success.json`: inline generated rows
- `responses/error.json`: normal service error
- `responses/quota-error.json`: quota error returned in an HTTP 200 response
