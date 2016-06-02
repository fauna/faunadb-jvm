package com.faunadb.client.query;

import com.faunadb.client.types.Ref;
import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Methods modeling the FaunaDB query language. This class is intended to be statically imported into your code:
 *
 * {@code import static com.faunadb.client.query.Language.*;}
 *
 * Each of these methods constructs a {@link Value}, which can then be composed with other methods to form complex
 * expressions.
 *
 * <b>Examples:</b>
 * <pre>{@code
 *   Value existsValue = Exists(Ref("some/ref"));
 *
 *   Value createValue = Create(
 *    Ref("classes/some_class"),
 *    Obj("data",
 *      Obj("some", Value("field")))
 *   );
 * }</pre>
 *
 * @see <a href="https://faunadb.com/documentation/queries">FaunaDB Query API</a>
 */
public final class Language {

  private Language() {
  }

  /**
   * Enumeration for time units.
   *
   * @see <a href="https://faunadb.com/documentation/queries#time_functions">FaunaDB Time Functions</a>.
   */
  public enum TimeUnit {
    SECOND("second"),
    MILLISECOND("millisecond"),
    MICROSECOND("microsecond"),
    NANOSECOND("nanosecond");

    private final Value value;

    TimeUnit(String value) {
      this.value = new StringV(value);
    }
  }

  /**
   * Enumeration for event action types.
   *
   * @see <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public enum Action {
    CREATE("create"),
    DELETE("delete");

    private final Value value;

    Action(String value) {
      this.value = new StringV(value);
    }
  }

  /**
   * Builder for let expressions.
   *
   * @see <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static final class LetBinding {

    private final ObjectV bindings;

    private LetBinding(ImmutableMap<String, Value> bindings) {
      this.bindings = new ObjectV(bindings);
    }

    public Expr in(Value in) {
      return Fn.apply("let", bindings, "in", in);
    }

  }

  /**
   * Builder for path selectors.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static final class Path {

    private final ImmutableList<Value> segments;

    private Path() {
      this(ImmutableList.<Value>of());
    }

    private Path(ImmutableList<Value> segments) {
      this.segments = segments;
    }

    /**
     * Narrow to a specific path on a object node.
     *
     * @return a new narrowed path
     */
    public Path at(String... others) {
      ImmutableList.Builder<Value> all = ImmutableList.<Value>builder().addAll(segments);
      for (String segment : others)
        all.add(new StringV(segment));

      return new Path(all.build());
    }

    /**
     * Narrow to a specific element index on a array node.
     *
     * @return a new narrowed path
     */
    public Path at(int... others) {
      ImmutableList.Builder<Value> all = ImmutableList.<Value>builder().addAll(segments);
      for (int segment : others)
        all.add(new LongV(segment));

      return new Path(all.build());
    }

  }

  /**
   * Creates a new Ref literal value.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Ref(String ref) {
    return Expr.create(new Ref(ref));
  }

  /**
   * Calls ref function to create a ref value.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Ref(Value ref) {
    return Expr.create(new Ref(ref));
  }

  /**
   * Calls ref function to create a ref value.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Ref(Value classRef, Value id) {
    return Fn.apply("ref", classRef, "id", id);
  }

  /**
   * Creates a new Ref from a specific class ref.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Ref(Value classRef, String id) {
    return Ref(classRef, new StringV(id));
  }

  /**
   * Creates a new String value.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(String value) {
    return Expr.create(new StringV(value));
  }

  /**
   * Creates a new Long value.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(long value) {
    return Expr.create(new LongV(value));
  }

  /**
   * Creates a new Double value.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(double value) {
    return Expr.create(new DoubleV(value));
  }

  /**
   * Creates a new Boolean value.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(boolean value) {
    return Expr.create(value ? BooleanV.TRUE : BooleanV.FALSE);
  }

  /**
   * Creates a new Timestamp value.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(Instant value) {
    return Expr.create(new TsV(value));
  }

  /**
   * Creates a new Date value.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(LocalDate value) {
    return Expr.create(new DateV(value));
  }

  /**
   * Creates a null value.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Null() {
    return Expr.create(NullV.NULL);
  }

  /**
   * Creates a new Object value wrapping the provided map.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(Map<String, ? extends Value> values) {
    return Expr.create(new ObjectV(values));
  }

  /**
   * Creates an empty Object value.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj() {
    return Obj(ImmutableMap.<String, Value>of());
  }

  /**
   * Creates a new Object value with the provided entries.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Value v1) {
    return Obj(ImmutableMap.of(k1, v1));
  }

  /**
   * Creates a new Object value with the provided entries.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Value v1, String k2, Value v2) {
    return Obj(ImmutableMap.of(k1, v1, k2, v2));
  }

  /**
   * Creates a new Object value with the provided entries.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Value v1, String k2, Value v2, String k3, Value v3) {
    return Obj(ImmutableMap.of(k1, v1, k2, v2, k3, v3));
  }

  /**
   * Creates a new Object value with the provided entries.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4) {
    return Obj(ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4));
  }

  /**
   * Creates a new Object value with the provided entries.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4, String k5, Value v5) {
    return Obj(ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
  }

  /**
   * Creates a new Array value containing the provided list of values.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Arr(List<? extends Value> values) {
    return Expr.create(new ArrayV(values));
  }

  /**
   * Creates a new Array value containing the provided entries.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Arr(Value... values) {
    return Arr(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Let expression wrapping the provided map of bindings.
   *
   * @see <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static LetBinding Let(Map<String, ? extends Value> bindings) {
    return new LetBinding(ImmutableMap.copyOf(bindings));
  }

  /**
   * Creates a new Let expression with the provided bindings.
   *
   * @see <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static LetBinding Let(String v1, Value d1) {
    return Let(ImmutableMap.of(v1, d1));
  }

  /**
   * Creates a new Let expression with the provided bindings.
   *
   * @see <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static LetBinding Let(String v1, Value d1, String v2, Value d2) {
    return Let(ImmutableMap.of(v1, d1, v2, d2));
  }

  /**
   * Creates a new Let expression with the provided bindings.
   *
   * @see <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static LetBinding Let(String v1, Value d1, String v2, Value d2, String v3, Value d3) {
    return Let(ImmutableMap.of(v1, d1, v2, d2, v3, d3));
  }

  /**
   * Creates a new Let expression with the provided bindings.
   *
   * @see <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static LetBinding Let(String v1, Value d1, String v2, Value d2, String v3, Value d3, String v4, Value d4) {
    return Let(ImmutableMap.of(v1, d1, v2, d2, v3, d3, v4, d4));
  }

  /**
   * Creates a new Let expression with the provided bindings.
   *
   * @see <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static LetBinding Let(String v1, Value d1, String v2, Value d2, String v3, Value d3, String v4, Value d4, String v5, Value d5) {
    return Let(ImmutableMap.of(v1, d1, v2, d2, v3, d3, v4, d4, v5, d5));
  }

  /**
   * Creates a new Var expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Var(String name) {
    return Fn.apply("var", new StringV(name));
  }

  /**
   * Creates a new If expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr If(Value condition, Value thenExpr, Value elseExpr) {
    return Fn.apply("if", condition, "then", thenExpr, "else", elseExpr);
  }

  /**
   * Creates a new Do expression wrapping the provided list of expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Do(ImmutableList<? extends Value> exprs) {
    return Fn.apply("do", Expr.create(new ArrayV(exprs)));
  }

  /**
   * Creates a new Do expression containing the provided expressions.
   *
   * @see <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Do(Value... exprs) {
    return Do(ImmutableList.copyOf(exprs));
  }

  /**
   * Creates a new Lambda expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Lambda(Value var, Value expr) {
    return Fn.apply("lambda", var, "expr", expr);
  }

  /**
   * Creates a new Map expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Map(Value lambda, Value collection) {
    return Fn.apply("map", lambda, "collection", collection);
  }

  /**
   * Creates a new Foreach expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Foreach(Value lambda, Value collection) {
    return Fn.apply("foreach", lambda, "collection", collection);
  }

  /**
   * Creates a new Filter expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Filter(Value lambda, Value collection) {
    return Fn.apply("filter", lambda, "collection", collection);
  }

  /**
   * Creates a new Take expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Take(Value num, Value collection) {
    return Fn.apply("take", num, "collection", collection);
  }

  /**
   * Creates a new Drop expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Drop(Value num, Value collection) {
    return Fn.apply("drop", num, "collection", collection);
  }

  /**
   * Creates a new Prepend expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Prepend(Value elements, Value collection) {
    return Fn.apply("prepend", elements, "collection", collection);
  }

  /**
   * Creates a new Append expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Append(Value elements, Value collection) {
    return Fn.apply("append", elements, "collection", collection);
  }

  /**
   * Creates a new Get expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Expr Get(Value ref) {
    return Fn.apply("get", ref);
  }

  /**
   * Creates a new Paginate expression.
   *
   * @see Pagination
   * @see <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Pagination Paginate(Value resource) {
    return Pagination.paginate(resource);
  }

  /**
   * Creates a new Before cursor.
   *
   * @see Pagination.Cursor
   * @see <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Pagination.Cursor Before(Value ref) {
    return new Pagination.Before(ref);
  }

  /**
   * Creates a new After cursor.
   *
   * @see Pagination.Cursor
   * @see <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Pagination.After After(Value ref) {
    return new Pagination.After(ref);
  }

  /**
   * Creates a new Exists expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Expr Exists(Value ref) {
    return Fn.apply("exists", ref);
  }

  /**
   * Creates a new Exists expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Expr Exists(Value ref, Value timestamp) {
    return Fn.apply("exists", ref, "ts", timestamp);
  }

  /**
   * Creates a new Count expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Expr Count(Value set) {
    return Fn.apply("count", set);
  }

  /**
   * Creates a new Count expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Expr Count(Value set, Value countEvents) {
    return Fn.apply("count", set, "events", countEvents);
  }

  /**
   * Creates a new Create expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Create(Value ref, Value params) {
    return Fn.apply("create", ref, "params", params);
  }

  /**
   * Creates a new Update expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Update(Value ref, Value params) {
    return Fn.apply("update", ref, "params", params);
  }

  /**
   * Creates a new Replace expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Replace(Value ref, Value params) {
    return Fn.apply("replace", ref, "params", params);
  }

  /**
   * Creates a new Delete expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Delete(Value ref) {
    return Fn.apply("delete", ref);
  }

  /**
   * Creates a new Insert expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Insert(Value ref, Value timestamp, Value action, Value params) {
    return Fn.apply("insert", ref, "ts", timestamp, "action", action, "params", params);
  }

  /**
   * Creates a new Insert expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Insert(Value ref, Value timestamp, Action action, Value params) {
    return Insert(ref, timestamp, action.value, params);
  }

  /**
   * Creates a new Remove expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Remove(Value ref, Value timestamp, Value action) {
    return Fn.apply("remove", ref, "ts", timestamp, "action", action);
  }

  /**
   * Creates a new Remove expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Remove(Value ref, Value timestamp, Action action) {
    return Remove(ref, timestamp, action.value);
  }

  /**
   * Creates a new Match expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Match(Value index) {
    return Fn.apply("match", index);
  }

  /**
   * Creates a new Match expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Match(Value index, Value term) {
    return Fn.apply("match", index, "terms", term);
  }

  /**
   * Creates a new Union expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Union(List<? extends Value> sets) {
    return Fn.apply("union", new ArrayV(sets));
  }

  /**
   * Creates a new Union expression operating on the given sets.
   */
  public static Expr Union(Value... sets) {
    return Union(ImmutableList.copyOf(sets));
  }

  /**
   * Creates a new Intersection expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Intersection(List<? extends Value> sets) {
    return Fn.apply("intersection", new ArrayV(sets));
  }

  /**
   * Creates a new Intersection expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Intersection(Value... sets) {
    return Intersection(ImmutableList.copyOf(sets));
  }

  /**
   * Creates a new Difference expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Difference(List<? extends Value> sets) {
    return Fn.apply("difference", new ArrayV(sets));
  }

  /**
   * Creates a new Difference expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Difference(Value... sets) {
    return Difference(ImmutableList.copyOf(sets));
  }

  /**
   * Creates a new Distinct expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Distinct(Value set) {
    return Fn.apply("distinct", set);
  }

  /**
   * Creates a new Join expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Join(Value source, Value target) {
    return Fn.apply("join", source, "with", target);
  }

  /**
   * Creates a new Login expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#auth_functions">FaunaDB Authentication Functions</a>
   */
  public static Expr Login(Value ref, Value params) {
    return Fn.apply("login", ref, "params", params);
  }

  /**
   * Creates a new Logout expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#auth_functions">FaunaDB Authentication Functions</a>
   */
  public static Expr Logout(Value invalidateAll) {
    return Fn.apply("logout", invalidateAll);
  }

  /**
   * Creates a new Identify expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#auth_functions">FaunaDB Authentication Functions</a>
   */
  public static Expr Identify(Value ref, Value password) {
    return Fn.apply("identify", ref, "password", password);
  }

  /**
   * Creates a new Concat expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public static Expr Concat(Value terms) {
    return Fn.apply("concat", terms);
  }

  /**
   * Creates a new Concat expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public static Expr Concat(Value terms, Value separator) {
    return Fn.apply("concat", terms, "separator", separator);
  }

  /**
   * Creates a new Casefold expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public static Expr Casefold(Value str) {
    return Fn.apply("casefold", str);
  }

  /**
   * Creates a new Time expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#time_functions">FaunaDB Time and Date Functions</a>
   */
  public static Expr Time(Value str) {
    return Fn.apply("time", str);
  }

  /**
   * Creates a new Epoch expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#time_functions">FaunaDB Time and Date Functions</a>
   */
  public static Expr Epoch(Value num, TimeUnit unit) {
    return Epoch(num, unit.value);
  }

  /**
   * Creates a new Epoch expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#time_functions">FaunaDB Time and Date Functions</a>
   */
  public static Expr Epoch(Value num, Value unit) {
    return Fn.apply("epoch", num, "unit", unit);
  }

  /**
   * Creates a new Date expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#time_functions">FaunaDB Time and Date Functions</a>
   */
  public static Expr Date(Value str) {
    return Fn.apply("date", str);
  }

  /**
   * Creates a new NextId expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr NextId() {
    return Fn.apply("next_id", NullV.NULL);
  }

  /**
   * Creates a new Equals expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Equals(List<? extends Value> values) {
    return Fn.apply("equals", new ArrayV(values));
  }

  /**
   * Creates a new Equals expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Equals(Value... values) {
    return Equals(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Contains expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Contains(Value path, Value in) {
    return Fn.apply("contains", path, "in", in);
  }

  /**
   * Creates a new Contains expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Contains(Path path, Value in) {
    return Contains(new ArrayV(path.segments), in);
  }

  /**
   * Helper for constructing a Path with the given path terms.
   *
   * @see Path
   */
  public static Path Path(String... segments) {
    return new Path().at(segments);
  }

  /**
   * Helper for constructing a Path with the given elements indexes.
   *
   * @see Path
   */
  public static Path Path(int... segments) {
    return new Path().at(segments);
  }

  /**
   * Creates a new Select expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Select(Value path, Value from) {
    return Fn.apply("select", path, "from", from);
  }

  /**
   * Creates a new Select expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Select(Path path, Value from) {
    return Select(new ArrayV(path.segments), from);
  }

  /**
   * Creates a new Add expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Add(List<? extends Value> values) {
    return Fn.apply("add", new ArrayV(values));
  }

  /**
   * Creates a new Add expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Add(Value... values) {
    return Add(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Multiply expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Multiply(List<? extends Value> values) {
    return Fn.apply("multiply", new ArrayV(values));
  }

  /**
   * Creates a new Multiply expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Multiply(Value... values) {
    return Multiply(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Subtract expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Subtract(List<? extends Value> values) {
    return Fn.apply("subtract", new ArrayV(values));
  }

  /**
   * Creates a new Subtract expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Subtract(Value... values) {
    return Subtract(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Divide expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Divide(List<? extends Value> values) {
    return Fn.apply("divide", new ArrayV(values));
  }

  /**
   * Creates a new Divide expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Divide(Value... values) {
    return Divide(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Modulo expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Modulo(List<? extends Value> values) {
    return Fn.apply("modulo", new ArrayV(values));
  }

  /**
   * Creates a new Modulo expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Modulo(Value... values) {
    return Modulo(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new LT expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr LT(List<? extends Value> values) {
    return Fn.apply("lt", new ArrayV(values));
  }

  /**
   * Creates a new LT expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr LT(Value... values) {
    return LT(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new LTE expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr LTE(List<? extends Value> values) {
    return Fn.apply("lte", new ArrayV(values));
  }

  /**
   * Creates a new LTE expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr LTE(Value... values) {
    return LTE(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new GT expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr GT(List<? extends Value> values) {
    return Fn.apply("gt", new ArrayV(values));
  }

  /**
   * Creates a new GT expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr GT(Value... values) {
    return GT(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new GTE expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr GTE(List<? extends Value> values) {
    return Fn.apply("gte", new ArrayV(values));
  }

  /**
   * Creates a new GTE expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr GTE(Value... values) {
    return GTE(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new And expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr And(List<? extends Value> values) {
    return Fn.apply("and", new ArrayV(values));
  }

  /**
   * Creates a new And expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr And(Value... values) {
    return And(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Or expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Or(List<? extends Value> values) {
    return Fn.apply("or", new ArrayV(values));
  }

  /**
   * Creates a new Or expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Or(Value... values) {
    return Or(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Not expression.
   *
   * @see <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Not(Value bool) {
    return Fn.apply("not", bool);
  }

}
