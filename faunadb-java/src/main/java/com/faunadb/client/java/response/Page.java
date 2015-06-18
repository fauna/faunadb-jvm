package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Value;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * An immutable value-type representation of a FaunaDB Page response. This, like other response types,
 * is obtained by coercing a {@link ResponseNode} using its associated conversion method.
 *
 * <p><i>Reference</i>: TBD </p>
 *
 * @see ResponseNode#asPage()
 */
public final class Page {
  private final ImmutableList<ResponseNode> data;
  private final Optional<ResponseNode> before;
  private final Optional<ResponseNode> after;

  @JsonCreator
  Page(@JsonProperty("data") ImmutableList<ResponseNode> data,
       @JsonProperty("before") Optional<ResponseNode> before,
       @JsonProperty("after") Optional<ResponseNode> after) {
    this.data = data;
    this.before = before;
    this.after = after;
  }

  /**
   * Returns an ordered list of the data in this page.
   */
  public ImmutableList<ResponseNode> data() {
    return data;
  }

  /**
   * Returns an {@link Optional} that wraps the before cursor if it exists.
   */
  public Optional<ResponseNode> before() {
    return before;
  }

  /**
   * Returns an {@link Optional} that wraps the after cursor if it exists.
   */
  public Optional<ResponseNode> after() {
    return after;
  }

  @Override
  public String toString() {
    return "Page(" + Joiner.on(", ").join(
        "data: " + data(),
        "before: " + before(),
        "after: " + after()
    ) + ")";
  }
}
