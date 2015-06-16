package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;
import com.google.common.base.Joiner;

public class Event {
  @JsonProperty("ts")
  private final Long ts;
  @JsonProperty("action")
  private final String action;
  @JsonProperty("resource")
  private final Ref resource;

  @JsonCreator
  Event(@JsonProperty("resource") Ref resource, @JsonProperty("action") String action, @JsonProperty("ts") Long ts) {
    this.resource = resource;
    this.action = action;
    this.ts = ts;
  }

  public Ref resource() {
    return resource;
  }

  public String action() {
    return action;
  }

  public Long ts() {
    return ts;
  }

  @Override
  public String toString() {
    return "Event(" + Joiner.on(", ").join(
      "resource: " + resource(),
      "action: " + action(),
      "ts: " + ts()
    ) +")";
  }
}
