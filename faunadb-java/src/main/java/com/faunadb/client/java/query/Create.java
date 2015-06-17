package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Identifier;
import com.faunadb.client.java.types.Value.*;

public class Create implements Identifier, Expression {
  @JsonProperty("create")
  private final Identifier ref;
  @JsonProperty("params")
  private final Expression params;

  public static Create create(Identifier ref) {
    return new Create(ref, ObjectV.empty());
  }

  public static Create create(Identifier ref, Expression params) {
    return new Create(ref, params);
  }

  Create(Identifier ref, Expression params) {
    this.ref = ref;
    this.params = params;
  }
}
