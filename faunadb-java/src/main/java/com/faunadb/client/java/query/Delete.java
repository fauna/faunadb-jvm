package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Identifier;

/** An immutable representation of a FaunaDB Delete function.
 *
 * @see Language#Delete(Identifier)
 */
public class Delete implements Expression {
  public static Delete create(Identifier ref) {
    return new Delete(ref);
  }

  @JsonProperty("delete")
  private final Identifier ref;

  Delete(Identifier ref) {
    this.ref = ref;
  }

  public Identifier ref() {
    return ref;
  }
}
