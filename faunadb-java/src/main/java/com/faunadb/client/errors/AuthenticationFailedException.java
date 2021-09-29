package com.faunadb.client.errors;

import java.util.List;

public class AuthenticationFailedException extends FaunaException {
  public AuthenticationFailedException(final String message, final int httpStatusCode, final List<String> position) {
    super(message, httpStatusCode, position);
  }

  @Override
  public CoreExceptionCodes code() {
    return CoreExceptionCodes.AUTHENTICATION_FAILED;
  }
}
