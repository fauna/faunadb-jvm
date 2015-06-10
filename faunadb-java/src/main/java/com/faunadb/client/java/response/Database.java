package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
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

  @JsonCreator
  Database(@JsonProperty("ref") Ref ref,
           @JsonProperty("class") Ref classRef,
           @JsonProperty("ts") Long ts,
           @JsonProperty("name") String name) {
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
