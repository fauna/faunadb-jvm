package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;
import com.google.common.base.Joiner;

/**
 * An immutable value-type representation of a FaunaDB Instance response. This, like other
 * response types, is created by coercing a {@link ResponseNode} using its associated conversion
 * method.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#guide-resource_types-instances">FaunaDB Resource Types</a></p>
 *
 * @see ResponseNode#asInstance()
 */
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

  /**
   * Returns the ref of this instance.
   */
  public Ref ref() {
    return ref;
  }

  /**
   * Returns the ref of the class of this instance.
   */
  public Ref classRef() {
    return classRef;
  }

  /**
   * Returns the timestamp of this instance.
   */
  public Long ts() {
    return ts;
  }

  /**
   * Returns the data of this instance.
   */
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
