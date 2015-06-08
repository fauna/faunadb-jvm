package com.faunadb.client.java.response;

import com.faunadb.client.java.types.Ref;

public class Database {
  private final Ref ref;
  private final Ref classRef;
  private final Long ts;
  private final String name;

  public Database(Ref ref, Ref classRef, Long ts, String name) {
    this.ref = ref;
    this.classRef = classRef;
    this.ts = ts;
    this.name = name;
  }
}
