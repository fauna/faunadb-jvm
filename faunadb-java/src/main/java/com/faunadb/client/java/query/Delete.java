package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Delete implements Expression {
  public static Delete create(Identifier ref) {
    return new Delete(ref);
  }

  @JsonProperty("delete")
  private final Identifier ref;

  Delete(Identifier ref) {
    this.ref = ref;
  }
}
