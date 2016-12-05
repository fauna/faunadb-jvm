package com.faunadb.client.errors;

import com.faunadb.client.HttpResponses;

/**
 * An exception thrown if a FaunaDB host is unavailable for any reason. For example, if the client cannot connect
 * to the host, or if the host does not respond.
 */
public class UnavailableException extends FaunaException {
  public UnavailableException(String message) {
    super("FaunaDB unavailable: "+message);
  }

  public UnavailableException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }
}
