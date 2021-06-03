package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonValue;
import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.LongV;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A pagination expression. Instances of this class are not thread safe and must not be
 * modified concurrently from different threads.
 * <p>
 * See {@link Language#Paginate(Expr)} for details.
 *
 * @see <a href="https://docs.fauna.com/fauna/current/api/fql/functions/paginate?lang=java">Paginate</a>
 * @see Language#Paginate(Expr)
 */
public final class Pagination extends Expr {

  private static abstract class Cursor {
    private final String name;
    private final Expr ref;

    private Cursor(String name, Expr ref) {
      this.name = Objects.requireNonNull(name);
      this.ref = Objects.requireNonNull(ref);
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

  private static final class RawCursor extends Cursor {
    private RawCursor(Expr obj) {
      super("cursor", obj);
    }
  }

  private final Expr resource;
  private Optional<Cursor> cursor = Optional.empty();
  private Optional<Expr> ts = Optional.empty();
  private Optional<Expr> size = Optional.empty();
  private Optional<Expr> sources = Optional.empty();
  private Optional<Expr> events = Optional.empty();

  Pagination(Expr resource) {
    this.resource = resource;
  }

  @Override
  @JsonValue
  protected Map<String, Expr> toJson() {
    Map<String, Expr> res = new LinkedHashMap<>();
    res.put("paginate", resource);

    cursor.ifPresent(cur -> res.put(cur.name, cur.ref));
    putIfPresent(events, res, "events");
    putIfPresent(sources, res, "sources");
    putIfPresent(ts, res, "ts");
    putIfPresent(size, res, "size");

    return Collections.unmodifiableMap(res);
  }

  private static void putIfPresent(Optional<Expr> optExpr, Map<String, Expr> res, String name) {
    optExpr.ifPresent(expr -> res.put(name, expr));
  }

  /**
   * Sets the cursor object of the pagination.
   *
   * @param cursor the cursor object
   * @return this {@link Pagination} instance
   */
  public Pagination cursor(Expr cursor) {
    this.cursor = Optional.of(new RawCursor(cursor));
    return this;
  }

  /**
   * Sets the cursor of the pagination to move backwards.
   *
   * @param cursor the before cursor
   * @return this {@link Pagination} instance
   */
  public Pagination before(Expr cursor) {
    this.cursor = Optional.of(new Before(cursor));
    return this;
  }

  /**
   * Sets the cursor of the pagination to move forward.
   *
   * @param cursor the after cursor
   * @return this {@link Pagination} instance
   */
  public Pagination after(Expr cursor) {
    this.cursor = Optional.of(new After(cursor));
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
