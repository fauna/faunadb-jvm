package com.faunadb.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.LazyValue;
import com.faunadb.client.types.Ref;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

public final class Token extends Instance {
  @JsonProperty("instance")
  private final Ref instance;
  @JsonProperty("secret")
  private final String secret;

  @JsonCreator
  Token(@JsonProperty("ref") Ref ref,
        @JsonProperty("class") Ref classRef,
        @JsonProperty("ts") Long ts,
        @JsonProperty("instance") Ref instance,
        @JsonProperty("secret") String secret,
        @JsonProperty("data") ImmutableMap<String, LazyValue> data) {
    super(ref, classRef, ts, data);
    this.instance = instance;
    this.secret = secret;
  }

  public Ref instance() {
    return instance;
  }

  public String secret() {
    return secret;
  }

  @Override
  public String toString() {
    return "Token(" + Joiner.on(", ").join(
      "ref: " + ref(),
      "class: "+ classRef(),
      "instance: " + instance(),
      "secret: " + secret(),
      "ts: "+ ts(),
      "data: " +data()
    ) + ")";
  }
}
