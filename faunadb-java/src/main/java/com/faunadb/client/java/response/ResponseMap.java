package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@JsonDeserialize(using=Codec.ResponseMapDeserializer.class)
public class ResponseMap extends ForwardingMap<String, ResponseNode> {
  private final ImmutableMap<String, ResponseNode> underlying;

  @JsonCreator
  public ResponseMap(ImmutableMap<String, ResponseNode> underlying) {
    this.underlying = underlying;
  }

  @Override
  protected Map<String, ResponseNode> delegate() {
    return underlying;
  }
}
