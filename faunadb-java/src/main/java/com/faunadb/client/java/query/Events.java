package com.faunadb.client.java.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;

@JsonSerialize(using=Codec.EventsSerializer.class)
public class Events {
  public static Events create(Identifier resource) {
    return new Events(resource, Optional.<Cursor>absent(), Optional.<Long>absent());
  }

  private final Identifier resource;
  private final Optional<Cursor> cursor;
  private final Optional<Long> size;

  Events(Identifier resource, Optional<Cursor> cursor, Optional<Long> size) {
    this.resource = resource;
    this.cursor = cursor;
    this.size = size;
  }

  public Events withCursor(Cursor cursor) {
    return new Events(this.resource, Optional.of(cursor), this.size);
  }

  public Events withSize(Long size) {
    return new Events(this.resource, this.cursor, Optional.of(size));
  }

  public Identifier resource() {
    return resource;
  }

  public Optional<Cursor> cursor() {
    return cursor;
  }

  public Optional<Long> size() {
    return size;
  }
}
