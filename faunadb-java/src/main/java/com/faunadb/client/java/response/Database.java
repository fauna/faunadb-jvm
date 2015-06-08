package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;

public class Database {
  @JsonProperty("ref")
  private final Ref ref;
  @JsonProperty("class")
  private final Ref classRef;
  @JsonProperty("ts")
  private final Long ts;
  @JsonProperty("name")
  private final String name;

  public Database(Ref ref, Ref classRef, Long ts, String name) {
    this.ref = ref;
    this.classRef = classRef;
    this.ts = ts;
    this.name = name;
  }

  public Ref ref() {
    return ref;
  }

  public Ref classRef() {
    return classRef;
  }

  public Long ts() {
    return ts;
  }

  public String name() {
    return name;
  }

}
