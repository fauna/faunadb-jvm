package com.faunadb.client.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.LazyValue;
import com.faunadb.client.types.Ref;
import com.google.common.base.Joiner;

/**
 * A FaunaDB event. This, like other response types, is created by coercing a {@link com.faunadb.client.types.Value} using its associated
 * conversion method.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-values-events">FaunaDB Event Value</a></p>
 *
 * @see LazyValue#asEvent()
 */
public final class Event {
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

  /**
   * Returns the Ref to the resource that this Event refers to.
   */
  public Ref resource() {
    return resource;
  }

  /**
   * Returns the action of this event.
   */
  public String action() {
    return action;
  }

  /**
   * Returns the timestamp of this event.
   */
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
