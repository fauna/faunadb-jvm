package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;

public class Match extends Set {
  @JsonProperty
  private final Ref index;
  @JsonProperty("match")
  private final Value term;

  public static Match create(Ref term, Ref index) {
    return new Match(Value.RefV.create(term), index);
  }

  public static Match create(Value term, Ref index) {
    return new Match(term, index);
  }

  Match(Value term, Ref index) {
    this.term = term;
    this.index = index;
  }
}
