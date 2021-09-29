package com.faunadb.client.errors;

import java.util.List;

public class InstanceAlreadyExistsException extends FaunaException {

  public InstanceAlreadyExistsException(final String message, final int httpStatusCode, final List<String> position) {
    super(message, httpStatusCode, position);
  }

  @Override
  public CoreExceptionCodes code() {
    return CoreExceptionCodes.INSTANCE_ALREADY_EXISTS;
  }
}
