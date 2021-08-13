package com.faunadb.client.errors;

import java.util.List;

public class InstanceNotFound extends FaunaException {

  public InstanceNotFound(final String message, final int httpStatusCode, final List<String> position) {
    super(message, httpStatusCode, position);
  }

  @Override
  public CoreExceptionCodes code() {
    return CoreExceptionCodes.INSTANCE_NOT_FOUND;
  }
}
