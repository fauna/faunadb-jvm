package com.faunadb.client.errors;

import java.util.List;

public class MissingIdentityException extends FaunaException {

  public MissingIdentityException(final String message, final int httpStatusCode, final List<String> position) {
    super(message, httpStatusCode, position);
  }

  @Override
  public CoreExceptionCodes code() {
    return CoreExceptionCodes.MISSING_IDENTITY;
  }
}
