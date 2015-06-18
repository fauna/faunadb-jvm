package com.faunadb.client.java.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.faunadb.client.java.types.Identifier;
import com.google.common.base.Optional;

/**
 * An immutable representation of a FaunaDB paginate function.
 *
 * <p>The paginate function takes optional parameters. These can be specified by the {@code withParameter} builder
 * style methods, each which returns a copy.</p>
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-reading-resources">FaunaDB Resource Retrieval Functions</a>
 *
 * @see Language#Paginate(Identifier)
 *
 */
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

  /**
   * Returns a copy of this with the optional timestamp parameter set.
   * @param ts the timestamp parameter for the paginate function.
   */
  public Paginate withTs(Long ts) {
    return new Paginate(resource, Optional.of(ts), cursor, size, sources, events);
  }

  /**
   * Returns a copy of this with the optional cursor parameter set.
   * @param cursor the cursor parameter for the paginate function.
   */
  public Paginate withCursor(Cursor cursor) {
    return new Paginate(resource, ts, Optional.of(cursor), size, sources, events);
  }

  /**
   * Returns a copy of this with the optional size parameter set.
   * @param size the size parameter for the paginate function.
   */
  public Paginate withSize(Integer size) {
    return new Paginate(resource, ts, cursor, Optional.of(size), sources, events);
  }

  /**
   * Returns a copy of this with the optional sources parameter set.
   * @param s the sources parameter for the paginate function.
   */
  public Paginate withSources(boolean s) {
    return new Paginate(resource, ts, cursor, size, s, events);
  }

  /**
   * Returns a copy of this with the optional events parameter set.
   * @param e the events parameter for the paginate function.
   */
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
