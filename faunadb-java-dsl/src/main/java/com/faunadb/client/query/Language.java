package com.faunadb.client.query;

import com.faunadb.client.types.Encoder;
import com.faunadb.client.types.Value.*;
import com.faunadb.client.types.time.HighPrecisionTime;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.Instant;
import org.joda.time.LocalDate;

import java.util.List;
import java.util.Map;

/**
 * Methods modeling the FaunaDB query language. This class is intended to be statically imported into your code:
 *
 * {@code import static com.faunadb.client.query.Language.*;}
 *
 * Each of these methods constructs a {@link Expr}, which can then be composed with other methods to form complex
 * expressions.
 *
 * <b>Examples:</b>
 * <pre>{@code
 *   Expr existsValue = Exists(Ref(Class("some_class"), "ref"));
 *
 *   Expr createValue = Create(
 *    Class("some_class"),
 *    Obj("data",
 *      Obj("some", Value("field")))
 *   );
 * }</pre>
 *
 * @see <a href="https://fauna.com/documentation/queries">FaunaDB Query API</a>
 */
public final class Language {

  private Language() {
  }

  private static Expr varargs(List<? extends Expr> exprs) {
    if (exprs.size() == 1) {
      return exprs.get(0);
    } else {
      return Arr(exprs);
    }
  }

  /**
   * Enumeration for time units.
   *
   * @see <a href="https://fauna.com/documentation/queries#time_functions">FaunaDB Time Functions</a>
   */
  public enum TimeUnit {
    SECOND("second"),
    MILLISECOND("millisecond"),
    MICROSECOND("microsecond"),
    NANOSECOND("nanosecond");

    private final Expr value;

    TimeUnit(String value) {
      this.value = Value(value);
    }
  }

  /**
   * Enumeration for event action types.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public enum Action {
    CREATE("create"),
    DELETE("delete");

    private final Expr value;

    Action(String value) {
      this.value = Value(value);
    }
  }

  /**
   * Enumeration for casefold operations.
   *
   * @see <a href="https://fauna.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public enum Normalizer {
    NFD("NFD"),
    NFC("NFC"),
    NFKD("NFKD"),
    NFKC("NFKC"),
    NFKCCaseFold("NFKCCaseFold");

    private final Expr value;

    Normalizer(String value) {
      this.value = Value(value);
    }
  }

  /**
   * Builder for let expressions.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static final class LetBinding {

    private final Expr bindings;

    private LetBinding(ImmutableMap<String, Expr> bindings) {
      this.bindings = Fn.apply(bindings);
    }

    public Expr in(Expr in) {
      return Fn.apply("let", bindings, "in", in);
    }

  }

  /**
   * Builder for path selectors.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static final class Path {

    private final ImmutableList<Expr> segments;

    private Path() {
      this(ImmutableList.<Expr>of());
    }

    private Path(ImmutableList<Expr> segments) {
      this.segments = segments;
    }

    /**
     * Narrow to a specific path on a object node.
     *
     * @return a new narrowed path
     */
    public Path at(String... others) {
      ImmutableList.Builder<Expr> all = ImmutableList.<Expr>builder().addAll(segments);
      for (String segment : others)
        all.add(Value(segment));

      return new Path(all.build());
    }

    /**
     * Narrow to a specific element index on a array node.
     *
     * @return a new narrowed path
     */
    public Path at(int... others) {
      ImmutableList.Builder<Expr> all = ImmutableList.<Expr>builder().addAll(segments);
      for (int segment : others)
        all.add(Value(segment));

      return new Path(all.build());
    }

  }

  /**
   * Creates a RefV value. The string "classes/widget/123" will be equivalent to:
   *
   * <pre>
   * {@code new RefV("123", new RefV("widget", Native.CLASSES)) }
   * </pre>
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Ref(String ref) {
    return Fn.apply("@ref", Value(ref));
  }

  /**
   * Calls ref function to create a ref value.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Ref(Expr classRef, Expr id) {
    return Fn.apply("ref", classRef, "id", id);
  }

  /**
   * Calls ref function to create a ref value.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Ref(Expr classRef, String id) {
    return Ref(classRef, Value(id));
  }

  /**
   * Returns a native reference to all classes.
   */
  public static Expr Classes() {
    return Classes(Null());
  }

  /**
   * Returns a native reference to all classes in a nested database.
   */
  public static Expr Classes(Expr scope) {
    return Fn.apply("classes", scope);
  }

  /**
   * Returns a native reference to all databases.
   */
  public static Expr Databases() {
    return Databases(Null());
  }

  /**
   * Returns a native reference to all databases in a nested database.
   */
  public static Expr Databases(Expr scope) {
    return Fn.apply("databases", scope);
  }

  /**
   * Returns a native reference to all indexes.
   */
  public static Expr Indexes() {
    return Indexes(Null());
  }

  /**
   * Returns a native reference to all indexes in a nested database.
   */
  public static Expr Indexes(Expr scope) {
    return Fn.apply("indexes", scope);
  }

  /**
   * Returns a native reference to all functions.
   */
  public static Expr Functions() {
    return Functions(Null());
  }

  /**
   * Returns a native reference to all functions in a nested database.
   */
  public static Expr Functions(Expr scope) {
    return Fn.apply("functions", scope);
  }

  /**
   * Returns a native reference to all keys.
   */
  public static Expr Keys() {
    return Keys(Null());
  }

  /**
   * Returns a native reference to all keys in a nested database.
   */
  public static Expr Keys(Expr scope) {
    return Fn.apply("keys", scope);
  }

  /**
   * Returns a native reference to all tokens.
   */
  public static Expr Tokens() {
    return Tokens(Null());
  }

  /**
   * Returns a native reference to all tokens in a nested database.
   */
  public static Expr Tokens(Expr scope) {
    return Fn.apply("tokens", scope);
  }

  /**
   * Returns a native reference to all credentials.
   */
  public static Expr Credentials() {
    return Credentials(Null());
  }

  /**
   * Returns a native reference to all credentials in a nested database.
   */
  public static Expr Credentials(Expr scope) {
    return Fn.apply("credentials", scope);
  }

  /**
   * Encode the given object using {@link Encoder}
   */
  public static Expr Value(Object value) {
    return Encoder.encode(value).get();
  }

  /**
   * Creates a new String value.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(String value) {
    return new StringV(value);
  }

  /**
   * Creates a new Long value.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(long value) {
    return new LongV(value);
  }

  /**
   * Creates a new Double value.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(double value) {
    return new DoubleV(value);
  }

  /**
   * Creates a new Boolean value.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(boolean value) {
    return BooleanV.valueOf(value);
  }

  /**
   * Creates a new Timestamp value.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(Instant value) {
    return new TimeV(HighPrecisionTime.fromInstant(value));
  }

  /**
   * Creates a new Timestamp value from a high precision time.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   * @see HighPrecisionTime
   */
  public static Expr Value(HighPrecisionTime value) {
    return new TimeV(value);
  }

  /**
   * Creates a new Date value.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(LocalDate value) {
    return new DateV(value);
  }

  /**
   * Creates a new Bytes value.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(byte[] bytes) {
    return new BytesV(bytes);
  }

  /**
   * Creates a null value.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Null() {
    return NullV.NULL;
  }

  /**
   * Creates a new Object value wrapping the provided map.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(Map<String, ? extends Expr> values) {
    return Fn.apply("object", Fn.apply(values));
  }

  /**
   * Creates an empty Object value.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj() {
    return Obj(ImmutableMap.<String, Expr>of());
  }

  /**
   * Creates a new Object value with the provided entries.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Expr v1) {
    return Obj(ImmutableMap.of(k1, v1));
  }

  /**
   * Creates a new Object value with the provided entries.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Expr v1, String k2, Expr v2) {
    return Obj(ImmutableMap.of(k1, v1, k2, v2));
  }

  /**
   * Creates a new Object value with the provided entries.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Expr v1, String k2, Expr v2, String k3, Expr v3) {
    return Obj(ImmutableMap.of(k1, v1, k2, v2, k3, v3));
  }

  /**
   * Creates a new Object value with the provided entries.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Expr v1, String k2, Expr v2, String k3, Expr v3, String k4, Expr v4) {
    return Obj(ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4));
  }

  /**
   * Creates a new Object value with the provided entries.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Expr v1, String k2, Expr v2, String k3, Expr v3, String k4, Expr v4, String k5, Expr v5) {
    return Obj(ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
  }

  /**
   * Creates a new Array value containing the provided list of values.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Arr(List<? extends Expr> values) {
    return Fn.apply(values);
  }

  /**
   * Creates a new Array value containing the provided entries.
   *
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Arr(Expr... values) {
    return Arr(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Abort expression
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Abort(String msg) {
    return Abort(Value(msg));
  }

  /**
   * Creates a new Abort expression
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Abort(Expr msg) {
    return Fn.apply("abort", msg);
  }

  /**
   * Creates a new Call expression
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Call(Expr ref, List<? extends  Expr> args) {
    return Fn.apply("call", ref, "arguments", varargs(args));
  }

  /**
   * Creates a new Call expression
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Call(Expr ref, Expr... args) {
    return Call(ref, ImmutableList.copyOf(args));
  }

  /**
   * Creates a new Query expression
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Query(Expr lambda) {
    return Fn.apply("query", lambda);
  }

  /**
   * Creates a new At expression
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr At(Expr timestamp, Expr expr) {
    return Fn.apply("at", timestamp, "expr", expr);
  }

  /**
   * Creates a new Let expression wrapping the provided map of bindings.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static LetBinding Let(Map<String, ? extends Expr> bindings) {
    return new LetBinding(ImmutableMap.copyOf(bindings));
  }

  /**
   * Creates a new Let expression with the provided bindings.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static LetBinding Let(String v1, Expr d1) {
    return Let(ImmutableMap.of(v1, d1));
  }

  /**
   * Creates a new Let expression with the provided bindings.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static LetBinding Let(String v1, Expr d1, String v2, Expr d2) {
    return Let(ImmutableMap.of(v1, d1, v2, d2));
  }

  /**
   * Creates a new Let expression with the provided bindings.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static LetBinding Let(String v1, Expr d1, String v2, Expr d2, String v3, Expr d3) {
    return Let(ImmutableMap.of(v1, d1, v2, d2, v3, d3));
  }

  /**
   * Creates a new Let expression with the provided bindings.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static LetBinding Let(String v1, Expr d1, String v2, Expr d2, String v3, Expr d3, String v4, Expr d4) {
    return Let(ImmutableMap.of(v1, d1, v2, d2, v3, d3, v4, d4));
  }

  /**
   * Creates a new Let expression with the provided bindings.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static LetBinding Let(String v1, Expr d1, String v2, Expr d2, String v3, Expr d3, String v4, Expr d4, String v5, Expr d5) {
    return Let(ImmutableMap.of(v1, d1, v2, d2, v3, d3, v4, d4, v5, d5));
  }

  /**
   * Creates a new Var expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Var(String name) {
    return Fn.apply("var", Value(name));
  }

  /**
   * Creates a new If expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr If(Expr condition, Expr thenExpr, Expr elseExpr) {
    return Fn.apply("if", condition, "then", thenExpr, "else", elseExpr);
  }

  /**
   * Creates a new Do expression wrapping the provided list of expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Do(List<? extends Expr> exprs) {
    return Fn.apply("do", varargs(exprs));
  }

  /**
   * Creates a new Do expression containing the provided expressions.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Do(Expr... exprs) {
    return Do(ImmutableList.copyOf(exprs));
  }

  /**
   * Creates a new Lambda expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Expr Lambda(Expr var, Expr expr) {
    return Fn.apply("lambda", var, "expr", expr);
  }

  /**
   * Creates a new Map expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Map(Expr collection, Expr lambda) {
    return Fn.apply("map", lambda, "collection", collection);
  }

  /**
   * Creates a new Foreach expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Foreach(Expr collection, Expr lambda) {
    return Fn.apply("foreach", lambda, "collection", collection);
  }

  /**
   * Creates a new Filter expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Filter(Expr collection, Expr lambda) {
    return Fn.apply("filter", lambda, "collection", collection);
  }

  /**
   * Creates a new Take expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Take(Expr num, Expr collection) {
    return Fn.apply("take", num, "collection", collection);
  }

  /**
   * Creates a new Drop expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Drop(Expr num, Expr collection) {
    return Fn.apply("drop", num, "collection", collection);
  }

  /**
   * Creates a new Prepend expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Prepend(Expr elements, Expr collection) {
    return Fn.apply("prepend", elements, "collection", collection);
  }

  /**
   * Creates a new Append expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Expr Append(Expr elements, Expr collection) {
    return Fn.apply("append", elements, "collection", collection);
  }

  /**
   * Creates a new Get expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Expr Get(Expr ref) {
    return Fn.apply("get", ref);
  }

  /**
   * Creates a new Get expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Expr Get(Expr ref, Expr timestamp) {
    return Fn.apply("get", ref, "ts", timestamp);
  }

  /**
   * Creates a new KeyFromSecret expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Expr KeyFromSecret(Expr secret) {
    return Fn.apply("key_from_secret", secret);
  }

  /**
   * Creates a new Paginate expression.
   *
   * @see Pagination
   * @see <a href="https://fauna.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Pagination Paginate(Expr resource) {
    return new Pagination(resource);
  }

  /**
   * Creates a new Exists expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Expr Exists(Expr ref) {
    return Fn.apply("exists", ref);
  }

  /**
   * Creates a new Exists expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#read_functions">FaunaDB Read Functions</a>
   */
  public static Expr Exists(Expr ref, Expr timestamp) {
    return Fn.apply("exists", ref, "ts", timestamp);
  }

  /**
   * Creates a new Create expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Create(Expr ref, Expr params) {
    return Fn.apply("create", ref, "params", params);
  }

  /**
   * Creates a new Update expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Update(Expr ref, Expr params) {
    return Fn.apply("update", ref, "params", params);
  }

  /**
   * Creates a new Replace expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Replace(Expr ref, Expr params) {
    return Fn.apply("replace", ref, "params", params);
  }

  /**
   * Creates a new Delete expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Delete(Expr ref) {
    return Fn.apply("delete", ref);
  }

  /**
   * Creates a new Insert expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Insert(Expr ref, Expr timestamp, Expr action, Expr params) {
    return Fn.apply("insert", ref, "ts", timestamp, "action", action, "params", params);
  }

  /**
   * Creates a new Insert expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Insert(Expr ref, Expr timestamp, Action action, Expr params) {
    return Insert(ref, timestamp, action.value, params);
  }

  /**
   * Creates a new Remove expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Remove(Expr ref, Expr timestamp, Expr action) {
    return Fn.apply("remove", ref, "ts", timestamp, "action", action);
  }

  /**
   * Creates a new Remove expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr Remove(Expr ref, Expr timestamp, Action action) {
    return Remove(ref, timestamp, action.value);
  }

  /**
   * Creates a new Create Class expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr CreateClass(Expr params) {
    return Fn.apply("create_class", params);
  }

  /**
   * Creates a new Create Database expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr CreateDatabase(Expr params) {
    return Fn.apply("create_database", params);
  }

  /**
   * Creates a new Create Key expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr CreateKey(Expr params) {
    return Fn.apply("create_key", params);
  }

  /**
   * Creates a new Create Index expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr CreateIndex(Expr params) {
    return Fn.apply("create_index", params);
  }

  /**
   * Creates a new Create Function expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#write_functions">FaunaDB Write Functions</a>
   */
  public static Expr CreateFunction(Expr params) {
    return Fn.apply("create_function", params);
  }

  /**
   * Create a new Singleton expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Singleton(Expr ref) {
    return Fn.apply("singleton", ref);
  }

  /**
   * Create a new Events expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Events(Expr refSet) {
    return Fn.apply("events", refSet);
  }

  /**
   * Creates a new Match expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Match(Expr index) {
    return Fn.apply("match", index);
  }

  /**
   * Creates a new Match expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Match(Expr index, Expr term) {
    return Fn.apply("match", index, "terms", term);
  }

  /**
   * Creates a new Union expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Union(List<? extends Expr> sets) {
    return Fn.apply("union", varargs(sets));
  }

  /**
   * Creates a new Union expression operating on the given sets.
   */
  public static Expr Union(Expr... sets) {
    return Union(ImmutableList.copyOf(sets));
  }

  /**
   * Creates a new Intersection expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Intersection(List<? extends Expr> sets) {
    return Fn.apply("intersection", varargs(sets));
  }

  /**
   * Creates a new Intersection expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Intersection(Expr... sets) {
    return Intersection(ImmutableList.copyOf(sets));
  }

  /**
   * Creates a new Difference expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Difference(List<? extends Expr> sets) {
    return Fn.apply("difference", varargs(sets));
  }

  /**
   * Creates a new Difference expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Difference(Expr... sets) {
    return Difference(ImmutableList.copyOf(sets));
  }

  /**
   * Creates a new Distinct expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Distinct(Expr set) {
    return Fn.apply("distinct", set);
  }

  /**
   * Creates a new Join expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   */
  public static Expr Join(Expr source, Expr target) {
    return Fn.apply("join", source, "with", target);
  }

  /**
   * Creates a new Login expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#auth_functions">FaunaDB Authentication Functions</a>
   */
  public static Expr Login(Expr ref, Expr params) {
    return Fn.apply("login", ref, "params", params);
  }

  /**
   * Creates a new Logout expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#auth_functions">FaunaDB Authentication Functions</a>
   */
  public static Expr Logout(Expr invalidateAll) {
    return Fn.apply("logout", invalidateAll);
  }

  /**
   * Creates a new Identify expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#auth_functions">FaunaDB Authentication Functions</a>
   */
  public static Expr Identify(Expr ref, Expr password) {
    return Fn.apply("identify", ref, "password", password);
  }

  /**
   * Creates a new Identity expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#auth_functions">FaunaDB Authentication Functions</a>
   */
  public static Expr Identity() {
    return Fn.apply("identity", Null());
  }

  /**
   * Creates a new HasIdentity expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#auth_functions">FaunaDB Authentication Functions</a>
   */
  public static Expr HasIdentity() {
    return Fn.apply("has_identity", Null());
  }

  /**
   * Creates a new Concat expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public static Expr Concat(Expr terms) {
    return Fn.apply("concat", terms);
  }

  /**
   * Creates a new Concat expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public static Expr Concat(Expr terms, Expr separator) {
    return Fn.apply("concat", terms, "separator", separator);
  }

  /**
   * Creates a new Casefold expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public static Expr Casefold(Expr str) {
    return Fn.apply("casefold", str);
  }

  /**
   * Creates a new Casefold expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public static Expr Casefold(Expr str, Expr normalizer) {
    return Fn.apply("casefold", str, "normalizer", normalizer);
  }

  /**
   * Creates a new Casefold expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public static Expr Casefold(Expr str, Normalizer normalizer) {
    return Casefold(str, normalizer.value);
  }

  /**
   * Creates a new NGram expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public static Expr NGram(Expr terms, Expr min, Expr max) {
    return Fn.apply("ngram", terms, "min", min, "max", max);
  }

  /**
   * Creates a new NGram expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public static Expr NGram(List<Expr> terms, Expr min, Expr max) {
    return NGram(varargs(terms), min, max);
  }

  /**
   * Creates a new NGram expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public static Expr NGram(List<Expr> terms) {
    return NGram(varargs(terms));
  }

  /**
   * Creates a new NGram expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#string_functions">FaunaDB String Functions</a>
   */
  public static Expr NGram(Expr terms) {
    return Fn.apply("ngram", terms);
  }

  /**
   * Creates a new Time expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#time_functions">FaunaDB Time and Date Functions</a>
   */
  public static Expr Time(Expr str) {
    return Fn.apply("time", str);
  }

  /**
   * Creates a new Epoch expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#time_functions">FaunaDB Time and Date Functions</a>
   */
  public static Expr Epoch(Expr num, TimeUnit unit) {
    return Epoch(num, unit.value);
  }

  /**
   * Creates a new Epoch expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#time_functions">FaunaDB Time and Date Functions</a>
   */
  public static Expr Epoch(Expr num, Expr unit) {
    return Fn.apply("epoch", num, "unit", unit);
  }

  /**
   * Creates a new Date expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#time_functions">FaunaDB Time and Date Functions</a>
   */
  public static Expr Date(Expr str) {
    return Fn.apply("date", str);
  }

  /**
   * Creates a new NextId expression.
   *
   * @deprecated Use NewId() instead.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  @Deprecated
  public static Expr NextId() {
    return Fn.apply("next_id", NullV.NULL);
  }

  /**
   * Creates a new NewId expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr NewId() {
    return Fn.apply("new_id", NullV.NULL);
  }

  /**
   * Creates a new Class expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Class(Expr name) {
    return Fn.apply("class", name);
  }

  /**
   * Creates a new Class expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Class(String name) {
    return Class(Value(name));
  }

  /**
   * Creates a new Class expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Class(Expr name, Expr database) {
    return Fn.apply("class", name, "scope", database);
  }

  /**
   * Creates a new Class expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Class(String name, Expr database) {
    return Class(Value(name), database);
  }

  /**
   * Creates a new Database expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Database(Expr name) {
    return Fn.apply("database", name);
  }

  /**
   * Creates a new Database expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Database(String name) {
    return Database(Value(name));
  }

  /**
   * Creates a new Database expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Database(Expr name, Expr database) {
      return Fn.apply("database", name, "scope", database);
  }

  /**
   * Creates a new Database expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Database(String name, Expr database) {
    return Database(Value(name), database);
  }

  /**
   * Creates a new Index expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Index(Expr name) {
    return Fn.apply("index", name);
  }

  /**
   * Creates a new Index expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Index(String name) {
    return Index(Value(name));
  }

  /**
   * Creates a new Index expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Index(Expr name, Expr database) {
    return Fn.apply("index", name, "scope", database);
  }

  /**
   * Creates a new Index expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Index(String name, Expr database) {
    return Index(Value(name), database);
  }

  /**
   * Creates a new Function expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Function(Expr name) {
    return Fn.apply("function", name);
  }

  /**
   * Creates a new Function expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Function(String name) {
    return Function(Value(name));
  }

  /**
   * Creates a new Function expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Function(Expr name, Expr database) {
    return Fn.apply("function", name, "scope", database);
  }

  /**
   * Creates a new Function expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Function(String name, Expr database) {
    return Function(Value(name), database);
  }

  /**
   * Creates a new Equals expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Equals(List<? extends Expr> values) {
    return Fn.apply("equals", varargs(values));
  }

  /**
   * Creates a new Equals expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Equals(Expr... values) {
    return Equals(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Contains expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Contains(Expr path, Expr in) {
    return Fn.apply("contains", path, "in", in);
  }

  /**
   * Creates a new Contains expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Contains(Path path, Expr in) {
    return Contains(Arr(path.segments), in);
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
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Select(Expr path, Expr from) {
    return Fn.apply("select", path, "from", from);
  }

  /**
   * Creates a new Select expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Select(Expr path, Expr from, Expr defaultValue) {
    return Fn.apply("select", path, "from", from, "default", defaultValue);
  }

  /**
   * Creates a new Select expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Select(Path path, Expr from) {
    return Select(Arr(path.segments), from);
  }

  /**
   * Creates a new Select expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Select(Path path, Expr from, Expr defaultValue) {
    return Select(Arr(path.segments), from, defaultValue);
  }

  /**
   * Creates a new SelectAll expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr SelectAll(Expr path, Expr from) {
    return Fn.apply("select_all", path, "from", from);
  }

  /**
   * Creates a new SelectAll expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr SelectAll(Path path, Expr from) {
    return SelectAll(Arr(path.segments), from);
  }

  /**
   * Creates a new Add expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Add(List<? extends Expr> values) {
    return Fn.apply("add", varargs(values));
  }

  /**
   * Creates a new Add expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Add(Expr... values) {
    return Add(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Multiply expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Multiply(List<? extends Expr> values) {
    return Fn.apply("multiply", varargs(values));
  }

  /**
   * Creates a new Multiply expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Multiply(Expr... values) {
    return Multiply(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Subtract expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Subtract(List<? extends Expr> values) {
    return Fn.apply("subtract", varargs(values));
  }

  /**
   * Creates a new Subtract expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Subtract(Expr... values) {
    return Subtract(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Divide expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Divide(List<? extends Expr> values) {
    return Fn.apply("divide", varargs(values));
  }

  /**
   * Creates a new Divide expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Divide(Expr... values) {
    return Divide(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Modulo expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Modulo(List<? extends Expr> values) {
    return Fn.apply("modulo", varargs(values));
  }

  /**
   * Creates a new Modulo expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Modulo(Expr... values) {
    return Modulo(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new LT expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr LT(List<? extends Expr> values) {
    return Fn.apply("lt", varargs(values));
  }

  /**
   * Creates a new LT expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr LT(Expr... values) {
    return LT(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new LTE expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr LTE(List<? extends Expr> values) {
    return Fn.apply("lte", varargs(values));
  }

  /**
   * Creates a new LTE expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr LTE(Expr... values) {
    return LTE(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new GT expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr GT(List<? extends Expr> values) {
    return Fn.apply("gt", varargs(values));
  }

  /**
   * Creates a new GT expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr GT(Expr... values) {
    return GT(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new GTE expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr GTE(List<? extends Expr> values) {
    return Fn.apply("gte", varargs(values));
  }

  /**
   * Creates a new GTE expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr GTE(Expr... values) {
    return GTE(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new And expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr And(List<? extends Expr> values) {
    return Fn.apply("and", varargs(values));
  }

  /**
   * Creates a new And expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr And(Expr... values) {
    return And(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Or expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Or(List<? extends Expr> values) {
    return Fn.apply("or", varargs(values));
  }

  /**
   * Creates a new Or expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Or(Expr... values) {
    return Or(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Not expression.
   *
   * @see <a href="https://fauna.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Not(Expr bool) {
    return Fn.apply("not", bool);
  }

}
