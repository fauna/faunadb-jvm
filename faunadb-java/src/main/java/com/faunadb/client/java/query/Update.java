package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Identifier;
import com.faunadb.client.java.types.Value.*;

public class Update implements Identifier, Expression {
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
}
