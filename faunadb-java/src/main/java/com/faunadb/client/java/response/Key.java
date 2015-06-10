package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;

public class Key {
  @JsonProperty("ref")
  private final Ref ref;
  @JsonProperty("class")
  private final Ref classRef;
  @JsonProperty("database")
  private final Ref database;
  @JsonProperty("role")
  private final String role;
  @JsonProperty("secret")
  private final String secret;
  @JsonProperty("hashed_secret")
  private final String hashedSecret;
  @JsonProperty("ts")
  private final Long ts;
  @JsonProperty("data")
  private final ResponseMap data;

  @JsonCreator
  Key(@JsonProperty("ref") Ref ref,
      @JsonProperty("class") Ref classRef,
      @JsonProperty("database") Ref database,
      @JsonProperty("role") String role,
      @JsonProperty("secret") String secret,
      @JsonProperty("hashed_secret") String hashedSecret,
      @JsonProperty("ts") Long ts,
      @JsonProperty("data") ResponseMap data) {

    this.ref = ref;
    this.classRef = classRef;
    this.database = database;
    this.role = role;
    this.secret = secret;
    this.hashedSecret = hashedSecret;
    this.ts = ts;
    this.data = data;
  }

  public Ref ref() {
    return ref;
  }

  public Ref database() {
    return database;
  }

  public Ref classRef() {
    return classRef;
  }

  public String secret() {
    return secret;
  }

  public String hashedSecret() {
    return hashedSecret;
  }

  public Long ts() {
    return ts;
  }

  public ResponseMap data() {
    return data;
  }

  public String role() {
    return role;
  }
}
