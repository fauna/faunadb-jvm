package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;

/**
 * An immutable value-type representation of a FaunaDB Database response. This, like other
 * response types, is created by coercing a {@link ResponseNode} using its associated conversion method.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#guide-resource_types-databases">FaunaDB Resource Types</a></p>
 *
 * @see ResponseNode#asDatabase()
 */
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

  /**
   * Returns the ref to this database.
   */
  public Ref ref() {
    return ref;
  }

  /**
   * Returns the ref of this resource's class. In this case, {@code databases}.
   */
  public Ref classRef() {
    return classRef;
  }

  /**
   * Returns the timestamp of this resource.
   */
  public Long ts() {
    return ts;
  }

  /**
   * Returns the name of this database.
   */
  public String name() {
    return name;
  }
}
