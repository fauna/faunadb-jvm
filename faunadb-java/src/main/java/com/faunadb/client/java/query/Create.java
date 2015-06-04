package com.faunadb.client.java.query;

import com.faunadb.client.java.query.Value.*;

public class Create implements Identifier {
  private final Identifier ref;
  private final ObjectV params;

  public static Create create(Identifier ref) {
    return new Create(ref, ObjectV.empty());
  }

  public static Create create(Identifier ref, ObjectV params) {
    return new Create(ref, params);
  }

  Create(Identifier ref, ObjectV params) {
    this.ref = ref;
    this.params = params;
  }
}
