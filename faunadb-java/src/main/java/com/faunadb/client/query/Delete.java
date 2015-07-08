package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;

/** An immutable representation of a FaunaDB Delete function.
 *
 * @see Language#Delete(Expression)
 */
public final class Delete implements Expression {
  public static Delete create(Expression ref) {
    return new Delete(ref);
  }

  @JsonProperty("delete")
  private final Expression ref;

  Delete(Expression ref) {
    this.ref = ref;
  }

  public Expression ref() {
    return ref;
  }
}
