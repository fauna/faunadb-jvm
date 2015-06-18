package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;
import com.faunadb.client.java.types.Value;

/**
 * An immutable representation of a FaunaDB Match set.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-sets"></a>FaunaDB Sets</p>
 *
 * @see Language#Match(Value, Ref)
 */
public final class Match extends Set {
  @JsonProperty
  private final Ref index;
  @JsonProperty("match")
  private final Value term;

  public static Match create(Value term, Ref index) {
    return new Match(term, index);
  }

  Match(Value term, Ref index) {
    this.term = term;
    this.index = index;
  }

  public Ref index() {
    return index;
  }

  public Value term() {
    return term;
  }
}
