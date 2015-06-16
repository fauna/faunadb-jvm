package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Value;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class Page {
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

  public ImmutableList<ResponseNode> data() {
    return data;
  }

  public Optional<ResponseNode> before() {
    return before;
  }

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
