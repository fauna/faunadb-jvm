package com.faunadb.client.errors;

import java.util.List;

public class InvalidWriteTimeException extends FaunaException {

  public InvalidWriteTimeException(final String message, final int httpStatusCode, final List<String> position) {
    super(message, httpStatusCode, position);
  }

  @Override
  public CoreExceptionCodes code() {
    return CoreExceptionCodes.INVALID_WRITE_TIME;
  }
}
