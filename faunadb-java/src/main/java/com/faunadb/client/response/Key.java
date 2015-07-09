package com.faunadb.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.LazyValue;
import com.faunadb.client.types.Ref;
import com.faunadb.client.types.Value;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

/**
 * An immutable value-type representation of a FaunaDB Key response. This, like other response types,
 * is obtained by coercing a {@link LazyValue} using its associated conversion method.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#guide-resource_types-keys">FaunaDB Resource Types</a></p>
 *
 * @see LazyValue#asKey()
 */
public final class Key {
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
  private final ImmutableMap<String, Value> data;

  @JsonCreator
  Key(@JsonProperty("ref") Ref ref,
      @JsonProperty("class") Ref classRef,
      @JsonProperty("database") Ref database,
      @JsonProperty("role") String role,
      @JsonProperty("secret") String secret,
      @JsonProperty("hashed_secret") String hashedSecret,
      @JsonProperty("ts") Long ts,
      @JsonProperty("data") ImmutableMap<String, LazyValue> data) {

    this.ref = ref;
    this.classRef = classRef;
    this.database = database;
    this.role = role;
    this.secret = secret;
    this.hashedSecret = hashedSecret;
    this.ts = ts;
    this.data = ImmutableMap.<String, Value>copyOf(data);
  }

  /**
   * Returns the ref to this key.
   */
  public Ref ref() {
    return ref;
  }

  /**
   * Returns the ref of the database that this key grants access to.
   */
  public Ref database() {
    return database;
  }

  /**
   * Returns the ref of the class of this key. In this case, {@code keys}.
   */
  public Ref classRef() {
    return classRef;
  }

  /**
   * Returns the key's secret.
   */
  public String secret() {
    return secret;
  }

  /**
   * Returns a hash of the key's secret.
   */
  public String hashedSecret() {
    return hashedSecret;
  }

  /**
   * Returns the timestamp of the key.
   */
  public Long ts() {
    return ts;
  }

  /**
   * Returns the data contained by the key.
   */
  public ImmutableMap<String, Value> data() {
    return data;
  }

  /**
   * Returns the key's role.
   */
  public String role() {
    return role;
  }

  @Override
  public String toString() {
    return "Key(" + Joiner.on(", ").join(
      "ref: " + ref,
      "class: " + classRef,
      "database: " + database,
      "role: " + role,
      "secret: "+ secret,
      "hashedSecret: " +hashedSecret,
      "ts: " + ts,
      "data: " + data
    ) + ")";
  }
}
