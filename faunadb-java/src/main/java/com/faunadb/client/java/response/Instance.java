package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;

public class Instance {
  @JsonProperty("ref")
  private final Ref ref;
  @JsonProperty("class")
  private final Ref classRef;
  @JsonProperty("ts")
  private final Long ts;
  @JsonProperty("data")
  private final ResponseMap data;

  Instance(Ref ref, Ref classRef, Long ts, ResponseMap data) {
    this.ref = ref;
    this.classRef = classRef;
    this.ts = ts;
    this.data = data;
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

  public ResponseMap data() {
    return data;
  }
}
