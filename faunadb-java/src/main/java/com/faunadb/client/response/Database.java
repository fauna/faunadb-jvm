package com.faunadb.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.LazyValue;
import com.faunadb.client.types.Ref;
import com.google.common.collect.ImmutableMap;

/**
 * A FaunaDB Database response. This, like other response types, is created by coercing a {@link LazyValue}
 * using its associated conversion method.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#objects-databases">FaunaDB Database Object</a></p>
 *
 * @see LazyValue#asDatabase()
 */
public final class Database extends Instance {
  @JsonProperty("name")
  private final String name;

  @JsonCreator
  Database(@JsonProperty("ref") Ref ref,
           @JsonProperty("class") Ref classRef,
           @JsonProperty("ts") Long ts,
           @JsonProperty("name") String name,
           @JsonProperty("data") ImmutableMap<String, LazyValue> data) {
    super(ref, classRef, ts, data);
    this.name = name;
  }

  /**
   * Returns the name of this database.
   */
  public String name() {
    return name;
  }
}
