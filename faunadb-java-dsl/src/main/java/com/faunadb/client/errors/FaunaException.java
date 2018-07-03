package com.faunadb.client.errors;

import com.faunadb.client.HttpResponses;
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

  public FaunaException(String message, Throwable cause) {
    super(message, cause);
    this.response = Optional.absent();
  }

  /**
   * Gets the list of errors that caused the query to fail.
   *
   * @return a list of errors
   */
  public ImmutableList<HttpResponses.QueryError> errors() {
    if (response.isPresent()) {
      return response.get().errors();
    } else {
      return ImmutableList.of();
    }
  }

  /**
   * Gets the HTTP status code of the underlying error response.
   *
   * @return HTTP status code
   */
  public int status() {
    if (response.isPresent()) {
      return response.get().status();
    } else {
      return 0;
    }
  }

  private static String constructErrorMessage(ImmutableList<HttpResponses.QueryError> errors) {
    ImmutableList.Builder<String> messages = ImmutableList.builder();
    for (HttpResponses.QueryError error : errors) {
      messages.add(error.code() + ": " + error.description());
    }

    return Joiner.on(", ").join(messages.build());
  }
}
