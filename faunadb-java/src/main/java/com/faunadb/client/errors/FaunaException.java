package com.faunadb.client.errors;

import java.util.List;

/**
 * The base type for all FaunaDB exceptions.
 */
public class FaunaException extends RuntimeException {
  private int httpStatusCode;
  private List<String> position;

  public FaunaException(String message) {
    super(message);
  }

  public FaunaException(String message, int httpStatusCode) {
    super(message);
    this.httpStatusCode = httpStatusCode;
  }

  public FaunaException(String message, int httpStatusCode, List<String> position) {
    super(message);
    this.httpStatusCode = httpStatusCode;
    this.position = position;
  }

  public FaunaException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Gets the HTTP status code of the underlying error response.
   *
   * @return HTTP status code
   */
  public int status() {
    return httpStatusCode;
  }

  public List<String> getPosition() {
    return position;
  }

  public CoreExceptionCodes code() {
    return null;
  }
}
