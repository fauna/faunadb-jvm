package com.faunadb.client.java.response;

import com.faunadb.client.java.types.Ref;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class Page {
  private final ImmutableList<ResponseNode> data;
  private final Optional<Ref> before;
  private final Optional<Ref> after;

  public Page(ImmutableList<ResponseNode> data, Optional<Ref> before, Optional<Ref> after) {
    this.data = data;
    this.before = before;
    this.after = after;
  }

  public ImmutableList<ResponseNode> data() {
    return data;
  }

  public Optional<Ref> before() {
    return before;
  }

  public Optional<Ref> after() {
    return after;
  }

}
