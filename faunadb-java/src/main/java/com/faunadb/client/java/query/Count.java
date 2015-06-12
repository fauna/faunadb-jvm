package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Count implements Identifier {
  public final static Count create(Set set) {
    return new Count(set);
  }

  @JsonProperty("count")
  private final Set set;

  Count(Set set) {
    this.set = set;
  }

  public Set set() {
    return set;
  }
}
