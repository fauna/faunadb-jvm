package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonValue;
import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.LongV;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;

import static com.faunadb.client.util.Objects.requireNonNull;

/**
 * A pagination expression. Instances of this class are not thread safe and must not be
 * modified concurrently from different threads.
 * <p>
 * See {@link Language#Paginate(Expr)} for details.
 *
 * @see <a href="https://fauna.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
 * @see Language#Paginate(Expr)
 */
public final class Pagination extends Expr {

  private static abstract class Cursor {
    private final String name;
    private final Expr ref;

    private Cursor(String name, Expr ref) {
      this.name = requireNonNull(name);
      this.ref = requireNonNull(ref);
    }
  }

  private static final class Before extends Cursor {
    private Before(Expr ref) {
      super("before", ref);
    }
  }

  private static final class After extends Cursor {
    private After(Expr ref) {
      super("after", ref);
    }
  }

  private final Expr resource;
  private Optional<Cursor> cursor = Optional.absent();
  private Optional<Expr> ts = Optional.absent();
  private Optional<Expr> size = Optional.absent();
  private Optional<Expr> sources = Optional.absent();
  private Optional<Expr> events = Optional.absent();

  Pagination(Expr resource) {
    this.resource = resource;
  }

  @Override
  @JsonValue
  protected ImmutableMap<String, Expr> toJson() {
    ImmutableMap.Builder<String, Expr> res = ImmutableMap.builder();
    res.put("paginate", resource);

    if (cursor.isPresent()) res.put(cursor.get().name, cursor.get().ref);
    if (events.isPresent()) res.put("events", events.get());
    if (sources.isPresent()) res.put("sources", sources.get());
    if (ts.isPresent()) res.put("ts", ts.get());
    if (size.isPresent()) res.put("size", size.get());

    return res.build();
  }

  /**
   * Sets the cursor of the pagination to move backwards.
   *
   * @param cursor the before cursor
   * @return this {@link Pagination} instance
   */
  public Pagination before(Expr cursor) {
    this.cursor = Optional.<Cursor>of(new Before(cursor));
    return this;
  }

  /**
   * Sets the cursor of the pagination to move forward.
   *
   * @param cursor the after cursor
   * @return this {@link Pagination} instance
   */
  public Pagination after(Expr cursor) {
    this.cursor = Optional.<Cursor>of(new After(cursor));
    return this;
  }

  /**
   * Sets the timestamp for the pagination.
   *
   * @param ts the desired timestamp. Type: timestamp
   * @return this {@link Pagination} instance
   * @see Language#Time(Expr)
   * @see Language#Value(Instant)
   */
  public Pagination ts(Expr ts) {
    this.ts = Optional.of(ts);
    return this;
  }

  /**
   * Sets the timestamp for the pagination.
   *
   * @param ts the desired timestamp in UNIX microseconds
   * @return this {@link Pagination} instance
   */
  public Pagination ts(Long ts) {
    return ts(new LongV(ts));
  }

  /**
   * Sets the maximum number of elements per page to return.
   *
   * @param size the desired page size. Type: Number
   * @return this {@link Pagination} instance
   */
  public Pagination size(Expr size) {
    this.size = Optional.of(size);
    return this;
  }

  /**
   * Sets the maximum number of elements per page to return.
   *
   * @param size the desired page size
   * @return this {@link Pagination} instance
   */
  public Pagination size(Integer size) {
    return size(new LongV(size));
  }

  /**
   * Define if the pagination should return source information or not.
   * By default source information is not returned.
   *
   * @param sources a boolean value
   * @return this {@link Pagination} instance
   */
  public Pagination sources(Expr sources) {
    this.sources = Optional.of(sources);
    return this;
  }

  /**
   * Define if the pagination should return source information or not.
   * By default source information is not returned.
   *
   * @param sources a boolean value
   * @return this {@link Pagination} instance
   */
  public Pagination sources(boolean sources) {
    if (!sources) return this;
    return sources(Value.BooleanV.TRUE);
  }

  /**
   * Define if the pagination should return events or not.
   * By default events are not returned.
   *
   * @param events a boolean value
   * @return this {@link Pagination} instance
   */
  public Pagination events(Expr events) {
    this.events = Optional.of(events);
    return this;
  }

  /**
   * Define if the pagination should return events or not.
   * By default events are not returned.
   *
   * @param events a boolean value
   * @return this {@link Pagination} instance
   */
  public Pagination events(boolean events) {
    if (!events) return this;
    return events(Value.BooleanV.TRUE);
  }

}
