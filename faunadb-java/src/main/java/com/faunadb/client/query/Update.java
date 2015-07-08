package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An immutable representation of a FaunaDB Update function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-modifying-resources">FaunaDB Resource Modification Functions</a></p>
 *
 * @see Language#Update(Expression, Expression)
 */
public final class Update implements Expression {
  public static Update create(Expression ref, Expression params) {
    return new Update(ref, params);
  }

  @JsonProperty("update")
  private final Expression ref;
  @JsonProperty("params")
  private final Expression params;

  Update(Expression ref, Expression params) {
    this.ref = ref;
    this.params = params;
  }

  public Expression ref() {
    return ref;
  }

  public Expression params() {
    return params;
  }
}
