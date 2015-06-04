package com.faunadb.client.java.query;

import com.faunadb.client.java.query.Value.*;

public class Replace implements Identifier {
  private final Identifier ref;
  private final ObjectV obj;

  static Replace create(Identifier ref, ObjectV obj) {
    return new Replace(ref, obj);
  }

  Replace(Identifier ref, ObjectV obj) {
    this.ref = ref;
    this.obj = obj;
  }
}
