package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * An immutable dictionary of response nodes. FaunaDB responses can be polymorphic, so
 * this dictionary allows individual entries to be coerced to concrete response types as required.
 *
 * @see ResponseNode
 * @see ForwardingMap
 */
@JsonDeserialize(using=Codec.ResponseMapDeserializer.class)
public final class ResponseMap extends ForwardingMap<String, ResponseNode> {
  private final ImmutableMap<String, ResponseNode> underlying;

  @JsonCreator
  ResponseMap(ImmutableMap<String, ResponseNode> underlying) {
    this.underlying = underlying;
  }

  @Override
  protected Map<String, ResponseNode> delegate() {
    return underlying;
  }
}
