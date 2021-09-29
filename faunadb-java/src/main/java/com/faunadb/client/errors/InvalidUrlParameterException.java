package com.faunadb.client.errors;

import java.util.List;

public class InvalidUrlParameterException extends FaunaException {

  public InvalidUrlParameterException(final String message, final int httpStatusCode, final List<String> position) {
    super(message, httpStatusCode, position);
  }

  @Override
  public CoreExceptionCodes code() {
    return CoreExceptionCodes.INVALID_URL_PARAMETER;
  }
}
