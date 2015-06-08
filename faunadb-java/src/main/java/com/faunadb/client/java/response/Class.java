package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;

public class Class {
  @JsonProperty("ref")
  private final Ref ref;
  @JsonProperty("class")
  private final Ref classRef;
  @JsonProperty("ts")
  private final Long ts;
  @JsonProperty("history_days")
  private final Long historyDays;
  @JsonProperty("name")
  private final String name;

  public Class(Ref ref, Ref classRef, Long ts, Long historyDays, String name) {
    this.ref = ref;
    this.classRef = classRef;
    this.ts = ts;
    this.historyDays = historyDays;
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

  public Long historyDays() {
    return historyDays;
  }

  public String name() {
    return name;
  }

}
