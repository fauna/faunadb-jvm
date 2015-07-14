package com.faunadb.client.query;

import com.faunadb.client.types.Value;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * A Builder for the FaunaDB Paginate function.
 *
 * <p>The paginate function takes optional parameters. These can be specified by the {@code withParameter} builder
 * style methods, each which returns a copy.</p>
 *
 * <p>Call {@link PaginateBuilder#build()} to create the Paginate function value.</p>
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-reading-resources">FaunaDB Resource Retrieval Functions</a>
 *
 * @see Language#Paginate(Value)
 *
 */
public final class PaginateBuilder {
  public static PaginateBuilder create(Value resource) {
    return new PaginateBuilder(resource, Optional.<Long>absent(), Optional.<Cursor>absent(), Optional.<Integer>absent(), false, false);
  }

  private final Value resource;
  private final Optional<Long> ts;
  private final Optional<Cursor> cursor;
  private final Optional<Integer> size;
  private final boolean sources;
  private final boolean events;

  PaginateBuilder(Value resource, Optional<Long> ts, Optional<Cursor> cursor, Optional<Integer> size, boolean sources, boolean events) {
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
  public PaginateBuilder withTs(Long ts) {
    return new PaginateBuilder(resource, Optional.of(ts), cursor, size, sources, events);
  }

  /**
   * Returns a copy of this with the optional cursor parameter set.
   * @param cursor the cursor parameter for the paginate function.
   */
  public PaginateBuilder withCursor(Cursor cursor) {
    return new PaginateBuilder(resource, ts, Optional.of(cursor), size, sources, events);
  }

  /**
   * Returns a copy of this with the optional size parameter set.
   * @param size the size parameter for the paginate function.
   */
  public PaginateBuilder withSize(Integer size) {
    return new PaginateBuilder(resource, ts, cursor, Optional.of(size), sources, events);
  }

  /**
   * Returns a copy of this with the optional sources parameter set.
   * @param s the sources parameter for the paginate function.
   */
  public PaginateBuilder withSources(boolean s) {
    return new PaginateBuilder(resource, ts, cursor, size, s, events);
  }

  /**
   * Returns a copy of this with the optional events parameter set.
   * @param e the events parameter for the paginate function.
   */
  public PaginateBuilder withEvents(boolean e) {
    return new PaginateBuilder(resource, ts, cursor, size, sources, e);
  }

  public Value resource() {
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

  /**
   * Builds the {@link Value} to be used to compose a Query.
   */
  public Value build() {
    ImmutableMap.Builder<String, Value> rv = ImmutableMap.builder();
    rv.put("paginate", resource);
    if (ts.isPresent()) {
      rv.put("ts", Value.LongV.create(ts.get()));
    }

    if (cursor.isPresent()) {
      Cursor c = cursor.get();
      if (c instanceof Cursor.Before) {
        rv.put("before", ((Cursor.Before)c).value());
      } else if (c instanceof Cursor.After) {
        rv.put("after", ((Cursor.After)c).value());
      }
    }

    if (size.isPresent()) {
      rv.put("size", Value.LongV.create(size.get()));
    }

    if (events) {
      rv.put("events", Value.BooleanV.create(events));
    }

    if (sources) {
      rv.put("sources", Value.BooleanV.create(sources));
    }

    return Value.ObjectV.create(rv.build());
  }
}
