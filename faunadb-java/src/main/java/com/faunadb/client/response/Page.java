package com.faunadb.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.LazyValue;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * An immutable value-type representation of a FaunaDB Page response. This, like other response types,
 * is obtained by coercing a {@link LazyValue} using its associated conversion method.
 *
 * <p><i>Reference</i>: TBD </p>
 *
 * @see LazyValue#asPage()
 */
public final class Page {
  private final ImmutableList<LazyValue> data;
  private final Optional<LazyValue> before;
  private final Optional<LazyValue> after;

  @JsonCreator
  Page(@JsonProperty("data") ImmutableList<LazyValue> data,
       @JsonProperty("before") Optional<LazyValue> before,
       @JsonProperty("after") Optional<LazyValue> after) {
    this.data = data;
    this.before = before;
    this.after = after;
  }

  /**
   * Returns an ordered list of the data in this page.
   */
  public ImmutableList<LazyValue> data() {
    return data;
  }

  /**
   * Returns an {@link Optional} that wraps the before cursor if it exists.
   */
  public Optional<LazyValue> before() {
    return before;
  }

  /**
   * Returns an {@link Optional} that wraps the after cursor if it exists.
   */
  public Optional<LazyValue> after() {
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
