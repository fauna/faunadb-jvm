package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonValue;
import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.LongV;
import com.faunadb.client.types.Value.ObjectV;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import static java.util.Objects.requireNonNull;

/**
 * A pagination expression.
 * <p>
 * <b>Example:</b>
 * We can paginate all instances of a class, with its evets, in pages with 4 elements only.
 * <pre>{@code
 * client.query(
 *    Paginate(Match(Ref("indexes/all_spells"))
 *      .withEvents(true)
 *      .withSize(4)
 * );
 * }</pre>
 *
 * @see Language#Paginate(Expr)
 * @see <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
 */
public final class Pagination extends Expr {

  /**
   * Helper to construct the pagination cursor that can constructed either
   * by {@link Language#Before(Expr)} or {@link Language#After(Expr)}
   *
   * @see <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public abstract static class Cursor {
    private final String name;
    private final Expr ref;

    Cursor(String name, Expr ref) {
      this.name = requireNonNull(name);
      this.ref = requireNonNull(ref);
    }
  }

  static class Before extends Cursor {
    Before(Expr ref) {
      super("before", ref);
    }
  }

  static class After extends Cursor {
    After(Expr ref) {
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
    this.resource = requireNonNull(resource);
  }

  @Override
  @JsonValue
  protected Value value() {
    ImmutableMap.Builder<String, Value> res = ImmutableMap.builder();
    res.put("paginate", resource.value());

    if (cursor.isPresent()) res.put(cursor.get().name, cursor.get().ref.value());
    if (events.isPresent()) res.put("events", events.get().value());
    if (sources.isPresent()) res.put("sources", sources.get().value());
    if (ts.isPresent()) res.put("ts", ts.get().value());
    if (size.isPresent()) res.put("size", size.get().value());

    return new ObjectV(res.build());
  }

  /**
   * Sets the cursor for the pagination.
   *
   * @param cursor the cursor
   * @return a new pagination with the cursor set
   * @see Cursor
   */
  public Pagination withCursor(Cursor cursor) {
    this.cursor = Optional.of(cursor);
    return this;
  }

  /**
   * Sets the timestamp for the pagination
   *
   * @param ts the desired timestamp
   * @return a new pagination with the timestamp set
   */
  public Pagination withTs(Expr ts) {
    this.ts = Optional.of(ts);
    return this;
  }

  /**
   * Sets the timestamp for the pagination
   *
   * @param ts the desired timestamp
   * @return a new pagination with the timestamp set
   */
  public Pagination withTs(Long ts) {
    return withTs(new LongV(ts));
  }

  /**
   * Sets the size of the pagination
   *
   * @param size the desired size
   * @return a new pagination with the size set
   */
  public Pagination withSize(Expr size) {
    this.size = Optional.of(size);
    return this;
  }

  /**
   * Sets the size of the pagination
   *
   * @param size the desired size
   * @return a new pagination with the size set
   */
  public Pagination withSize(Integer size) {
    return withSize(new LongV(size));
  }

  /**
   * Define if the pagination should include sources of not
   *
   * @return a new pagination with sources option set
   */
  public Pagination withSources(Expr sources) {
    this.sources = Optional.of(sources);
    return this;
  }

  /**
   * Define if the pagination should include sources of not
   *
   * @return a new pagination with sources option set
   */
  public Pagination withSources(boolean sources) {
    if (!sources) return this;
    return withSources(Value.BooleanV.TRUE);
  }

  /**
   * Define if the pagination should include events of not
   *
   * @return a new pagination with events option set
   */
  public Pagination withEvents(Expr events) {
    this.events = Optional.of(events);
    return this;
  }

  /**
   * Define if the pagination should include events of not
   *
   * @return a new pagination with events option set
   */
  public Pagination withEvents(boolean events) {
    if (!events) return this;
    return withEvents(Value.BooleanV.TRUE);
  }

}
