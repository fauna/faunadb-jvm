package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.query.Value.*;
import com.faunadb.client.java.types.Ref;

public class Create implements Identifier {
  @JsonProperty("create")
  private final Identifier ref;
  @JsonProperty("params")
  private final ObjectV params;

  public static Create create(Identifier ref) {
    return new Create(ref, ObjectV.empty());
  }

  public static Create create(Ref ref) {
    return new Create(RefV.create(ref), ObjectV.empty());
  }

  public static Create create(Identifier ref, ObjectV params) {
    return new Create(ref, params);
  }

  public static Create create(Ref ref, ObjectV params) {
    return new Create(RefV.create(ref), params);
  }

  Create(Identifier ref, ObjectV params) {
    this.ref = ref;
    this.params = params;
  }
}
