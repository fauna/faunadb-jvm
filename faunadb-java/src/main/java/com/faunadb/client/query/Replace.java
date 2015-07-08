package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An immutable representation of a FaunaDB Replace function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-modifying-resources">FaunaDB Resource Modification Functions</a></p>
 *
 * @see Language#Replace(Expression, Expression)
 *
 */
public final class Replace implements Expression {
  public static Replace create(Expression ref, Expression params) {
    return new Replace(ref, params);
  }

  @JsonProperty("replace")
  private final Expression ref;
  @JsonProperty("params")
  private final Expression params;

  Replace(Expression ref, Expression params) {
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
