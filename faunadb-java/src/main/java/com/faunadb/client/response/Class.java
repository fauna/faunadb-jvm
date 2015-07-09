package com.faunadb.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.LazyValue;
import com.faunadb.client.types.Ref;
import com.google.common.base.Joiner;

/**
 * An immutable value-type representation of a FaunaDB Class response. This, like other response
 * types, is created by coercing a {@link LazyValue} using one of the conversion methods.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#guide-resource_types-classes">FaunaDB Resource Types</a></p>
 *
 * @see LazyValue#asClass()
 */
public final class Class {
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

  @JsonCreator
  Class(@JsonProperty("ref") Ref ref,
        @JsonProperty("class") Ref classRef,
        @JsonProperty("ts") Long ts,
        @JsonProperty("history_days") Long historyDays,
        @JsonProperty("name") String name) {
    this.ref = ref;
    this.classRef = classRef;
    this.ts = ts;
    this.historyDays = historyDays;
    this.name = name;
  }

  /**
   * Returns the ref to this class.
   */
  public Ref ref() {
    return ref;
  }

  /**
   * Returns the ref of the class of the resource. In this case, {@code classes/}.
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
   * Returns the number of days of event history retention.
   */
  public Long historyDays() {
    return historyDays;
  }

  /**
   * Returns the name of the class.
   */
  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return "Class(" + Joiner.on(", ").join(
      "ref: " + ref(),
      "class: " + classRef(),
      "ts: " + ts(),
      "historyDays: " + historyDays(),
      "name: " + name()
    ) + ")";
  }
}
