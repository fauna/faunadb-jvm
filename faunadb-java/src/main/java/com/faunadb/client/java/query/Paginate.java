package com.faunadb.client.java.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.faunadb.client.java.types.Identifier;
import com.google.common.base.Optional;

@JsonSerialize(using=Codec.PaginateSerializer.class)
public class Paginate implements Identifier, Expression {
  public static Paginate create(Identifier resource) {
    return new Paginate(resource, Optional.<Long>absent(), Optional.<Cursor>absent(), Optional.<Integer>absent(), false, false);
  }

  private final Identifier resource;
  private final Optional<Long> ts;
  private final Optional<Cursor> cursor;
  private final Optional<Integer> size;
  private final boolean sources;
  private final boolean events;

  Paginate(Identifier resource, Optional<Long> ts, Optional<Cursor> cursor, Optional<Integer> size, boolean sources, boolean events) {
    this.resource = resource;
    this.ts = ts;
    this.cursor = cursor;
    this.size = size;
    this.sources = sources;
    this.events = events;
  }

  public Paginate withTs(Long ts) {
    return new Paginate(resource, Optional.of(ts), cursor, size, sources, events);
  }

  public Paginate withCursor(Cursor cursor) {
    return new Paginate(resource, ts, Optional.of(cursor), size, sources, events);
  }

  public Paginate withSize(Integer size) {
    return new Paginate(resource, ts, cursor, Optional.of(size), sources, events);
  }

  public Paginate withSources(boolean s) {
    return new Paginate(resource, ts, cursor, size, s, events);
  }

  public Paginate withEvents(boolean e) {
    return new Paginate(resource, ts, cursor, size, sources, e);
  }

  public Identifier resource() {
    return resource;
  }

  public Optional<Long> ts() {
    return ts;
  }

  public Optional<Cursor> cursor() {
    return cursor;
  }

  public Optional<Integer> size() {
    return size;
  }

  public boolean sources() {
    return sources;
  }

  public boolean events() {
    return events;
  }
}
