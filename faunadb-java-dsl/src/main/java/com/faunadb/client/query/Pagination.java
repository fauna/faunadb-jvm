package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonValue;
import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.LongV;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import static com.faunadb.client.util.Objects.requireNonNull;

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
 * @see <a href="https://fauna.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
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
   * @param cursor the cursor
   * @return a new pagination with the cursor set
   */
  public Pagination before(Expr cursor) {
    this.cursor = Optional.<Cursor>of(new Before(cursor));
    return this;
  }

  /**
   * Sets the cursor of the pagination to move forward.
   *
   * @param cursor the cursor
   * @return a new pagination with the cursor set
   */
  public Pagination after(Expr cursor) {
    this.cursor = Optional.<Cursor>of(new After(cursor));
    return this;
  }

  /**
   * Sets the timestamp for the pagination
   *
   * @param ts the desired timestamp
   * @return a new pagination with the timestamp set
   */
  public Pagination ts(Expr ts) {
    this.ts = Optional.of(ts);
    return this;
  }

  /**
   * Sets the timestamp for the pagination
   *
   * @param ts the desired timestamp
   * @return a new pagination with the timestamp set
   */
  public Pagination ts(Long ts) {
    return ts(new LongV(ts));
  }

  /**
   * Sets the size of the pagination
   *
   * @param size the desired size
   * @return a new pagination with the size set
   */
  public Pagination size(Expr size) {
    this.size = Optional.of(size);
    return this;
  }

  /**
   * Sets the size of the pagination
   *
   * @param size the desired size
   * @return a new pagination with the size set
   */
  public Pagination size(Integer size) {
    return size(new LongV(size));
  }

  /**
   * Define if the pagination should include sources of not
   *
   * @return a new pagination with sources option set
   */
  public Pagination sources(Expr sources) {
    this.sources = Optional.of(sources);
    return this;
  }

  /**
   * Define if the pagination should include sources of not
   *
   * @return a new pagination with sources option set
   */
  public Pagination sources(boolean sources) {
    if (!sources) return this;
    return sources(Value.BooleanV.TRUE);
  }

  /**
   * Define if the pagination should include events of not
   *
   * @return a new pagination with events option set
   */
  public Pagination events(Expr events) {
    this.events = Optional.of(events);
    return this;
  }

  /**
   * Define if the pagination should include events of not
   *
   * @return a new pagination with events option set
   */
  public Pagination events(boolean events) {
    if (!events) return this;
    return events(Value.BooleanV.TRUE);
  }

}
