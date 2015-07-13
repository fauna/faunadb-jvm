package com.faunadb.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.LazyValue;
import com.faunadb.client.types.Value;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * A FaunaDB Page response. This, like other response types, is obtained by coercing a {@link Value} using its
 * associated conversion method.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-values-pages">FaunaDB Page Value</a> </p>
 *
 * @see Value#asPage()
 */
public final class Page {
  private final ImmutableList<Value> data;
  private final Optional<Value> before;
  private final Optional<Value> after;

  @JsonCreator
  Page(@JsonProperty("data") ImmutableList<LazyValue> data,
       @JsonProperty("before") Optional<LazyValue> before,
       @JsonProperty("after") Optional<LazyValue> after) {
    this.data = ImmutableList.<Value>copyOf(data);
    this.before = Optional.<Value>fromNullable(before.orNull());
    this.after = Optional.<Value>fromNullable(after.orNull());
  }

  /**
   * Returns an ordered list of the data in this page.
   */
  public ImmutableList<Value> data() {
    return data;
  }

  /**
   * Returns an {@link Optional} that wraps the before cursor if it exists.
   */
  public Optional<Value> before() {
    return before;
  }

  /**
   * Returns an {@link Optional} that wraps the after cursor if it exists.
   */
  public Optional<Value> after() {
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
