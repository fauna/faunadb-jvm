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
 * The {@link Language} class contains static constructors for the FaunaDB Query Language expressions.
 * This class is intended to be statically imported into your code with:
 *
 * {@code import static com.faunadb.client.query.Language.*;}
 *
 * <p>
 * Each method in the {@link Language} class constructs a new {@link Expr} instance.
 * No computation is executed until the expression is evaluated by the FaunaDB server.
 * </p>
 *
 * <b>Example:</b>
 * <pre>{@code
 *   // Creates a new expression that, once executed, it will create a new instance
 *   // of the user class with an username and password fields.
 *   Expr createUserExpr = Create(Class("user"), Obj("data", Obj(
 *     "username", Value("bob"),
 *     "password", Value("abc123"),
 *   )));
 *
 *   // Executes the expression created above and get its result.
 *   Value result = client.query(createUserExpr).get();
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
   * @see <a href="https://fauna.com/documentation/queries#time-and-date">FaunaDB Time Functions</a>
   * @see #Epoch(Expr, TimeUnit)
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
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see #Insert(Expr, Expr, Action, Expr)
   * @see #Remove(Expr, Expr, Action)
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
   * Enumeration for casefold normalizers.
   *
   * @see <a href="https://fauna.com/documentation/queries#string-functions">FaunaDB String Functions</a>
   * @see #Casefold(Expr, Normalizer)
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
   * To complete the let binding, the {@link LetBinding#in(Expr)} method must be called.
   *
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   * @see #Let(Map)
   * @see #Let(String, Expr)
   */
  public static final class LetBinding {

    private final Expr bindings;

    private LetBinding(ImmutableMap<String, Expr> bindings) {
      this.bindings = Fn.apply(bindings);
    }

    /**
     * Defines the scope where the let bindings will apply.
     * @param in the scope where the let bindings apply
     * @return a new {@link Expr} instance
     */
    public Expr in(Expr in) {
      return Fn.apply("let", bindings, "in", in);
    }
  }

  /**
   * Builder for path selectors. This builder must be constructed using
   * either the {@link #Path(String...)} or {@link #Path(int...)} functions.
   *
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   * @see #Contains(Path, Expr)
   * @see #Select(Path, Expr)
   * @see #Select(Path, Expr, Expr)
   * @see #SelectAll(Path, Expr)
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
     * Narrow to a specific path in a object key.
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
     * Narrow to a specific element index in an array.
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
   * Creates a new reference.
   * For example {@code Ref("classes/users/123")}.
   *
   * <p>
   * The usage of this method is discouraged.
   * Prefer {@link #Ref(Expr, String)} or {@link #Ref(Expr, Expr)}.
   * </p>
   *
   * @param ref the reference id
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   * @see #Ref(Expr, String)
   */
  public static Expr Ref(String ref) {
    return Fn.apply("@ref", Value(ref));
  }

  /**
   * Creates a new scoped reference.
   * For example {@code Ref(Class("users"), "123")}.
   *
   * @param classRef the scope reference. Type: Reference
   * @param id the reference id. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   * @see #Class(String)
   * @see #Database(String)
   * @see #Index(String)
   * @see #Function(String)
   */
  public static Expr Ref(Expr classRef, Expr id) {
    return Fn.apply("ref", classRef, "id", id);
  }

  /**
   * Creates a new scoped reference.
   * For example {@code Ref(Class("users"), "123")}.
   *
   * @param classRef the scope reference. Type: Reference
   * @param id the reference id
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   * @see #Class(String)
   * @see #Database(String)
   * @see #Index(String)
   * @see #Function(String)
   */
  public static Expr Ref(Expr classRef, String id) {
    return Ref(classRef, Value(id));
  }

  /**
   * Returns a reference to a set of all classes in the database.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @return a new {@link Expr} instance
   * @see #Paginate(Expr) 
   */
  public static Expr Classes() {
    return Classes(Null());
  }

  /**
   * Returns a reference to a set of all classes in the specified database.
   * A reference set must be paginated in order to retrieve its values.
   * 
   * @param scope a reference to a database. Type: Reference
   * @return a new {@link Expr} instance
   * @see #Database(String)
   * @see #Paginate(Expr)
   */
  public static Expr Classes(Expr scope) {
    return Fn.apply("classes", scope);
  }

  /**
   * Returns a reference to a set of all databases.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @return a new {@link Expr} instance
   * @see #Paginate(Expr)
   */
  public static Expr Databases() {
    return Databases(Null());
  }

  /**
   * Returns a reference to a set of all databases in the specified database.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @param scope a reference to a database. Type: Reference
   * @return a new {@link Expr} instance
   * @see #Database(String)
   * @see #Paginate(Expr)
   */
  public static Expr Databases(Expr scope) {
    return Fn.apply("databases", scope);
  }

  /**
   * Returns a reference to a set of all indexes in the database.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @return a new {@link Expr} instance
   * @see #Paginate(Expr)
   */
  public static Expr Indexes() {
    return Indexes(Null());
  }

  /**
   * Returns a reference to a set of all indexes in the specified database.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @param scope a reference to a database. Type: Reference
   * @return a new {@link Expr} instance
   * @see #Paginate(Expr)
   */
  public static Expr Indexes(Expr scope) {
    return Fn.apply("indexes", scope);
  }

  /**
   * Returns a reference to a set of all user defined functions in the database.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @return a new {@link Expr} instance
   * @see #Paginate(Expr)
   */
  public static Expr Functions() {
    return Functions(Null());
  }

  /**
   * Returns a reference to a set of all user defined functions in the specified database.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @param scope a reference to a database. Type: Reference
   * @return a new {@link Expr} instance
   * @see #Ref(Expr, String)
   * @see #Database(String)
   * @see #Paginate(Expr)
   */
  public static Expr Functions(Expr scope) {
    return Fn.apply("functions", scope);
  }

  /**
   * Returns a reference to a set of all keys in the database.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @return a new {@link Expr} instance
   * @see #Paginate(Expr)
   */
  public static Expr Keys() {
    return Keys(Null());
  }

  /**
   * Returns a reference to a set of all keys in the specified database.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @param scope a reference to a database. Type: Reference
   * @return a new {@link Expr} instance
   * @see #Ref(Expr, String)
   * @see #Database(String)
   * @see #Paginate(Expr)
   */
  public static Expr Keys(Expr scope) {
    return Fn.apply("keys", scope);
  }

  /**
   * Returns a reference to a set of all tokens in the database.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @return a new {@link Expr} instance
   * @see #Paginate(Expr)
   */
  public static Expr Tokens() {
    return Tokens(Null());
  }

  /**
   * Returns a reference to a set of all tokens in the specified database.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @param scope a reference to a database. Type: Reference
   * @return a new {@link Expr} instance
   * @see #Ref(Expr, String)
   * @see #Database(String)
   * @see #Paginate(Expr)
   */
  public static Expr Tokens(Expr scope) {
    return Fn.apply("tokens", scope);
  }

  /**
   * Returns a reference to a set of all credentials in the database.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @return a new {@link Expr} instance
   * @see #Paginate(Expr)
   */
  public static Expr Credentials() {
    return Credentials(Null());
  }

  /**
   * Returns a reference to a set of all credentials in the specified database.
   * A reference set must be paginated in order to retrieve its values.
   *
   * @param scope a reference to a database. Type: Reference
   * @return a new {@link Expr} instance
   * @see #Ref(Expr, String)
   * @see #Database(String)
   * @see #Paginate(Expr)
   */
  public static Expr Credentials(Expr scope) {
    return Fn.apply("credentials", scope);
  }

  /**
   * Encodes the given object using the reflection {@link Encoder}.
   *
   * @param value the object to be encoded
   * @return a new {@link Expr} instance
   * @see Encoder
   */
  public static Expr Value(Object value) {
    return Encoder.encode(value).get();
  }

  /**
   * Encodes the given {@link String} as an {@link Expr} instance.
   *
   * @param value the string to be encoded
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(String value) {
    return new StringV(value);
  }

  /**
   * Encodes the given {@link Long} as an {@link Expr} instance.
   *
   * @param value the number to be encoded
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(long value) {
    return new LongV(value);
  }

  /**
   * Encodes the given {@link Double} as an {@link Expr} instance.
   *
   * @param value the number to be encoded
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(double value) {
    return new DoubleV(value);
  }

  /**
   * Encodes the given {@link Boolean} as an {@link Expr} instance.
   *
   * @param value the boolean value to be encoded
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(boolean value) {
    return BooleanV.valueOf(value);
  }

  /**
   * Encodes the given {@link Instant} as an {@link Expr} instance.
   *
   * @param value the timestamp to be encoded
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   * @see Instant
   */
  public static Expr Value(Instant value) {
    return new TimeV(HighPrecisionTime.fromInstant(value));
  }

  /**
   * Encodes the given {@link HighPrecisionTime} as an {@link Expr} instance.
   *
   * @param value the timestamp to be encoded
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   * @see HighPrecisionTime
   */
  public static Expr Value(HighPrecisionTime value) {
    return new TimeV(value);
  }

  /**
   * Encodes the given {@link LocalDate} as an {@link Expr} instance.
   *
   * @param value the date to be encoded
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   * @see LocalDate
   */
  public static Expr Value(LocalDate value) {
    return new DateV(value);
  }

  /**
   * Encodes the given {@link Byte} array as an {@link Expr} instance.
   *
   * @param bytes the byte array to be encoded
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Value(byte[] bytes) {
    return new BytesV(bytes);
  }

  /**
   * Creates a new expression representing a null value.
   *
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Null() {
    return NullV.NULL;
  }

  /**
   * Creates a new object wrapping the {@link Map} provided.
   * For convenience, the following helpers are available:
   * <ul>
   *   <li>{@link #Obj()}</li>
   *   <li>{@link #Obj(String, Expr)}</li>
   *   <li>{@link #Obj(String, Expr, String, Expr)}</li>
   *   <li>{@link #Obj(String, Expr, String, Expr, String, Expr)}</li>
   *   <li>{@link #Obj(String, Expr, String, Expr, String, Expr, String, Expr)}</li>
   *   <li>{@link #Obj(String, Expr, String, Expr, String, Expr, String, Expr, String, Expr)}</li>
   * </ul>
   *
   * @param values the key/value {@link Map} to be wrapped
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(Map<String, ? extends Expr> values) {
    return Fn.apply("object", Fn.apply(values));
  }

  /**
   * Creates an empty object.
   *
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj() {
    return Obj(ImmutableMap.<String, Expr>of());
  }

  /**
   * Creates a new object with the provided key and value.
   *
   * @param k1 a key
   * @param v1 a value
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Expr v1) {
    return Obj(ImmutableMap.of(k1, v1));
  }

  /**
   * Creates a new object with two key/value pairs.
   *
   * @param k1 the first key
   * @param v1 the first value
   * @param k2 the second key
   * @param v2 the second value
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Expr v1, String k2, Expr v2) {
    return Obj(ImmutableMap.of(k1, v1, k2, v2));
  }

  /**
   * Creates a new object with three key/value pairs.
   *
   * @param k1 the first key
   * @param v1 the first value
   * @param k2 the second key
   * @param v2 the second value
   * @param k3 the third key
   * @param v3 the third value
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Expr v1, String k2, Expr v2, String k3, Expr v3) {
    return Obj(ImmutableMap.of(k1, v1, k2, v2, k3, v3));
  }

  /**
   * Creates a new object with four key/value pairs.
   *
   * @param k1 the first key
   * @param v1 the first value
   * @param k2 the second key
   * @param v2 the second value
   * @param k3 the third key
   * @param v3 the third value
   * @param k4 the fourth key
   * @param v4 the fourth value
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Expr v1, String k2, Expr v2, String k3, Expr v3, String k4, Expr v4) {
    return Obj(ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4));
  }

  /**
   * Creates a new object with five key/value pairs.
   *
   * @param k1 the first key
   * @param v1 the first value
   * @param k2 the second key
   * @param v2 the second value
   * @param k3 the third key
   * @param v3 the third value
   * @param k4 the fourth key
   * @param v4 the fourth value
   * @param k5 the fifth key
   * @param v5 the fifth value
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Obj(String k1, Expr v1, String k2, Expr v2, String k3, Expr v3, String k4, Expr v4, String k5, Expr v5) {
    return Obj(ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
  }

  /**
   * Creates a new array wrapping the provided {@link List}.
   * For convenience, see the {@link #Arr(Expr...)} helper.
   *
   * @param values the {@link List} instance to be wrapped
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Arr(List<? extends Expr> values) {
    return Fn.apply(values);
  }

  /**
   * Creates a new array containing with the values provided.
   *
   * @param values the elements of the new array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#values">FaunaDB Values</a>
   */
  public static Expr Arr(Expr... values) {
    return Arr(ImmutableList.copyOf(values));
  }

  /**
   * Aborts the current transaction with a given message.
   *
   * @param msg a message to be used when aborting the transaction
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   */
  public static Expr Abort(String msg) {
    return Abort(Value(msg));
  }

  /**
   * Aborts the current transaction with a given message.
   *
   * @param msg a message to be used when aborting the transaction. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   */
  public static Expr Abort(Expr msg) {
    return Fn.apply("abort", msg);
  }

  /**
   * Calls the given user defined function with the arguments provided.
   *
   * @param ref the reference to the user defined function to be called. Type: Reference
   * @param args the list of arguments for the given user defined function
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   */
  public static Expr Call(Expr ref, List<? extends  Expr> args) {
    return Fn.apply("call", ref, "arguments", varargs(args));
  }

  /**
   * Calls the given user defined function with the arguments provided.
   *
   * @param ref the reference to the user defined function to be called. Type: Reference
   * @param args the arguments for the given user defined function
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   */
  public static Expr Call(Expr ref, Expr... args) {
    return Call(ref, ImmutableList.copyOf(args));
  }

  /**
   * Creates a new query expression with the given lambda.
   *
   * @param lambda a lambda type
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   * @see #CreateFunction(Expr)
   * @see #Lambda(Expr, Expr)
   */
  public static Expr Query(Expr lambda) {
    return Fn.apply("query", lambda);
  }

  /**
   * Execute the reads associated with the given expression at the timestamp provided.
   * Writes are still executed at the current transaction time.
   *
   * @param timestamp the read timestamp. Type: Timestamp
   * @param expr the scoped expression
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   * @see #Time(Expr)
   * @see #Value(Instant)
   * @see #Value(HighPrecisionTime)
   */
  public static Expr At(Expr timestamp, Expr expr) {
    return Fn.apply("at", timestamp, "expr", expr);
  }

  /**
   * Bind values to one or more variables.
   * Variables must be accessed using the {@link #Var(String)} function.
   *
   * <p>Example:</p>
   * <pre>{@code
   * // Creates a binding map of variable names and their values,
   * // then use the binding map to build a let expression where
   * // the values can be referenced to by the expression provided
   * // using their given names.
   * Map<String, Expr> bindings = new HashMap<>();
   * bindings.add("name", Value("Bob"));
   * bindings.add("age", Value(53));
   *
   * client.query(
   *   Let(bindings).in(
   *     Create(Class("users"), Obj("data", Obj(
   *       "name", Var("name"),
   *       "age", Var("age"),
   *     )))
   *   )
   * ).get();
   * }</pre>
   *
   * @param bindings a {@link Map} of variable names to values.
   * @return a new {@link LetBinding} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   * @see #Var(String)
   * @see LetBinding
   */
  public static LetBinding Let(Map<String, ? extends Expr> bindings) {
    return new LetBinding(ImmutableMap.copyOf(bindings));
  }

  /**
   * Bind values to one or more variables.
   * Variables must be accessed using the {@link #Var(String)} function.
   *
   * <p>Example:</p>
   * <pre>{@code
   * client.query(
   *   Let("a", Value(1)).in(
   *     Add(Value(1), Var("a"))
   *   )
   * ).get();
   * }</pre>
   *
   * @param v1 the variable name
   * @param d1 the variable value
   * @return a new {@link LetBinding} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   * @see #Var(String)
   * @see LetBinding
   */
  public static LetBinding Let(String v1, Expr d1) {
    return Let(ImmutableMap.of(v1, d1));
  }

  /**
   * Bind values to one or more variables.
   * Variables must be accessed using the {@link #Var(String)} function.
   *
   * <p>Example:</p>
   * <pre>{@code
   * client.query(
   *   Let("a", Value(1), "b", Value(2)).in(
   *     Add(Var("a"), Var("b"))
   *   )
   * ).get();
   * }</pre>
   *
   * @param v1 the first variable name
   * @param d1 the first variable value
   * @param v2 the second variable name
   * @param d2 the second variable value
   * @return a new {@link LetBinding} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   * @see #Var(String)
   * @see LetBinding
   */
  public static LetBinding Let(String v1, Expr d1, String v2, Expr d2) {
    return Let(ImmutableMap.of(v1, d1, v2, d2));
  }

  /**
   * Bind values to one or more variables.
   * Variables must be accessed using the {@link #Var(String)} function.
   *
   * <p>Example:</p>
   * <pre>{@code
   * client.query(
   *   Let(
   *     "a", Value(1),
   *     "b", Value(2),
   *     "c", Value(3),
   *   ).in(
   *     Add(Var("a"), Var("b"), Var("c"))
   *   )
   * ).get();
   * }</pre>
   *
   * @param v1 the first variable name
   * @param d1 the first variable value
   * @param v2 the second variable name
   * @param d2 the second variable value
   * @param v3 the third variable name
   * @param d3 the third variable value
   * @return a new {@link LetBinding} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   * @see #Var(String)
   * @see LetBinding
   */
  public static LetBinding Let(String v1, Expr d1, String v2, Expr d2, String v3, Expr d3) {
    return Let(ImmutableMap.of(v1, d1, v2, d2, v3, d3));
  }

  /**
   * Bind values to one or more variables.
   * Variables must be accessed using the {@link #Var(String)} function.
   *
   * <p>Example:</p>
   * <pre>{@code
   * client.query(
   *   Let(
   *     "a", Value(1),
   *     "b", Value(2),
   *     "c", Value(3),
   *     "d", Value(4),
   *   ).in(
   *     Add(Var("a"), Var("b"), Var("c"), Var("d"))
   *   )
   * ).get();
   * }</pre>
   *
   * @param v1 the first variable name
   * @param d1 the first variable value
   * @param v2 the second variable name
   * @param d2 the second variable value
   * @param v3 the third variable name
   * @param d3 the third variable value
   * @param v4 the fourth variable name
   * @param d4 the fourth variable value
   * @return a new {@link LetBinding} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   * @see #Var(String)
   * @see LetBinding
   */
  public static LetBinding Let(String v1, Expr d1, String v2, Expr d2, String v3, Expr d3, String v4, Expr d4) {
    return Let(ImmutableMap.of(v1, d1, v2, d2, v3, d3, v4, d4));
  }

  /**
   * Bind values to one or more variables.
   * Variables must be accessed using the {@link #Var(String)} function.
   *
   * <p>Example:</p>
   * <pre>{@code
   * client.query(
   *   Let(
   *     "a", Value(1),
   *     "b", Value(2),
   *     "c", Value(3),
   *     "d", Value(4),
   *     "e", Value(4),
   *   ).in(
   *     Add(Var("a"), Var("b"), Var("c"), Var("d"), Var("e"))
   *   )
   * ).get()
   * }</pre>
   *
   * @param v1 the first variable name
   * @param d1 the first variable value
   * @param v2 the second variable name
   * @param d2 the second variable value
   * @param v3 the third variable name
   * @param d3 the third variable value
   * @param v4 the fourth variable name
   * @param d4 the fourth variable value
   * @param v5 the fifth variable name
   * @param d5 the fitfh variable value
   * @return a new {@link LetBinding} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   * @see #Var(String)
   * @see LetBinding
   */
  public static LetBinding Let(String v1, Expr d1, String v2, Expr d2, String v3, Expr d3, String v4, Expr d4, String v5, Expr d5) {
    return Let(ImmutableMap.of(v1, d1, v2, d2, v3, d3, v4, d4, v5, d5));
  }

  /**
   * Creates a expression that refers to the value of the given variable name in the current lexical scope.
   *
   * @param name the referred variable name
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   * @see #Let(Map)
   * @see #Lambda(Expr, Expr)
   */
  public static Expr Var(String name) {
    return Fn.apply("var", Value(name));
  }

  /**
   * Evaluates the then or else branch according to the given condition.
   *
   * @param condition the if condition. Type: Boolean
   * @param thenExpr the then branch for the if expression
   * @param elseExpr the else branch for the if expression
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   */
  public static Expr If(Expr condition, Expr thenExpr, Expr elseExpr) {
    return Fn.apply("if", condition, "then", thenExpr, "else", elseExpr);
  }

  /**
   * Evaluates the given expressions sequentially evaluates its arguments,
   * and returns the result of the last expression.
   *
   * @param exprs a list of expressions to be evaluated sequentially
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   */
  public static Expr Do(List<? extends Expr> exprs) {
    return Fn.apply("do", varargs(exprs));
  }

  /**
   * Evaluates the given expressions sequentially evaluates its arguments,
   * and returns the result of the last expression.
   *
   * @param exprs a list of expressions to be evaluated sequentially
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   */
  public static Expr Do(Expr... exprs) {
    return Do(ImmutableList.copyOf(exprs));
  }

  /**
   * Creates an anonymous function that binds one or more variables in the expression provided.
   *
   * <p>Example:</p>
   * <pre>{@code
   * client.query(
   *   Map(userNames, Lambda(Value("name"),
   *     Create(Class("user"), Obj("data", Obj(
   *       "name", Var("name")
   *     )))
   *   ))
   * ).get();
   * }</pre>
   *
   * <p>An array of variable names can be provided to match multiple variables at once. For example:</p>
   * <pre>{@code
   * // Assuming the variable userFullNames has the following format:
   * // [
   * //   ["name", "surname"],
   * //   ["name", "surname"]
   * // ]
   * client.query(
   *   Map(userFullNames,
   *     Lambda(
   *       Arr(
   *         Value("name"),
   *         Value("surname")
   *       ),
   *       Create(Class("user"), Obj("data", Obj(
   *         "fullName", Concat(
   *           Var("name"),
   *           Var("surname")
   *         )
   *       )))
   *     )
   *   )
   * ).get();
   * }</pre>
   *
   * @param var the lambda's parameter binding. Type: String or an Array of strings
   * @param expr the lambda's function body. Type: An expression.
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#basic-forms">FaunaDB Basic Forms</a>
   * @see #Map(Expr, Expr)
   * @see #Foreach(Expr, Expr)
   * @see #Filter(Expr, Expr)
   * @see #Query(Expr)
   */
  public static Expr Lambda(Expr var, Expr expr) {
    return Fn.apply("lambda", var, "expr", expr);
  }

  /**
   * Applies the given lambda to each element of the provided collection, and returns
   * the results of each application in a new collection of the same type.
   *
   * Map applies the lambda function concurrently to each element in the collection. Side-effects do not affect
   * evaluation of other lambda applications. The order of possible refs being generated within the lambda are
   * non-deterministic.
   *
   * @param collection the source collection. Type: Collection.
   * @param lambda the lambda function to be applied for each element in the given collection. Type: A lambda function.
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#collections">FaunaDB Collection Functions</a>
   * @see #Lambda(Expr, Expr)
   */
  public static Expr Map(Expr collection, Expr lambda) {
    return Fn.apply("map", lambda, "collection", collection);
  }

  /**
   * Applies the given lambda to each element of the provided collection.
   *
   * Foreach applies the lambda function concurrently to each element in the collection. Side-effects do not affect
   * evaluation of other lambda applications. The order of possible refs being generated within the lambda are
   * non-deterministic.
   *
   * @param collection the source collection. Type: Collection.
   * @param lambda the lambda function to be applied for each element in the given collection. Type: A lambda function.
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#collections">FaunaDB Collection Functions</a>
   * @see #Lambda(Expr, Expr)
   */
  public static Expr Foreach(Expr collection, Expr lambda) {
    return Fn.apply("foreach", lambda, "collection", collection);
  }

  /**
   * Applies the given lambda to each element of the collection provided, and
   * returns a new collection containing only the elements for which the lambda returns true.
   *
   * @param collection the source collection. Type: Collection
   * @param lambda the filter lambda. Type: A lambda expression that returns a boolean value
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#collections">FaunaDB Collection Functions</a>
   */
  public static Expr Filter(Expr collection, Expr lambda) {
    return Fn.apply("filter", lambda, "collection", collection);
  }

  /**
   * Returns a new collection containing the given number of elements taken from the provided collection.
   *
   * @param num the number of elements to be taken from the source collection. Type: Number
   * @param collection the source collection. Type: Collection
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#collections">FaunaDB Collection Functions</a>
   */
  public static Expr Take(Expr num, Expr collection) {
    return Fn.apply("take", num, "collection", collection);
  }

  /**
   * Returns a new collection containing after dropping the given number of elements from the provided collection.
   *
   * @param num the number of elements to be dropped from the source collection. Type: Number
   * @param collection the source collection. Type: Collection
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#collections">FaunaDB Collection Functions</a>
   */
  public static Expr Drop(Expr num, Expr collection) {
    return Fn.apply("drop", num, "collection", collection);
  }

  /**
   * Returns a new collection with the given elements prepended to the provided collection.
   *
   * @param elements the elements to be prepended to the source collection. Type: Array
   * @param collection the source collection. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#collections">FaunaDB Collection Functions</a>
   */
  public static Expr Prepend(Expr elements, Expr collection) {
    return Fn.apply("prepend", elements, "collection", collection);
  }

  /**
   * Returns a new collection with the given elements appended to the provided collection.
   *
   * @param elements the elements to be appended to the source collection. Type: Array
   * @param collection the source collection. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#collections">FaunaDB Collection Functions</a>
   */
  public static Expr Append(Expr elements, Expr collection) {
    return Fn.apply("append", elements, "collection", collection);
  }

  /**
   * Returns true if the given collection is empty, or false otherwise.
   *
   * @param collection the source collection to check for emptiness. Type: Collection
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#collections">FaunaDB Collection Functions</a>
   */
  public static Expr IsEmpty(Expr collection) {
    return Fn.apply("is_empty", collection);
  }

  /**
   * Returns true if the given collection is not empty, or false otherwise.
   *
   * @param collection the source collection to check for non-emptiness. Type: Collection
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#collections">FaunaDB Collection Functions</a>
   */
  public static Expr IsNonEmpty(Expr collection) {
    return Fn.apply("is_nonempty", collection);
  }

  /**
   * Retrieves the instance identified by the given reference.
   *
   * @param ref the reference to be retrieved. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#read-functions">FaunaDB Read Functions</a>
   * @see #Ref(Expr, String)
   */
  public static Expr Get(Expr ref) {
    return Fn.apply("get", ref);
  }

  /**
   * Retrieves the instance identified by the given reference at a specific point in time.
   *
   * @param ref the reference to be retrieved. Type: Reference
   * @param timestamp the timestamp from which the reference's data will be retrieved. Type: Timestamp
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#read-functions">FaunaDB Read Functions</a>
   * @see #Ref(Expr, String)
   * @see #Time(Expr)
   * @see #Value(Instant)
   * @see #Value(HighPrecisionTime)
   * @see #At(Expr, Expr)
   */
  public static Expr Get(Expr ref, Expr timestamp) {
    return Fn.apply("get", ref, "ts", timestamp);
  }

  /**
   * Retrieves the key object given the key's secret string.
   *
   * @param secret the key's secret string. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#read-functions">FaunaDB Read Functions</a>
   */
  public static Expr KeyFromSecret(Expr secret) {
    return Fn.apply("key_from_secret", secret);
  }

  /**
   * Returns a Page object that groups a page of results and cursors for retrieving pages before or after
   * the current page. Pages are collections and can be passed directly to some functions such as
   * {@link #Map(Expr, Expr)}, {@link #Foreach(Expr, Expr)}, or {@link #Filter(Expr, Expr)}.
   * Transformations are applied to the Page’s data array; cursors are passed through.
   *
   * <p>Example:</p>
   * <pre>{@code
   * Pagination paginateUsersQuery = Paginate(Match(Index("all_users_refs"))).withSize(20);
   * Optional<Value> nextPageCursor = Optional.absent();
   *
   * do {
   *   if (nextPageCursor.isPresent()) {
   *     paginateUsersQuery.after(nextPageCursor);
   *   }
   *
   *   Value result = client.query(
   *     Map(
   *       paginateUsersQuery,
   *       Lambda(
   *         Value("ref"),
   *         Get(Var("ref"))
   *       )
   *     )
   *   ).get();
   *
   *   Collection<User> allUsers = result.at("data")
   *     .asCollectionOf(User.class)
   *     .get();
   *
   *   // The before and after cursors must be considered opaque values.
   *   // There is no need to convert them to different types. However,
   *   // this call will safe guard our code from the absence of a cursor.
   *   nextPageCursor = result.at("after")
   *     .to(Value.class)
   *     .getOptional();
   *
   *   doSomething(allUsers);
   *
   * } while(nextPageCursor.isPresent());
   * }</pre>
   *
   * @param resource the resource to paginate
   * @return a {@link Pagination} builder
   * @see Pagination
   * @see <a href="https://fauna.com/documentation/queries#read-functions">FaunaDB Read Functions</a>
   * @see #Match(Expr)
   * @see #Map(Expr, Expr)
   * @see #Foreach(Expr, Expr)
   * @see #Filter(Expr, Expr)
   */
  public static Pagination Paginate(Expr resource) {
    return new Pagination(resource);
  }

  /**
   * Returns true if the provided reference exists, or false otherwise.
   *
   * @param ref the reference to check for existence. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#read-functions">FaunaDB Read Functions</a>
   * @see #Ref(Expr, String)
   */
  public static Expr Exists(Expr ref) {
    return Fn.apply("exists", ref);
  }

  /**
   * Returns true if the provided reference exists at a specific point in time, or false otherwise.
   *
   * @param ref the reference to check for existence. Type: Reference
   * @param timestamp a timestamp to check for ref's existence. Type: Timestamp
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#read-functions">FaunaDB Read Functions</a>
   * @see #Ref(Expr, String)
   */
  public static Expr Exists(Expr ref, Expr timestamp) {
    return Fn.apply("exists", ref, "ts", timestamp);
  }

  /**
   * Creates a new instance of the given class with the parameters provided.
   *
   * @param ref the class reference for which a new instance will be created. Type: Reference
   * @param params the parameters used to create the new instance. Type: Object
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see #Class(Expr)
   * @see #Ref(Expr, String)
   * @see #Obj(Map)
   */
  public static Expr Create(Expr ref, Expr params) {
    return Fn.apply("create", ref, "params", params);
  }

  /**
   * Updates the resource identified by the given reference. Updates are partial, and only modify values specified.
   * Scalar values or arrays are replaced by new versions, objects are merged, and null removes a value.
   *
   * @param ref the resource reference to update. Type: Reference
   * @param params the parameters used to update the new instance. Type: Object
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see #Ref(Expr, String)
   * @see #Obj(Map)
   */
  public static Expr Update(Expr ref, Expr params) {
    return Fn.apply("update", ref, "params", params);
  }

  /**
   * Replaces the resource identified by the given reference.
   *
   * @param ref the resource reference to be replaced. Type: Reference
   * @param params the parameters used to replace the resource's old values. Type: Object
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see #Ref(Expr, String)
   * @see #Obj(Map)
   */
  public static Expr Replace(Expr ref, Expr params) {
    return Fn.apply("replace", ref, "params", params);
  }

  /**
   * Delete the resource identified by the given reference.
   *
   * @param ref the resource reference to be deleted. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see #Ref(Expr, String)
   */
  public static Expr Delete(Expr ref) {
    return Fn.apply("delete", ref);
  }

  /**
   * Inserts a new event in the instance's history.
   *
   * @param ref the target resource. Type: Reference
   * @param timestamp the timestamp in which the event will be inserted in the
   *                  instance's history. Type: Timestamp
   * @param action the event action. Type: action
   * @param params the event's parameters. Type: Object
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see Action
   * @see #Ref(Expr, String)
   * @see #Obj(Map)
   * @see #Time(Expr)
   * @see #Value(Instant)
   * @see #Value(HighPrecisionTime)
   */
  public static Expr Insert(Expr ref, Expr timestamp, Expr action, Expr params) {
    return Fn.apply("insert", ref, "ts", timestamp, "action", action, "params", params);
  }

  /**
   * Inserts a new event in the instance's history.
   *
   * @param ref the target resource. Type: Reference
   * @param timestamp the timestamp in which the event will be inserted in the
   *                  instance's history. Type: Timestamp
   * @param action the event action
   * @param params the event's parameters. Type: Object
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see #Ref(Expr, String)
   * @see #Obj(Map)
   * @see #Time(Expr)
   * @see #Value(Instant)
   * @see #Value(HighPrecisionTime)
   */
  public static Expr Insert(Expr ref, Expr timestamp, Action action, Expr params) {
    return Insert(ref, timestamp, action.value, params);
  }

  /**
   * Removes an event from an instance's history.
   *
   * @param ref the target resource. Type: Reference
   * @param timestamp the timestamp in which the event happened. Type: Timestamp
   * @param action the event's action. Type: event action
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see Action
   * @see #Ref(Expr, String)
   * @see #Obj(Map)
   * @see #Time(Expr)
   * @see #Value(Instant)
   * @see #Value(HighPrecisionTime)
   */
  public static Expr Remove(Expr ref, Expr timestamp, Expr action) {
    return Fn.apply("remove", ref, "ts", timestamp, "action", action);
  }

  /**
   * Removes an event from an instance's history.
   *
   * @param ref the target resource. Type: Reference
   * @param timestamp the timestamp in which the event happened. Type: Timestamp
   * @param action the event's action
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see #Ref(Expr, String)
   * @see #Obj(Map)
   * @see #Time(Expr)
   * @see #Value(Instant)
   * @see #Value(HighPrecisionTime)
   */
  public static Expr Remove(Expr ref, Expr timestamp, Action action) {
    return Remove(ref, timestamp, action.value);
  }

  /**
   * Creates a new class in the current database.
   *
   * @param params the class's configuration parameters. Type: Object
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see #Obj(Map)
   */
  public static Expr CreateClass(Expr params) {
    return Fn.apply("create_class", params);
  }

  /**
   * Creates a new database.
   *
   * <p>Example:</p>
   * <pre>{@code
   * client.query(
   *   CreateDatabase(Obj(
   *     "name", Value("my_database")
   *   ))
   * ).get()
   * }</pre>
   *
   * @param params the database's configuration parameters. Type: Object
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see #Obj(Map)
   */
  public static Expr CreateDatabase(Expr params) {
    return Fn.apply("create_database", params);
  }

  /**
   * Create a new key in the current database.
   *
   * @param params the key's configuration parameters: Type Object
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see #Obj(Map)
   */
  public static Expr CreateKey(Expr params) {
    return Fn.apply("create_key", params);
  }

  /**
   * Creates a new index in the current database.
   *
   * @param params the index's configuration parameter. Type: Object
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see #Obj(Map)
   */
  public static Expr CreateIndex(Expr params) {
    return Fn.apply("create_index", params);
  }

  /**
   * Creates a new user defined function in the current database.
   *
   * @param params the function's configuration parameters. Type: Object
   * @return a new {@link Expr} instnace
   * @see <a href="https://fauna.com/documentation/queries#write-functions">FaunaDB Write Functions</a>
   * @see #Obj(Map)
   */
  public static Expr CreateFunction(Expr params) {
    return Fn.apply("create_function", params);
  }

  /**
   * Returns the history of an instance's presence for the given reference.
   *
   * @param ref the instance's reference to retrieve the singleton history. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   * @see #Ref(Expr, String)
   */
  public static Expr Singleton(Expr ref) {
    return Fn.apply("singleton", ref);
  }

  /**
   * Returns the history of an instance's data for the given reference.
   *
   * @param refSet the resource to retrieve events for. Type: Reference or set reference.
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   * @see #Ref(Expr, String)
   */
  public static Expr Events(Expr refSet) {
    return Fn.apply("events", refSet);
  }

  /**
   * Returns the set of resources for the given index.
   * Sets must be paginated with the function {@link #Paginate(Expr)} in order to retrieve their values.
   * If the provided index has terms configured, check the {@link #Match(Expr, Expr)} function.
   *
   * @param index an index reference. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   * @see #Index(String)
   * @see #Paginate(Expr)
   */
  public static Expr Match(Expr index) {
    return Fn.apply("match", index);
  }

  /**
   * Returns the set of resources that match the terms for the given index.
   * Sets must be paginated with the function {@link #Paginate(Expr)} in order to retrieve their values.
   *
   * @param index an index reference. Type: Reference
   * @param term a value to search in the index provided. Type: any value
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   * @see #Index(String)
   * @see #Paginate(Expr)
   */
  public static Expr Match(Expr index, Expr term) {
    return Fn.apply("match", index, "terms", term);
  }

  /**
   * Returns the set of resources present in at least on of the sets provided.
   *
   * @param sets the sets to execute the union operation
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   * @see #Match(Expr)
   */
  public static Expr Union(List<? extends Expr> sets) {
    return Fn.apply("union", varargs(sets));
  }

  /**
   * Returns the set of resources present in at least on of the sets provided.
   *
   * @param sets the sets to execute the union operation. Type: Array of sets
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   * @see #Match(Expr)
   */
  public static Expr Union(Expr... sets) {
    return Union(ImmutableList.copyOf(sets));
  }

  /**
   * Returns the set of resources present in all sets provided.
   *
   * @param sets the sets to execute the intersection
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   * @see #Match(Expr)
   */
  public static Expr Intersection(List<? extends Expr> sets) {
    return Fn.apply("intersection", varargs(sets));
  }

  /**
   * Returns the set of resources present in all sets provided.
   *
   * @param sets the sets to execute the intersection. Type: Array of sets
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   * @see #Match(Expr)
   */
  public static Expr Intersection(Expr... sets) {
    return Intersection(ImmutableList.copyOf(sets));
  }

  /**
   * Returns the set of resources present in the first set and not in any other set provided.
   *
   * @param sets the sets to take the difference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   * @see #Match(Expr)
   */
  public static Expr Difference(List<? extends Expr> sets) {
    return Fn.apply("difference", varargs(sets));
  }

  /**
   * Returns the set of resources present in the first set and not in any other set provided.
   *
   * @param sets the sets to take the difference. Type: Array of sets
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   * @see #Match(Expr)
   */
  public static Expr Difference(Expr... sets) {
    return Difference(ImmutableList.copyOf(sets));
  }

  /**
   * Returns a new set after removing all duplicated values.
   *
   * @param set the set to remove duplicates. Type: Set
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   * @see #Match(Expr)
   */
  public static Expr Distinct(Expr set) {
    return Fn.apply("distinct", set);
  }

  /**
   * Derives a set of resources from applying each instance of the source set to the target parameter.
   * The target parameter can assume two forms: a index reference, and a lambda function.
   * When the target is an index reference, Join will match each result of the source set with the target index's terms
   * When target is a lambda function, each result from the source set will be passed as the lambda's arguments.
   *
   * @param source the source set. Type: Array or Set.
   * @param target the join target. Type: Index reference or Lambda function
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#sets">FaunaDB Set Functions</a>
   * @see #Index(String)
   * @see #Lambda(Expr, Expr)
   */
  public static Expr Join(Expr source, Expr target) {
    return Fn.apply("join", source, "with", target);
  }

  /**
   * Creates a new authentication token for the provided reference.
   *
   * @param ref the token's owner reference. Type: Reference
   * @param params the token's configuration object. Type: Object
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#authentication">FaunaDB Authentication Functions</a>
   * @see #Ref(Expr, String)
   * @see #Obj(Map)
   */
  public static Expr Login(Expr ref, Expr params) {
    return Fn.apply("login", ref, "params", params);
  }

  /**
   * Delete authentication tokens. 
   * If {@code invalidateAll} is true, deletes all tokens associated with the current session. 
   * Otherwise, deletes only the token used for the current request.
   *
   * @param invalidateAll if the Logout function should delete all tokens
   *                      associated with the current session. Type: Boolean
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#authentication">FaunaDB Authentication Functions</a>
   * @see #Login(Expr, Expr)
   */
  public static Expr Logout(Expr invalidateAll) {
    return Fn.apply("logout", invalidateAll);
  }

  /**
   * Checks the given password against the reference's credentials, retuning true if valid, or false otherwise.
   *
   * @param ref the reference to authenticate. Type: Reference
   * @param password the authentication password. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#authentication">FaunaDB Authentication Functions</a>
   */
  public static Expr Identify(Expr ref, Expr password) {
    return Fn.apply("identify", ref, "password", password);
  }

  /**
   * Returns the reference associated with the authentication token used for the current request.
   *
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#authentication">FaunaDB Authentication Functions</a>
   * @see #Login(Expr, Expr)
   */
  public static Expr Identity() {
    return Fn.apply("identity", Null());
  }

  /**
   * Returns true if the authentication used for the request has an identity.
   *
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#authentication">FaunaDB Authentication Functions</a>
   * @see #Identity()
   * @see #Identify(Expr, Expr)
   * @see #Login(Expr, Expr)
   */
  public static Expr HasIdentity() {
    return Fn.apply("has_identity", Null());
  }

  /**
   * Concatenates a list of strings into a single string value.
   *
   * @param terms a list of strings. Type: Array of strings
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#string-functions">FaunaDB String Functions</a>
   * @see #Arr(List)
   * @see #Value(String)
   */
  public static Expr Concat(Expr terms) {
    return Fn.apply("concat", terms);
  }

  /**
   * Concatenates a list of strings into a single string value using the separator provided.
   *
   * @param terms a list of strings. Type: Array of strings
   * @param separator a string to separate each element in the result. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#string-functions">FaunaDB String Functions</a>
   * @see #Arr(List)
   * @see #Value(String)
   */
  public static Expr Concat(Expr terms, Expr separator) {
    return Fn.apply("concat", terms, "separator", separator);
  }

  /**
   * Normalizes strings according to the Unicode Standard, section 5.18 "Case Mappings". By default, it uses
   * NKFCCaseFold as recommended by W3C. In order to use a specific string normalizer,
   * see {@link #Casefold(Expr, Normalizer)}.
   *
   * @param str the string to be normalized. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#string-functions">FaunaDB String Functions</a>
   * @see <a href="https://www.w3.org/TR/charmod-norm/">W3C Character Model for the World Wide Web: String Matching</a>
   * @see #Casefold(Expr, Normalizer)
   */
  public static Expr Casefold(Expr str) {
    return Fn.apply("casefold", str);
  }

  /**
   * Normalizes strings according to the Unicode Standard, section 5.18 "Case Mappings", and the normalizer provided.
   * Pre-defined normalizers are available for the overload {@link #Casefold(Expr, Normalizer)}.
   *
   * @param str the string to be normalized. Type: String
   * @param normalizer the string normalizer to use. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#string-functions">FaunaDB String Functions</a>
   * @see <a href="https://www.w3.org/TR/charmod-norm/">W3C Character Model for the World Wide Web: String Matching</a>
   * @see #Casefold(Expr, Normalizer)
   */
  public static Expr Casefold(Expr str, Expr normalizer) {
    return Fn.apply("casefold", str, "normalizer", normalizer);
  }

  /**
   * Normalizes strings according to the Unicode Standard, section 5.18 "Case Mappings", and the normalizer provided.
   *
   * @param str the string to be normalized. Type: String
   * @param normalizer the string normalizer to use
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#string-functions">FaunaDB String Functions</a>
   * @see <a href="https://www.w3.org/TR/charmod-norm/">W3C Character Model for the World Wide Web: String Matching</a>
   * @see #Casefold(Expr, Normalizer)
   */
  public static Expr Casefold(Expr str, Normalizer normalizer) {
    return Casefold(str, normalizer.value);
  }

  /**
   * Tokenize the input into n-grams of the given sizes.
   *
   * @param terms the value to tokenize. Type: String
   * @param min the minimum size for the n-grams. Type: Number
   * @param max the maximum size for the n-grams. Type: Number
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#string-functions">FaunaDB String Functions</a>
   * @see #Value(String)
   * @see #Value(long)
   */
  public static Expr NGram(Expr terms, Expr min, Expr max) {
    return Fn.apply("ngram", terms, "min", min, "max", max);
  }

  /**
   * Tokenize the input into n-grams of the given sizes.
   *
   * @param terms the list of values to tokenize. Type: Array of strings
   * @param min the minimum size for the n-grams. Type: Number
   * @param max the maximum size for the n-grams. Type: Number
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#string-functions">FaunaDB String Functions</a>
   * @see #Arr(List)
   * @see #Value(String)
   * @see #Value(long)
   */
  public static Expr NGram(List<Expr> terms, Expr min, Expr max) {
    return NGram(varargs(terms), min, max);
  }

  /**
   * Tokenize the input into n-grams of the 1 and 2 elements in size.
   *
   * @param terms the list of values to tokenize. Type: Array of strings
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#string-functions">FaunaDB String Functions</a>
   * @see #Arr(List)
   * @see #Value(String)
   */
  public static Expr NGram(List<Expr> terms) {
    return NGram(varargs(terms));
  }

  /**
   * Tokenize the input into n-grams of the 1 and 2 elements in size.
   *
   * @param terms the list of values to tokenize. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#string-functions">FaunaDB String Functions</a>
   * @see #Arr(List)
   * @see #Value(String)
   */
  public static Expr NGram(Expr terms) {
    return Fn.apply("ngram", terms);
  }

  /**
   * Creates a new timestamp from an ISO-8601 offset date/time string. A special string "now" can be used to get the
   * current transaction time. Multiple references to "now" within the same query will be equal.
   *
   * @param str an ISO-8601 formatted string or the string literal "now"
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#time-and-date">FaunaDB Time and Date Functions</a>
   * @see #Value(String)
   */
  public static Expr Time(Expr str) {
    return Fn.apply("time", str);
  }

  /**
   * Constructs a timestamp relative to the epoch "1970-01-01T00:00:00Z" given a unit type and a number of units.
   *
   * @param num the number of units. Type: Number
   * @param unit the unit type
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#time-and-date">FaunaDB Time and Date Functions</a>
   * @see TimeUnit
   * @see #Value(long)
   */
  public static Expr Epoch(Expr num, TimeUnit unit) {
    return Epoch(num, unit.value);
  }

  /**
   * Constructs a timestamp relative to the epoch "1970-01-01T00:00:00Z" given a unit type and a number of units.
   * Possible unit types are:
   * <ul>
   *   <li>second</li>
   *   <li>millisecond</li>
   *   <li>microsecond</li>
   *   <li>nanosecond</li>
   * </ul>
   *
   * @param num the number of units. Type: Number
   * @param unit the unit type. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#time-and-date">FaunaDB Time and Date Functions</a>
   * @see #Value(long)
   */
  public static Expr Epoch(Expr num, Expr unit) {
    return Fn.apply("epoch", num, "unit", unit);
  }

  /**
   * Creates a date from an ISO-8601 formatted date string.
   *
   * @param str an ISO-8601 formatted date string
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#time-and-date">FaunaDB Time and Date Functions</a>
   * @see #Value(String)
   */
  public static Expr Date(Expr str) {
    return Fn.apply("date", str);
  }

  /**
   * Returns a new string identifier suitable for use when constructing references.
   *
   * @deprecated Use NewId() instead.
   *
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   * @see #Ref(Expr, Expr)
   */
  @Deprecated
  public static Expr NextId() {
    return Fn.apply("next_id", NullV.NULL);
  }

  /**
   * Returns a new string identifier suitable for use when constructing references.
   *
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   * @see #Ref(Expr, Expr)
   */
  public static Expr NewId() {
    return Fn.apply("new_id", NullV.NULL);
  }

  /**
   * Creates a new reference for the given class name.
   *
   * @param name the class name. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Class(Expr name) {
    return Fn.apply("class", name);
  }

  /**
   * Creates a new reference for the given class name.
   *
   * @param name the class name
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Class(String name) {
    return Class(Value(name));
  }

  /**
   * Creates a reference for the given class name, scoped to the database provided.
   *
   * @param name the class name. Type: String
   * @param database the scope database. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   * @see #Database(String)
   */
  public static Expr Class(Expr name, Expr database) {
    return Fn.apply("class", name, "scope", database);
  }

  /**
   * Creates a reference for the given class name, scoped to the database provided.
   *
   * @param name the class name
   * @param database the scope database. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   * @see #Database(String)
   */
  public static Expr Class(String name, Expr database) {
    return Class(Value(name), database);
  }

  /**
   * Creates a reference for the given database name.
   *
   * @param name the database name. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Database(Expr name) {
    return Fn.apply("database", name);
  }

  /**
   * Creates a reference for the given database name.
   *
   * @param name the database name
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Database(String name) {
    return Database(Value(name));
  }

  /**
   * Creates a reference for the given database name, scoped to the database provided.
   *
   * @param name the database name. Type: String
   * @param database the scope database. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Database(Expr name, Expr database) {
      return Fn.apply("database", name, "scope", database);
  }

  /**
   * Creates a reference for the given database name, scoped to the database provided.
   *
   * @param name the database name
   * @param database the scope database. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Database(String name, Expr database) {
    return Database(Value(name), database);
  }

  /**
   * Creates a reference for the given index name.
   *
   * @param name the index name. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Index(Expr name) {
    return Fn.apply("index", name);
  }

  /**
   * Creates a reference for the given index name.
   *
   * @param name the index name
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Index(String name) {
    return Index(Value(name));
  }

  /**
   * Creates a reference for the given index name, scoped to the database provided.
   *
   * @param name the index name. Type: String
   * @param database the scope database. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Index(Expr name, Expr database) {
    return Fn.apply("index", name, "scope", database);
  }

  /**
   * Creates a reference for the given index name, scoped to the database provided.
   *
   * @param name the index name
   * @param database the scope database. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Index(String name, Expr database) {
    return Index(Value(name), database);
  }

  /**
   * Creates a reference for the given user defined function name.
   *
   * @param name the user defined function name. Type: String
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Function(Expr name) {
    return Fn.apply("function", name);
  }

  /**
   * Creates a reference for the given user defined function name.
   *
   * @param name the user defined function name
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Function(String name) {
    return Function(Value(name));
  }

  /**
   * Creates a reference for the given user defined function name, scoped to the database provided.
   *
   * @param name the user defined function name. Type: String
   * @param database the scope database. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Function(Expr name, Expr database) {
    return Fn.apply("function", name, "scope", database);
  }

  /**
   * Creates a reference for the given user defined function name, scoped to the database provided.
   *
   * @param name the user defined function name
   * @param database the scope database. Type: Reference
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Function(String name, Expr database) {
    return Function(Value(name), database);
  }

  /**
   * Tests equivalence between a list of values.
   *
   * @param values the values to test equivalence for
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Equals(List<? extends Expr> values) {
    return Fn.apply("equals", varargs(values));
  }

  /**
   * Tests equivalence between a list of values.
   *
   * @param values the values to test equivalence for
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Equals(Expr... values) {
    return Equals(ImmutableList.copyOf(values));
  }

  /**
   * Returns true if the target value contains the given path, and false otherwise.
   * The path must be an array in which each element can be either a string, or a number.
   * If a string, the path segment refers to an object key. If a number, the path segment refers to an array index.
   *
   * For convenience, a path builder is available at the {@link #Contains(Path, Expr)} function.
   *
   * @param path the desired path to check for presence. Type: Array
   * @param in the target value. Type: Object or Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   * @see #Contains(Path, Expr)
   */
  public static Expr Contains(Expr path, Expr in) {
    return Fn.apply("contains", path, "in", in);
  }

  /**
   * Returns true if the target value contains the given path, and false otherwise.
   *
   * @param path the desired path to check for presence
   * @param in the target value. Type: Object or Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#miscellaneous-functions">FaunaDB Miscellaneous Functions</a>
   */
  public static Expr Contains(Path path, Expr in) {
    return Contains(Arr(path.segments), in);
  }

  /**
   * Constructs a path matching object keys.
   *
   * @param segments the object keys
   * @return a new {@link Path} instance
   * @see Path
   * @see #Contains(Path, Expr)
   * @see #Select(Path, Expr)
   * @see #Select(Path, Expr, Expr)
   * @see #SelectAll(Path, Expr)
   */
  public static Path Path(String... segments) {
    return new Path().at(segments);
  }

  /**
   * Constructs a path matching array indexes.
   *
   * @param segments the array indexes
   * @return a new {@link Path} instance
   * @see Path
   * @see #Contains(Path, Expr)
   * @see #Select(Path, Expr)
   * @see #Select(Path, Expr, Expr)
   * @see #SelectAll(Path, Expr)
   */
  public static Path Path(int... segments) {
    return new Path().at(segments);
  }

  /**
   * Traverses target resource returning the value under the given path. Returns an error if the path doesn't exist.
   * The path must be an array in which each element can be either a string, or a number. If a string, the path segment
   * refers to an object key. If a number, the path segment refers to an array index.
   *
   * For convenience, a path builder is available at the {@link #Select(Path, Expr)} function.
   *
   * @param path the path to the desired value. Type: Array
   * @param from the target resource. Type: Object or Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#read-functions">FaunaDB Read Functions</a>
   * @see #Select(Path, Expr)
   */
  public static Expr Select(Expr path, Expr from) {
    return Fn.apply("select", path, "from", from);
  }

  /**
   * Traverses target resource returning the value under the given path. Returns the default value provided if the path
   * doesn't exist. The path must be an array in which each element can be either a string, or a number. If a string,
   * the path segment refers to an object key. If a number, the path segment refers to an array index.
   *
   * For convenience, a path builder is available at the {@link #Select(Path, Expr, Expr)} function.
   *
   * @param path the path to the desired value. Type: Array
   * @param from the target resource. Type: Object or Array
   * @param defaultValue the default value to return if the desired path doesn't exist
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#read-functions">FaunaDB Read Functions</a>
   * @see #Select(Path, Expr, Expr)
   */
  public static Expr Select(Expr path, Expr from, Expr defaultValue) {
    return Fn.apply("select", path, "from", from, "default", defaultValue);
  }

  /**
   * Traverses target resource returning the value under the given path. Returns an error if the path doesn't exist.
   *
   * @param path the path to the desired value
   * @param from the target resource. Type: Object or Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#read-functions">FaunaDB Read Functions</a>
   */
  public static Expr Select(Path path, Expr from) {
    return Select(Arr(path.segments), from);
  }

  /**
   * Traverses target resource returning the value under the given path. Returns the default value provided if the path
   * doesn't exist.
   *
   * @param path the path to the desired value
   * @param from the target resource. Type: Object or Array
   * @param defaultValue the default value to return if the desired path doesn't exist
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#read-functions">FaunaDB Read Functions</a>
   */
  public static Expr Select(Path path, Expr from, Expr defaultValue) {
    return Select(Arr(path.segments), from, defaultValue);
  }

  /**
   * Selects the desired path for each element in the given array. The path must be an array in which each element
   * can be either a string, or a number. If a string, the path segment refers to an object key. If a number, the
   * path segment refers to an array index.
   *
   * For convenience, a path builder is available at the {@link #SelectAll(Path, Expr)} function.
   *
   * @param path the path to the desired value. Type: Array
   * @param from the collection to traverse. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#read-functions">FaunaDB Read Functions</a>
   * @see #Select(Path, Expr)
   */
  public static Expr SelectAll(Expr path, Expr from) {
    return Fn.apply("select_all", path, "from", from);
  }

  /**
   * Selects the desired path for each element in the given array.
   *
   * @param path the path to the desired value
   * @param from the collection to traverse. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#read-functions">FaunaDB Read Functions</a>
   */
  public static Expr SelectAll(Path path, Expr from) {
    return SelectAll(Arr(path.segments), from);
  }

  /**
   * Computes the sum of a list of numbers.
   *
   * @param values the list of numbers. Type: Array
   * @return a {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#mathematical-functions">FaunaDB Mathematical Functions</a>
   */
  public static Expr Add(List<? extends Expr> values) {
    return Fn.apply("add", varargs(values));
  }

  /**
   * Computes the sum of a list of numbers.
   *
   * @param values the list of numbers. Type: Array
   * @return a {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#mathematical-functions">FaunaDB Mathematical Functions</a>
   */
  public static Expr Add(Expr... values) {
    return Add(ImmutableList.copyOf(values));
  }

  /**
   * Computes the product of a list of numbers.
   *
   * @param values the list of numbers. Type: Array
   * @return a {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#mathematical-functions">FaunaDB Mathematical Functions</a>
   */
  public static Expr Multiply(List<? extends Expr> values) {
    return Fn.apply("multiply", varargs(values));
  }

  /**
   * Computes the product of a list of numbers.
   *
   * @param values the list of numbers. Type: Array
   * @return a {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#mathematical-functions">FaunaDB Mathematical Functions</a>
   */
  public static Expr Multiply(Expr... values) {
    return Multiply(ImmutableList.copyOf(values));
  }

  /**
   * Computes the difference of a list of numbers.
   *
   * @param values the list of numbers. Type: Array
   * @return a {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#mathematical-functions">FaunaDB Mathematical Functions</a>
   */
  public static Expr Subtract(List<? extends Expr> values) {
    return Fn.apply("subtract", varargs(values));
  }

  /**
   * Computes the difference of a list of numbers.
   *
   * @param values the list of numbers. Type: Array
   * @return a {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#mathematical-functions">FaunaDB Mathematical Functions</a>
   */
  public static Expr Subtract(Expr... values) {
    return Subtract(ImmutableList.copyOf(values));
  }

  /**
   * Computes the quotient of a list of numbers.
   *
   * @param values the list of numbers. Type: Array
   * @return a {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#mathematical-functions">FaunaDB Mathematical Functions</a>
   */
  public static Expr Divide(List<? extends Expr> values) {
    return Fn.apply("divide", varargs(values));
  }

  /**
   * Computes the quotient of a list of numbers.
   *
   * @param values the list of numbers. Type: Array
   * @return a {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#mathematical-functions">FaunaDB Mathematical Functions</a>
   */
  public static Expr Divide(Expr... values) {
    return Divide(ImmutableList.copyOf(values));
  }

  /**
   * Computes the remainder after division of a list of numbers.
   *
   * @param values the list of numbers. Type: Array
   * @return a {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#mathematical-functions">FaunaDB Mathematical Functions</a>
   */
  public static Expr Modulo(List<? extends Expr> values) {
    return Fn.apply("modulo", varargs(values));
  }

  /**
   * Computes the remainder after division of a list of numbers.
   *
   * @param values the list of numbers. Type: Array
   * @return a {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#mathematical-functions">FaunaDB Mathematical Functions</a>
   */
  public static Expr Modulo(Expr... values) {
    return Modulo(ImmutableList.copyOf(values));
  }

  /**
   * Returns true if the first element of the given collection is less than the ones following, and false otherwise.
   *
   * @param values the list of numbers to compare. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr LT(List<? extends Expr> values) {
    return Fn.apply("lt", varargs(values));
  }

  /**
   * Returns true if the first element of the given collection is less than the ones following, and false otherwise.
   *
   * @param values the list of numbers to compare. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr LT(Expr... values) {
    return LT(ImmutableList.copyOf(values));
  }

  /**
   * Returns true if the first element of the given collection is less than or equal to the ones following,
   * and false otherwise.
   *
   * @param values the list of numbers to compare. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr LTE(List<? extends Expr> values) {
    return Fn.apply("lte", varargs(values));
  }

  /**
   * Returns true if the first element of the given collection is less than or equal to the ones following,
   * and false otherwise.
   *
   * @param values the list of numbers to compare. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr LTE(Expr... values) {
    return LTE(ImmutableList.copyOf(values));
  }

  /**
   * Returns true if the first element of the given collection is greater than the ones following, and false otherwise.
   *
   * @param values the list of numbers to compare. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr GT(List<? extends Expr> values) {
    return Fn.apply("gt", varargs(values));
  }

  /**
   * Returns true if the first element of the given collection is greater than the ones following, and false otherwise.
   *
   * @param values the list of numbers to compare. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr GT(Expr... values) {
    return GT(ImmutableList.copyOf(values));
  }

  /**
   * Returns true if the first element of the given collection is greater than or equal to the ones following,
   * and false otherwise.
   *
   * @param values the list of numbers to compare. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr GTE(List<? extends Expr> values) {
    return Fn.apply("gte", varargs(values));
  }

  /**
   * Returns true if the first element of the given collection is greater than or equal to the ones following,
   * and false otherwise.
   *
   * @param values the list of numbers to compare. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr GTE(Expr... values) {
    return GTE(ImmutableList.copyOf(values));
  }

  /**
   * Returns true if all elements in the given collection are true, and false otherwise.
   *
   * @param values a collection of boolean values. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr And(List<? extends Expr> values) {
    return Fn.apply("and", varargs(values));
  }

  /**
   * Returns true if all elements in the given collection are true, and false otherwise.
   *
   * @param values a collection of boolean values. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr And(Expr... values) {
    return And(ImmutableList.copyOf(values));
  }

  /**
   * Returns true if any element in the given collection is true, and false otherwise.
   *
   * @param values a collection of boolean values. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr Or(List<? extends Expr> values) {
    return Fn.apply("or", varargs(values));
  }

  /**
   * Returns true if any element in the given collection is true, and false otherwise.
   *
   * @param values a collection of boolean values. Type: Array
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr Or(Expr... values) {
    return Or(ImmutableList.copyOf(values));
  }

  /**
   * Returns true if the given boolean parameter is false, or false if the boolean parameter is true.
   *
   * @param bool a boolean value. Type: Boolean
   * @return a new {@link Expr} instance
   * @see <a href="https://fauna.com/documentation/queries#logical-functions">FaunaDB Logical Functions</a>
   */
  public static Expr Not(Expr bool) {
    return Fn.apply("not", bool);
  }

}
