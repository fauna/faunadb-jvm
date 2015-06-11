package com.faunadb.client.java.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;

@JsonSerialize(using=Codec.PaginateSerializer.class)
public class Paginate implements Identifier {
  public static Paginate create(Identifier resource) {
    return new Paginate(resource, Optional.<Long>absent(), Optional.<Cursor>absent(), Optional.<Integer>absent());
  }

  private final Identifier resource;
  private final Optional<Long> ts;
  private final Optional<Cursor> cursor;
  private final Optional<Integer> size;

  Paginate(Identifier resource, Optional<Long> ts, Optional<Cursor> cursor, Optional<Integer> size) {
    this.resource = resource;
    this.ts = ts;
    this.cursor = cursor;
    this.size = size;
  }

  public Paginate withTs(Long ts) {
    return new Paginate(resource, Optional.of(ts), cursor, size);
  }

  public Paginate withCursor(Cursor cursor) {
    return new Paginate(resource, ts, Optional.of(cursor), size);
  }

  public Paginate withSize(Integer size) {
    return new Paginate(resource, ts, cursor, Optional.of(size));
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
}
