package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * An immutable dictionary of response nodes. FaunaDB responses can be polymorphic, so
 * this dictionary allows individual entries to be coerced to concrete response types as required.
 *
 * @see LazyValue
 * @see ForwardingMap
 */
@JsonDeserialize(using=Codec.LazyValueMapDeserializer.class)
public final class LazyValueMap extends ForwardingMap<String, Value> {
  private final ImmutableMap<String, Value> underlying;

  @JsonCreator
  LazyValueMap(ImmutableMap<String, Value> underlying) {
    this.underlying = underlying;
  }

  @Override
  protected Map<String, Value> delegate() {
    return underlying;
  }
}
