package com.faunadb.client.errors;

import java.util.List;

/**
 * An exception thrown if a HTTP 403 (Forbidden) is returned from FaunaDB.
 */
public class PermissionDeniedException extends FaunaException {

  public PermissionDeniedException(String message, int httpStatusCode, List<String> position) {
    super(message, httpStatusCode, position);
  }

  @Override
  public CoreExceptionCodes code() {
    return CoreExceptionCodes.PERMISSION_DENIED;
  }
}
