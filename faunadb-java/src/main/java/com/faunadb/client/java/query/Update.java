package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.query.Value.*;

public class Update implements Identifier {
  public static Update create(Identifier ref, ObjectV params) {
    return new Update(ref, params);
  }

  @JsonProperty("update")
  private final Identifier ref;
  @JsonProperty("params")
  private final ObjectV params;

  Update(Identifier ref, ObjectV params) {
    this.ref = ref;
    this.params = params;
  }
}
