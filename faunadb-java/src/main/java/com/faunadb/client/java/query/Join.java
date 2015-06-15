package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Join {
  public static Join create(Set source, Lambda target) {
    return new Join(source, target);
  }

  @JsonProperty("join")
  private final Set source;

  @JsonProperty("with")
  private final Lambda target;

  Join(Set source, Lambda target) {
    this.source = source;
    this.target = target;
  }
}
