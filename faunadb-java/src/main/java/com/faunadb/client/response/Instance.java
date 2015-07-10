package com.faunadb.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.Ref;
import com.faunadb.client.types.LazyValue;
import com.faunadb.client.types.Value;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

/**
 * An immutable value-type representation of a FaunaDB Instance response. This, like other
 * response types, is created by coercing a {@link LazyValue} using its associated conversion
 * method.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#guide-resource_types-instances">FaunaDB Resource Types</a></p>
 *
 * @see LazyValue#asInstance()
 */
public final class Instance {
  @JsonProperty("ref")
  private final Ref ref;
  @JsonProperty("class")
  private final Ref classRef;
  @JsonProperty("ts")
  private final Long ts;
  @JsonProperty("data")
  private final ImmutableMap<String, Value> data;

  @JsonCreator
  Instance(@JsonProperty("ref") Ref ref,
           @JsonProperty("class") Ref classRef,
           @JsonProperty("ts") Long ts,
           @JsonProperty("data") ImmutableMap<String, LazyValue> data) {
    this.ref = ref;
    this.classRef = classRef;
    this.ts = ts;
    if (data == null) {
      this.data = ImmutableMap.of();
    } else {
      this.data = ImmutableMap.<String, Value>copyOf(data);
    }
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
  public ImmutableMap<String, Value> data() {
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
