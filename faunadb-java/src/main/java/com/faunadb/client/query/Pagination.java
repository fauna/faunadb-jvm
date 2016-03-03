package com.faunadb.client.query;

import com.faunadb.client.types.Value;
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
 * @see Language#Paginate(Value)
 * @see <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
 */
public final class Pagination extends Fn {

  /**
   * Helper to construct the pagination cursor that can constructed either
   * by {@link Language#Before(Value)} or {@link Language#After(Value)}
   *
   * @see <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public abstract static class Cursor {
    private final String name;
    private final Value ref;

    Cursor(String name, Value ref) {
      this.name = requireNonNull(name);
      this.ref = requireNonNull(ref);
    }
  }

  static class Before extends Cursor {
    Before(Value ref) {
      super("before", ref);
    }
  }

  static class After extends Cursor {
    After(Value ref) {
      super("after", ref);
    }
  }

  static Pagination paginate(Value resource) {
    return new Pagination(new Fn.Call(
      ImmutableMap.of("paginate", resource)
    ));
  }

  private final Fn.Call call;

  private Pagination(Fn.Call call) {
    super(call);
    this.call = call;
  }

  /**
   * Sets the cursor for the pagination.
   *
   * @param cursor the cursor
   * @return a new pagination with the cursor set
   * @see Cursor
   */
  public Pagination withCursor(Cursor cursor) {
    return new Pagination(with(cursor.name, cursor.ref));
  }

  /**
   * Sets the timestamp for the pagination
   *
   * @param ts the desired timestamp
   * @return a new pagination with the timestamp set
   */
  public Pagination withTs(Value ts) {
    return new Pagination(with("ts", ts));
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
  public Pagination withSize(Value size) {
    return new Pagination(with("size", size));
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
  public Pagination withSources(Value sources) {
    return new Pagination(with("sources", sources));
  }

  /**
   * Define if the pagination should include sources of not
   *
   * @return a new pagination with sources option set
   */
  public Pagination withSources(boolean sources) {
    if (!sources) return this;
    return withSources(BooleanV.TRUE);
  }

  /**
   * Define if the pagination should include events of not
   *
   * @return a new pagination with events option set
   */
  public Pagination withEvents(Value events) {
    return new Pagination(with("events", events));
  }

  /**
   * Define if the pagination should include events of not
   *
   * @return a new pagination with events option set
   */
  public Pagination withEvents(boolean events) {
    if (!events) return this;
    return withEvents(BooleanV.TRUE);
  }

  private Fn.Call with(String key, Value value) {
    ImmutableMap.Builder<String, Value> page = ImmutableMap.builder();
    page.putAll(call.body().asObject());
    page.put(key, value);

    return new Fn.Call(page.build());
  }

}
