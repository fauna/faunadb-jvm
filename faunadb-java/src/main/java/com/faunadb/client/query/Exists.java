package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.Ref;

/**
 * A representation of the Exists function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-read_functions">FaunaDB Read Functions</a></p>
 */
public final class Exists implements Expression {
  /**
   * Obtains a new instance of this class.
   *
   * @see Language#Exists(Ref)
   */
  public final static Exists create(Ref ref) {
    return new Exists(ref);
  }

  @JsonProperty("exists")
  private final Ref ref;

  Exists(Ref ref) {
    this.ref = ref;
  }

  /**
   * Returns the ref to test the existence of.
   */
  public Ref ref() {
    return ref;
  }
}
