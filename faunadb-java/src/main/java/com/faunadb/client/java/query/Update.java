package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Identifier;
import com.faunadb.client.java.types.Value.*;

/**
 * An immutable representation of a FaunaDB Update function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-modifying-resources">FaunaDB Resource Modification Functions</a></p>
 *
 * @see Language#Update(Identifier, Expression)
 */
public final class Update implements Identifier, Expression {
  public static Update create(Identifier ref, Expression params) {
    return new Update(ref, params);
  }

  @JsonProperty("update")
  private final Identifier ref;
  @JsonProperty("params")
  private final Expression params;

  Update(Identifier ref, Expression params) {
    this.ref = ref;
    this.params = params;
  }

  public Identifier ref() {
    return ref;
  }

  public Expression params() {
    return params;
  }
}
