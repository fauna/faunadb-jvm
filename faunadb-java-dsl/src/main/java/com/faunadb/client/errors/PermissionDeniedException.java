package com.faunadb.client.errors;

import com.faunadb.client.HttpResponses;

/**
 * An exception thrown if a HTTP 403 (Forbidden) is returned from FaunaDB.
 */
public class PermissionDeniedException extends FaunaException {
  public PermissionDeniedException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }

  public PermissionDeniedException(String message) {
    super(message);
  }
}
