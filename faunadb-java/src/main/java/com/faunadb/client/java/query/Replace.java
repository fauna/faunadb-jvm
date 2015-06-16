package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Identifier;
import com.faunadb.client.java.types.Value.*;

public class Replace implements Identifier, Expression {
  public static Replace create(Identifier ref, ObjectV obj) {
    return new Replace(ref, obj);
  }

  @JsonProperty("replace")
  private final Identifier ref;
  @JsonProperty("params")
  private final ObjectV obj;

  Replace(Identifier ref, ObjectV obj) {
    this.ref = ref;
    this.obj = obj;
  }
}
