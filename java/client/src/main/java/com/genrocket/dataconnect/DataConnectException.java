package com.genrocket.dataconnect;

public final class DataConnectException extends RuntimeException {
  private final String protocol;
  private final String operation;
  private final String errorCode;
  private final Integer statusCode;

  public DataConnectException(
      String message,
      String protocol,
      String operation,
      String errorCode,
      Integer statusCode,
      Throwable cause) {
    super(message, cause);
    this.protocol = protocol;
    this.operation = operation;
    this.errorCode = errorCode;
    this.statusCode = statusCode;
  }

  public DataConnectException(
      String message, String protocol, String operation, String errorCode, Integer statusCode) {
    this(message, protocol, operation, errorCode, statusCode, null);
  }

  public String protocol() {
    return protocol;
  }

  public String operation() {
    return operation;
  }

  public String errorCode() {
    return errorCode;
  }

  public Integer statusCode() {
    return statusCode;
  }

  public boolean isAuthenticationError() {
    return (statusCode != null && (statusCode == 401 || statusCode == 403))
        || "DAAS_TOKEN_EXPIRED".equals(errorCode)
        || "DAAS_TOKEN_INVALID".equals(errorCode)
        || "TOKEN_EXPIRED".equals(errorCode)
        || "TOKEN_INVALID".equals(errorCode);
  }

  @Override
  public String getMessage() {
    StringBuilder message = new StringBuilder(protocol + " " + operation + " failed");
    if (errorCode != null) {
      message.append(", code=").append(errorCode);
    }
    if (statusCode != null) {
      message.append(", status=").append(statusCode);
    }
    return message.append(": ").append(super.getMessage()).toString();
  }
}
