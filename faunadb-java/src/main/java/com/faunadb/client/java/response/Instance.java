package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;
import com.google.common.base.Joiner;

public class Instance {
  @JsonProperty("ref")
  private final Ref ref;
  @JsonProperty("class")
  private final Ref classRef;
  @JsonProperty("ts")
  private final Long ts;
  @JsonProperty("data")
  private final ResponseMap data;

  @JsonCreator
  Instance(@JsonProperty("ref") Ref ref,
           @JsonProperty("class") Ref classRef,
           @JsonProperty("ts") Long ts,
           @JsonProperty("data") ResponseMap data) {
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

  @Override
  public String toString() {
    return "Instance(" + Joiner.on(", ").join(
      "ref: "+ref,
      "class: "+classRef,
      "ts: "+ts,
      "data: "+data
    ) + ")";
  }
}
