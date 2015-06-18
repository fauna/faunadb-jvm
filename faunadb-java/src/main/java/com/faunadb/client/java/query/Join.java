package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An immutable representation of a FaunaDB Join set.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-sets">FaunaDB Sets</a></p>
 *
 * @see Language#Join(Set, Lambda)
 */
public final class Join extends Set {
  public static Join create(Set source, Lambda target) {
    return new Join(source, target);
  }

  @JsonProperty("join")
  private final Set source;

  @JsonProperty("with")
  private final Lambda target;

  Join(Set source, Lambda target) {
    this.source = source;
    this.target = target;
  }

  public Set source() {
    return source;
  }

  public Lambda target() {
    return target;
  }
}
