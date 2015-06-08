package com.faunadb.client.java.response;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ResponseMap extends ForwardingMap<String, ResponseNode> {
  private final ImmutableMap<String, ResponseNode> underlying;

  public ResponseMap(ImmutableMap<String, ResponseNode> underlying) {
    this.underlying = underlying;
  }

  @Override
  protected Map<String, ResponseNode> delegate() {
    return underlying;
  }
}
