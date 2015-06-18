package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Identifier;
import com.faunadb.client.java.types.Value.*;

/**
 * An immutable representation of a FaunaDB Replace function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-modifying-resources">FaunaDB Resource Modification Functions</a></p>
 *
 * @see Language#Replace(Identifier, Expression)
 *
 */
public class Replace implements Identifier, Expression {
  public static Replace create(Identifier ref, Expression params) {
    return new Replace(ref, params);
  }

  @JsonProperty("replace")
  private final Identifier ref;
  @JsonProperty("params")
  private final Expression params;

  Replace(Identifier ref, Expression params) {
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
