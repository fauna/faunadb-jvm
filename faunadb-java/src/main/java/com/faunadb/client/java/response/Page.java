package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class Page {
  private final ImmutableList<ResponseNode> data;
  private final Optional<Ref> before;
  private final Optional<Ref> after;

  @JsonCreator
  Page(@JsonProperty("data") ImmutableList<ResponseNode> data,
       @JsonProperty("before") Optional<Ref> before,
       @JsonProperty("after") Optional<Ref> after) {
    this.data = data;
    this.before = before;
    this.after = after;
  }

  public ImmutableList<ResponseNode> data() {
    return data;
  }

  public Optional<Ref> before() {
    return before;
  }

  public Optional<Ref> after() {
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
