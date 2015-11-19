package com.faunadb.client.query;

import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.*;
import com.faunadb.client.types.Ref;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Helper methods for the FaunaDB query language. This class is intended to be statically imported into your code:
 *
 * <p>{@code import static com.faunadb.client.java.query.Language.*;}</p>
 *
 * <p>Each of these helper methods constructs a {@link Value}, which can then be composed with other helper methods.
 *
 * <h3>Examples:</h3>
 * <pre>{@code Value existsValue = Exists(Ref("some/ref"));
 * Value createValue = Create(Ref("classes/some_class"), Quote(ObjectV("data", ObjectV("some", StringV("field")))));
 * }</pre>
 */
public final class Language {
  Language() { }

  /**
   * Enumeration for time units. Used by <a href="https://faunadb.com/documentation/queries#time_functions">FaunaDB Time Functions</a>.
   */
  public static final class TimeUnit {
    private final String value;

    public final String getValue() {
      return value;
    }

    TimeUnit(String value) {
      this.value = value;
    }

    public final static TimeUnit SECOND = new TimeUnit("second");
    public final static TimeUnit MILLISECOND = new TimeUnit("millisecond");
    public final static TimeUnit MICROSECOND = new TimeUnit("microsecond");
    public final static TimeUnit NANOSECOND = new TimeUnit("nanosecond");
  }

  /**
   * Enumeration for event action types.
   */
  public static final class Action {
    private final String value;
    public final String getValue() { return value; }

    Action(String value) {
      this.value = value;
    }

    public final static Action CREATE = new Action("create");
    public final static Action DELETE = new Action("delete");
  }

  /**
   * Returns the Null value.
   */
  public static NullV NullV() {
    return NullV.Null;
  }

  /**
   * Creates a new Object expression, wrapping an empty object value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Object() {
    return ObjectV("object", ObjectV());
  }

  /**
   * Creates a new Object expression, wrapping the provided object value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Object(ObjectV value) {
    return ObjectV("object", value);
  }

  /**
   * Returns an empty object value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static Value.ObjectV ObjectV() {
    return ObjectV.empty();
  }

  /**
   * Creates a new object value, wrapping the provided dictionary of values.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static ObjectV ObjectV(ImmutableMap<String, Value> values) {
    return ObjectV.create(values);
  }

  /**
   * Creates a new object value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static ObjectV ObjectV(String k1, Value v1) {
    return ObjectV.create(k1, v1);
  }

  /**
   * Creates a new object value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2) {
    return ObjectV.create(k1, v1, k2, v2);
  }

  /**
   * Creates a new object value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2, String k3, Value v3) {
    return ObjectV.create(k1, v1, k2, v2, k3, v3);
  }

  /**
   * Creates a new object value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4) {
    return ObjectV.create(k1, v1, k2, v2, k3, v3, k4, v4);
  }

  /**
   * Creates a new object value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4, String k5, Value v5) {
    return ObjectV.create(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
  }

  /**
   * Creates an empty array value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static ArrayV ArrayV() {
    return ArrayV.empty();
  }

  /**
   * Creates a new array value containing the provided list of values.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static ArrayV ArrayV(ImmutableList<Value> values) {
    return ArrayV.create(values);
  }

  /**
   * Creates a new array value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static ArrayV ArrayV(Value... values) {
    return ArrayV.create(ImmutableList.copyOf(values));
  }

  /**
   * Creates a new Ref value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static Ref Ref(String ref) {
    return Ref.create(ref);
  }

  /**
   * Creates a new String value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static StringV StringV(String value) {
    return StringV.create(value);
  }

  /**
   * Creates a new Long value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static LongV LongV(long value) {
    return LongV.create(value);
  }

  /**
   * Create aa new Double value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static DoubleV DoubleV(double value) {
    return DoubleV.create(value);
  }

  /**
   * Creates a new Boolean value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static BooleanV BooleanV(boolean value) {
    return BooleanV.create(value);
  }

  /**
   * Creates a new Timestamp value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static TsV TsV(Instant value) {
    return TsV.create(value);
  }

  /**
   * Creates a new Date value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#values">FaunaDB Values</a></p>
   */
  public static DateV DateV(LocalDate value) {
    return DateV.create(value);
  }

  /**
   * Creates a new Exists expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a></p>
   */
  public static Value Exists(Value ref) {
    return ObjectV("exists", ref);
  }

  /**
   * Creates a new Count expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a></p>
   */
  public static Value Count(Value set) {
    return ObjectV("count", set);
  }

  /**
   * Creates a new Create expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a></p>
   */
  public static Value Create(Value ref) {
    return ObjectV("create", ref);
  }

  /**
   * Creates a new Create expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a></p>
   */
  public static Value Create(Value ref, Value params) {
    return ObjectV("create", ref, "params", params);
  }

  /**
   * Creates a new Insert expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a></p>
   */
  public static Value Insert(Value ref, long ts, Action action, Value params) {
    return ObjectV("insert", ref, "ts", LongV(ts), "action", StringV(action.getValue()), "params", params);
  }

  /**
   * Creates a new Remove expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a></p>
   */
  public static Value Remove(Value ref, long ts, Action action) {
    return ObjectV("remove", ref, "ts", LongV(ts), "action", StringV(action.getValue()));
  }

  /**
   * Creates a new Delete expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a></p>
   */
  public static Value Delete(Value ref) {
    return ObjectV("delete", ref);
  }

  /**
   * Creates a new Difference set.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a></p>
   */
  public static Value Difference(ImmutableList<Value> sets) {
    return ObjectV("difference", ArrayV(sets));
  }

  /**
   * Creates a new Difference set expression operating on the given sets.
   */
  public static Value Difference(Value... sets) {
    return Difference(ImmutableList.copyOf(sets));
  }

  /**
   * Creates a new Do expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Do(ImmutableList<Value> expressions) {
    return ObjectV("do", ArrayV(expressions));
  }

  /**
   * Creates a new Do expression with the given terms.
   */
  public static Value Do(Value... exprs) {
    return Do(ImmutableList.copyOf(exprs));
  }

  /**
   * Creates a new Foreach expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Value Foreach(Value lambda, Value collection) {
    return ObjectV("foreach", lambda, "collection", collection);
  }

  /**
   * Creates a new Filter expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Value Filter(Value lambda, Value collection) {
    return ObjectV("filter", lambda, "collection", collection);
  }

  /**
   * Creates a new Take expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Value Take(Value num, Value collection) {
    return ObjectV("take", num, "collection", collection);
  }

  /**
   * Creates a new Drop expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Value Drop(Value num, Value collection) {
    return ObjectV("drop", num, "collection", collection);
  }

  /**
   * Creates a new Prepend expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Value Prepend(Value elems, Value collection) {
    return ObjectV("prepend", elems, "collection", collection);
  }

  /**
   * Creates a new Append expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Value Append(Value elems, Value collection) {
    return ObjectV("append", elems, "collection", collection);
  }

  /**
   * Creates a new Get expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a></p>
   */
  public static ObjectV Get(Value resource) {
    return ObjectV("get", resource);
  }

  /**
   * Creates a new If expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static ObjectV If(Value condition, Value then, Value elseExpression) {
    return ObjectV("if", condition, "then", then, "else", elseExpression);
  }

  /**
   * Creates a new Intersection expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a></p>
   */
  public static Value Intersection(ImmutableList<Value> sets) {
    return ObjectV("intersection", ArrayV(sets));
  }

  /**
   * Creates a new Intersection set expression operating on the given sets.
   */
  public static Value Intersection(Value... sets) {
    return Intersection(ImmutableList.copyOf(sets));
  }

  /**
   * Creates a new Join expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a></p>
   */
  public static Value Join(Value source, Value target) {
    return ObjectV("join", source, "with", target);
  }

  /**
   * Creates a new Lambda expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Lambda(String argument, Value expr) {
    return ObjectV("lambda", StringV(argument), "expr", expr);
  }

  /**
   * Creates a new Let expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Let(ImmutableMap<String, Value> vars, Value in) {
    return ObjectV("let", ObjectV(vars), "in", in);
  }

  /**
   * Creates a new Map expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#collection_functions">FaunaDB Collection Functions</a>
   */
  public static Value Map(Value lambda, Value collection) {
    return ObjectV("map", lambda, "collection", collection);
  }

  /**
   * Creates a new Match expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a></p>
   */
  public static Value Match(Value term, Ref index) {
    return ObjectV("match", term, "index", index);
  }

  /**
   * Creates a new Paginate expression builder.
   *
   * @see PaginateBuilder
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#read_functions">FaunaDB Read Functions</a></p>
   */
  public static PaginateBuilder Paginate(Value resource) {
    return PaginateBuilder.create(resource);
  }

  /**
   * Creates a new Quote expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Quote(Value expression) {
    return ObjectV("quote", expression);
  }

  /**
   * Creates a new Replace expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a></p>
   */
  public static Value Replace(Value ref, Value obj) {
    return ObjectV("replace", ref, "params", obj);
  }

  /**
   * Creates a new Select expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Select(ImmutableList<Path> path, Value from) {
    ImmutableList.Builder<Value> pathValueBuilder = ImmutableList.builder();
    for (Path term : path) {
      pathValueBuilder.add(term.value());
    }

    return ObjectV("select", ArrayV(pathValueBuilder.build()), "from", from);
  }

  /**
   * Creates a new Union expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#sets">FaunaDB Set Functions</a></p>
   */
  public static Value Union(ImmutableList<Value> sets) {
    return ObjectV("union", ArrayV(sets));
  }

  /**
   * Creates a new Union expression operating on the given sets.
   */
  public static Value Union(Value... sets) {
    return Union(ImmutableList.copyOf(sets));
  }

  /**
   * Creates a new Update expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#write_functions">FaunaDB Write Functions</a></p>
   */
  public static Value Update(Value ref, Value params) {
    return ObjectV("update", ref, "params", params);
  }

  /**
   * Creates a new Var expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Var(String variable) {
    return ObjectV("var", StringV(variable));
  }

  /**
   * Creates a new Before cursor.
   *
   * @see Cursor
   */
  public static Cursor.Before Before(Value value) {
    return Cursor.Before.create(value);
  }

  /**
   * Creates a new After cursor.
   *
   * @see Cursor
   */
  public static Cursor.After After(Value value) {
    return Cursor.After.create(value);
  }

  /**
   * Creates a new Add expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Add(ImmutableList<Value> terms) {
    return ObjectV("add", ArrayV(terms));
  }

  /**
   * Creates a new Add expression operating on the given terms.
   */
  public static Value Add(Value... terms) {
    return Add(ImmutableList.copyOf(terms));
  }

  /**
   * Creates a new Subtract expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Subtract(ImmutableList<Value> terms) {
    return ObjectV("subtract", ArrayV(terms));
  }

  /**
   * Creates a new Subtract expression operating on the given terms.
   */
  public static Value Subtract(Value... terms) {
    return Subtract(ImmutableList.copyOf(terms));
  }
  /**
   * Creates a new Divide expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Divide(ImmutableList<Value> terms) {
    return ObjectV("divide", ArrayV(terms));
  }

  /**
   * Creates a new Divide expression operating on the given terms.
   */
  public static Value Divide(Value... terms) {
    return Divide(ImmutableList.copyOf(terms));
  }

  /**
   * Creates a new Multiply expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Multiply(ImmutableList<Value> terms) {
    return ObjectV("multiply", ArrayV(terms));
  }

  /**
   * Creates a new Multiply expression operating on the given terms.
   */
  public static Value Multiply(Value... terms) {
    return Multiply(ImmutableList.copyOf(terms));
  }

  /**
   * Creates a new Modulo expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Modulo(ImmutableList<Value> terms) {
    return ObjectV("modulo", ArrayV(terms));
  }

  /**
   * Creates a new Modulo expression operating on the given terms.
   */
  public static Value Modulo(Value... terms) {
    return Modulo(ImmutableList.copyOf(terms));
  }

  /**
   * Creates a new And expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value And(ImmutableList<Value> terms) {
    return ObjectV("and", ArrayV(terms));
  }

  /**
   * Creates a new And expression operating on the given terms.
   */
  public static Value And(Value... terms) {
    return And(ImmutableList.copyOf(terms));
  }

  /**
   * Creates a new Or expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Or(ImmutableList<Value> terms) {
    return ObjectV("or", ArrayV(terms));
  }

  /**
   * Creates a new Or expression operating on the given terms.
   */
  public static Value Or(Value... terms) {
    return Or(ImmutableList.copyOf(terms));
  }

  /**
   * Creates a new Not expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Not(Value term) {
    return ObjectV("not", term);
  }

  /**
   * Creates a new Equals expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Equals(ImmutableList<Value> terms) {
    return ObjectV("equals", ArrayV(terms));
  }

  /**
   * Creates a new Equals expression operating on the given terms.
   */
  public static Value Equals(Value... terms) {
    return Equals(ImmutableList.copyOf(terms));
  }

  /**
   * Creates a new Concat expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Concat(ImmutableList<Value> terms) {
    return ObjectV("concat", ArrayV(terms));
  }

  public static Value Concat(ImmutableList<Value> terms, String separator) {
    return ObjectV("concat", ArrayV(terms), "separator", StringV(separator));
  }

  /**
   * Creates a new Concat expression operating on the given terms.
   */
  public static Value Concat(Value... terms) {
    return Concat(ImmutableList.copyOf(terms));
  }

  /**
   * Creates a new Contains expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Contains(ImmutableList<Path> path, Value in) {
    ImmutableList.Builder<Value> pathValueBuilder = ImmutableList.builder();
    for (Path term : path) {
      pathValueBuilder.add(term.value());
    }

    return ObjectV("contains", ArrayV(pathValueBuilder.build()), "in", in);
  }

  /**
   * Creates a new Login expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#auth_functions">FaunaDB Authentication Functions</a></p>
   */
  public static Value Login(Value ref, Value params) {
    return ObjectV("login", ref, "params", params);
  }

  /**
   * Creates a new Logout expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#auth_functions">FaunaDB Authentication Functions</a></p>
   */
  public static Value Logout(boolean invalidateAll) {
    return ObjectV("logout", BooleanV(invalidateAll));
  }

  /**
   * Creates a new Identify expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#auth_functions">FaunaDB Authentication Functions</a></p>
   */
  public static Value Identify(Value ref, Value password) {
    return ObjectV("identify", ref, "password", password);
  }

  /**
   * Creates a new Time expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#time_functions">FaunaDB Time and Date Functions</a></p>
   */
  public static Value Time(Value time) {
    return ObjectV("time", time);
  }

  /**
   * Creates a new Epoch expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#time_functions">FaunaDB Time and Date Functions</a></p>
   */
  public static Value Epoch(Value num, TimeUnit unit) {
    return ObjectV("epoch", num, "unit", StringV(unit.getValue()));
  }

  /**
   * Creates a new Epoch expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#time_functions">FaunaDB Time and Date Functions</a></p>
   */
  public static Value Epoch(Value num, String unit) {
    return ObjectV("epoch", num, "unit", StringV(unit));
  }

  /**
   * Creates a new Date expression.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation/queries#time_functions">FaunaDB Time and Date Functions</a></p>
   */
  public static Value Date(Value date) {
    return ObjectV("date", date);
  }

  /**
   * Helper for constructing a Path list with the given path terms.
   *
   * @see Path
   */
  public static ImmutableList<Path> Path(Path... terms) {
    return ImmutableList.copyOf(terms);
  }
}
