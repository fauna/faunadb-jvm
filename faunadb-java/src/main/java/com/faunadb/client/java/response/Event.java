package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;
import com.google.common.base.Joiner;

/**
 * An immutable value-type representation of a FaunaDB event. This, like other response types,
 * is created by coercing a {@link ResponseNode} using its associated conversion method.
 *
 * <p><i>Reference</i>: TBD</p>
 *
 * @see ResponseNode#asEvent()
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
