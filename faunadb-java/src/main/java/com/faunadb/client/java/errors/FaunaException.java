package com.faunadb.client.java.errors;

import com.faunadb.client.java.HttpResponses;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * The base type for all FaunaDB exceptions.
 */
public class FaunaException extends RuntimeException {
  private final Optional<HttpResponses.QueryErrorResponse> response;

  public FaunaException(HttpResponses.QueryErrorResponse response) {
    super(constructErrorMessage(response.errors()));
    this.response = Optional.of(response);
  }

  public FaunaException(String message) {
    super(message);
    this.response = Optional.absent();
  }

  /**
   * Gets the list of errors that caused the query to fail.
   */
  public ImmutableList<HttpResponses.QueryError> errors() {
    return response.get().errors();
  }

  /**
   * Gets the HTTP status code of the underlying error response.
   */
  public int status() {
    return response.get().status();
  }

  private static String constructErrorMessage(ImmutableList<HttpResponses.QueryError> errors) {
    ImmutableList.Builder<String> messages = ImmutableList.builder();
    for (HttpResponses.QueryError error : errors) {
      messages.add(error.code() + ": " + error.reason());
    }

    return Joiner.on(", ").join(messages.build());
  }
}
