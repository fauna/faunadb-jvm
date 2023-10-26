package com.faunadb.client.errors;

import com.faunadb.client.HttpResponses;

public class ResourceNotAvailableException extends FaunaException {
  public ResourceNotAvailableException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }
  public ResourceNotAvailableException(String message) {
    super(message);
  }
}
