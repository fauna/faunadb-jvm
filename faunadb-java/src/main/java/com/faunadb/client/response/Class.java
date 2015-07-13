package com.faunadb.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.LazyValue;
import com.faunadb.client.types.Ref;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

/**
 * A FaunaDB Class response. This, like other response types, is created by coercing a {@link LazyValue} using
 * one of the conversion methods.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#objects-classes">FaunaDB Class Object</a></p>
 *
 * @see LazyValue#asClass()
 */
public final class Class extends Instance {
  @JsonProperty("history_days")
  private final Long historyDays;
  @JsonProperty("name")
  private final String name;

  @JsonCreator
  Class(@JsonProperty("ref") Ref ref,
        @JsonProperty("class") Ref classRef,
        @JsonProperty("ts") Long ts,
        @JsonProperty("history_days") Long historyDays,
        @JsonProperty("name") String name,
        @JsonProperty("data") ImmutableMap<String, LazyValue> data) {
    super(ref, classRef, ts, data);
    this.historyDays = historyDays;
    this.name = name;
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
