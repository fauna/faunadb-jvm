package com.faunadb.client.errors;

import java.util.List;

public class InvalidReferenceException extends FaunaException {

  public InvalidReferenceException(final String message, final int httpStatusCode, final List<String> position) {
    super(message, httpStatusCode, position);
  }

  @Override
  public CoreExceptionCodes code() {
    return CoreExceptionCodes.INVALID_REF;
  }
}
