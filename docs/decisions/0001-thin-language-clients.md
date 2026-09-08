# ADR 0001: Thin language-local clients

**Status:** Accepted

Each language owns a small, non-published client layer shared by its examples.
This keeps request construction readable while centralizing configuration,
authentication, redaction, and error handling. Standalone scripts would repeat
security-sensitive logic; published SDKs would create an unsupported public
API and release burden.
