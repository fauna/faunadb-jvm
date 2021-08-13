package com.faunadb.client.errors;

import java.util.List;

/**
 * An exception thrown if a FaunaDB response is unknown or unparseable by the client.
 */
public class UnknownException extends FaunaException {

  private CoreExceptionCodes code;

  public UnknownException(String message) { super(message); }

  public UnknownException(String message, int httpStatusCode, List<String> position, CoreExceptionCodes code) {
    super(message, httpStatusCode, position);
    this.code = code;
  }
  public UnknownException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public CoreExceptionCodes code() {
    return this.code;
  }
}
