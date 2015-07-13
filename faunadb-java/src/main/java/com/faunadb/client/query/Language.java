package com.faunadb.client.query;

import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.*;
import com.faunadb.client.types.Ref;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
   * Returns the Null value.
   */
  public static NullV NullV() {
    return NullV.Null;
  }

  /**
   * Creates a new Object function, wrapping an empty object value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Object() {
    return ObjectV("object", ObjectV());
  }

  /**
   * Creates a new Object function, wrapping the provided object value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Object(ObjectV value) {
    return ObjectV("object", value);
  }

  /**
   * Returns an empty object value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static Value.ObjectV ObjectV() {
    return ObjectV.empty();
  }

  /**
   * Creates a new object value, wrapping the provided dictionary of values.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ObjectV ObjectV(ImmutableMap<String, Value> values) {
    return ObjectV.create(values);
  }

  /**
   * Creates a new object value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ObjectV ObjectV(String k1, Value v1) {
    return ObjectV.create(k1, v1);
  }

  /**
   * Creates a new object value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2) {
    return ObjectV.create(k1, v1, k2, v2);
  }

  /**
   * Creates a new object value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2, String k3, Value v3) {
    return ObjectV.create(k1, v1, k2, v2, k3, v3);
  }

  /**
   * Creates a new object value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4) {
    return ObjectV.create(k1, v1, k2, v2, k3, v3, k4, v4);
  }

  /**
   * Creates a new object value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4, String k5, Value v5) {
    return ObjectV.create(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
  }

  /**
   * Creates an empty array value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ArrayV ArrayV() {
    return ArrayV.empty();
  }

  /**
   * Creates a new array value containing the provided list of values.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ArrayV ArrayV(ImmutableList<Value> values) {
    return ArrayV.create(values);
  }

  /**
   * Creates a new array value containing the given entry.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ArrayV ArrayV(Value v1) {
    return ArrayV.create(v1);
  }

  /**
   * Creates a new array value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ArrayV ArrayV(Value v1, Value v2) {
    return ArrayV.create(v1, v2);
  }

  /**
   * Creates a new array value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ArrayV ArrayV(Value v1, Value v2, Value v3) {
    return ArrayV.create(v1, v2, v3);
  }

  /**
   * Creates a new array value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ArrayV ArrayV(Value v1, Value v2, Value v3, Value v4) {
    return ArrayV.create(v1, v2, v3, v4);
  }

  /**
   * Creates a new array value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ArrayV ArrayV(Value v1, Value v2, Value v3, Value v4, Value v5) {
    return ArrayV.create(v1, v2, v3, v4, v5);
  }

  /**
   * Creates a new array value containing the given entries.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static ArrayV ArrayV(Value v1, Value v2, Value v3, Value v4, Value v5, Value v6) {
    return ArrayV.create(v1, v2, v3, v4, v5, v6);
  }

  /**
   * Creates a new Ref value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static Ref Ref(String ref) {
    return Ref.create(ref);
  }

  /**
   * Creates a new String value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static StringV StringV(String value) {
    return StringV.create(value);
  }

  /**
   * Creates a new Long value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static LongV LongV(long value) {
    return LongV.create(value);
  }

  /**
   * Create aa new Double value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static DoubleV DoubleV(double value) {
    return DoubleV.create(value);
  }

  /**
   * Creates a new Boolean value.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Values</a></p>
   */
  public static BooleanV BooleanV(boolean value) {
    return BooleanV.create(value);
  }

  /**
   * Creates a new Exists function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-read_functions">FaunaDB Read Functions</a></p>
   */
  public static Value Exists(Value ref) {
    return ObjectV("exists", ref);
  }

  /**
   * Creates a new Count function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-read_functions">FaunaDB Read Functions</a></p>
   */
  public static Value Count(Value set) {
    return ObjectV("count", set);
  }

  /**
   * Creates a new Create function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-write_functions">FaunaDB Write Functions</a></p>
   */
  public static Value Create(Value ref) {
    return ObjectV("create", ref);
  }

  /**
   * Creates a new Create function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-write_functions">FaunaDB Write Functions</a></p>
   */
  public static Value Create(Value ref, Value params) {
    return ObjectV("create", ref, "params", params);
  }

  /**
   * Creates a new Delete function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-write_functions">FaunaDB Write Functions</a></p>
   */
  public static Value Delete(Value ref) {
    return ObjectV("delete", ref);
  }

  /**
   * Creates a new Difference set.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-sets">FaunaDB Set Functions</a></p>
   */
  public static Value Difference(ImmutableList<Value> sets) {
    return ObjectV("difference", ArrayV(sets));
  }

  /**
   * Creates a new Difference set function operating on the given sets.
   */
  public static Value Difference(Value set1) {
    return Difference(ImmutableList.of(set1));
  }

  /**
   * Creates a new Difference set function operating on the given sets.
   */
  public static Value Difference(Value set1, Value set2) {
    return Difference(ImmutableList.of(set1, set2));
  }

  /**
   * Creates a new Difference set function operating on the given sets.
   */
  public static Value Difference(Value set1, Value set2, Value set3) {
    return Difference(ImmutableList.of(set1, set2, set3));
  }

  /**
   * Creates a new Difference set function operating on the given sets.
   */
  public static Value Difference(Value set1, Value set2, Value set3, Value set4) {
    return Difference(ImmutableList.of(set1, set2, set3, set4));
  }

  /**
   * Creates a new Difference set function operating on the given sets.
   */
  public static Value Difference(Value set1, Value set2, Value set3, Value set4, Value set5) {
    return Difference(ImmutableList.of(set1, set2, set3, set4, set5));
  }

  /**
   * Creates a new Difference set function operating on the given sets.
   */
  public static Value Difference(Value set1, Value set2, Value set3, Value set4, Value set5, Value set6) {
    return Difference(ImmutableList.of(set1, set2, set3, set4, set5, set6));
  }

  /**
   * Creates a new Do function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Do(ImmutableList<Value> expressions) {
    return ObjectV("do", ArrayV(expressions));
  }

  /**
   * Creates a new Do function with the given terms.
   */
  public static Value Do(Value expr1) {
    return Do(ImmutableList.of(expr1));
  }

  /**
   * Creates a new Do function with the given terms.
   */
  public static Value Do(Value expr1, Value expr2) {
    return Do(ImmutableList.of(expr1, expr2));
  }

  /**
   * Creates a new Do function with the given terms.
   */
  public static Value Do(Value expr1, Value expr2, Value expr3) {
    return Do(ImmutableList.of(expr1, expr2, expr3));
  }

  /**
   * Creates a new Do function with the given terms.
   */
  public static Value Do(Value expr1, Value expr2, Value expr3, Value expr4) {
    return Do(ImmutableList.of(expr1, expr2, expr3, expr4));
  }

  /**
   * Creates a new Do function with the given terms.
   */
  public static Value Do(Value expr1, Value expr2, Value expr3, Value expr4, Value expr5) {
    return Do(ImmutableList.of(expr1, expr2, expr3, expr4, expr5));
  }

  /**
   * Creates a new Do function with the given terms.
   */
  public static Value Do(Value expr1, Value expr2, Value expr3, Value expr4, Value expr5, Value expr6) {
    return Do(ImmutableList.of(expr1, expr2, expr3, expr4, expr5, expr6));
  }

  /**
   * Creates a new Foreach function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-collection_functions">FaunaDB Collection Functions</a>
   */
  public static Value Foreach(Value lambda, Value collection) {
    return ObjectV("foreach", lambda, "collection", collection);
  }

  /**
   * Creates a new Get function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-read_functions">FaunaDB Read Functions</a></p>
   */
  public static ObjectV Get(Value resource) {
    return ObjectV("get", resource);
  }

  /**
   * Creates a new If function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a>
   */
  public static ObjectV If(Value condition, Value then, Value elseExpression) {
    return ObjectV("if", condition, "then", then, "else", elseExpression);
  }

  /**
   * Creates a new Intersection function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-sets">FaunaDB Set Functions</a></p>
   */
  public static Value Intersection(ImmutableList<Value> sets) {
    return ObjectV("intersection", ArrayV(sets));
  }

  /**
   * Creates a new Intersection set function operating on the given sets.
   */
  public static Value Intersection(Value set1) {
    return Intersection(ImmutableList.of(set1));
  }

  /**
   * Creates a new Intersection set function operating on the given sets.
   */
  public static Value Intersection(Value set1, Value set2) {
    return Intersection(ImmutableList.of(set1, set2));
  }

  /**
   * Creates a new Intersection set function operating on the given sets.
   */
  public static Value Intersection(Value set1, Value set2, Value set3) {
    return Intersection(ImmutableList.of(set1, set2, set3));
  }

  /**
   * Creates a new Intersection set function operating on the given sets.
   */
  public static Value Intersection(Value set1, Value set2, Value set3, Value set4) {
    return Intersection(ImmutableList.of(set1, set2, set3, set4));
  }

  /**
   * Creates a new Intersection set function operating on the given sets.
   */
  public static Value Intersection(Value set1, Value set2, Value set3, Value set4, Value set5) {
    return Intersection(ImmutableList.of(set1, set2, set3, set4, set5));
  }

  /**
   * Creates a new Intersection set function operating on the given sets.
   */
  public static Value Intersection(Value set1, Value set2, Value set3, Value set4, Value set5, Value set6) {
    return Intersection(ImmutableList.of(set1, set2, set3, set4, set5, set6));
  }

  /**
   * Creates a new Join function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-sets">FaunaDB Set Functions</a></p>
   */
  public static Value Join(Value source, Value target) {
    return ObjectV("join", source, "with", target);
  }

  /**
   * Creates a new Lambda function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Lambda(String argument, Value expr) {
    return ObjectV("lambda", StringV(argument), "expr", expr);
  }

  /**
   * Creates a new Let function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Let(ImmutableMap<String, Value> vars, Value in) {
    return ObjectV("let", ObjectV(vars), "in", in);
  }

  /**
   * Creates a new Map function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-collection_functions">FaunaDB Collection Functions</a>
   */
  public static Value Map(Value lambda, Value collection) {
    return ObjectV("map", lambda, "collection", collection);
  }

  /**
   * Creates a new Match function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-sets">FaunaDB Set Functions</a></p>
   */
  public static Value Match(Value term, Ref index) {
    return ObjectV("match", term, "index", index);
  }

  /**
   * Creates a new Paginate function builder.
   *
   * @see PaginateBuilder
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-read_functions">FaunaDB Read Functions</a></p>
   */
  public static PaginateBuilder Paginate(Value resource) {
    return PaginateBuilder.create(resource);
  }

  /**
   * Creates a new Quote function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a>
   */
  public static Value Quote(Value expression) {
    return ObjectV("quote", expression);
  }

  /**
   * Creates a new Replace function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-write_functions">FaunaDB Write Functions</a></p>
   */
  public static Value Replace(Value ref, Value obj) {
    return ObjectV("replace", ref, "params", obj);
  }

  /**
   * Creates a new Select function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Select(ImmutableList<Path> path, Value from) {
    ImmutableList.Builder<Value> pathValueBuilder = ImmutableList.builder();
    for (Path term : path) {
      pathValueBuilder.add(term.value());
    }

    return ObjectV("select", ArrayV(pathValueBuilder.build()), "from", from);
  }

  /**
   * Creates a new Union function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-sets">FaunaDB Set Functions</a></p>
   */
  public static Value Union(ImmutableList<Value> sets) {
    return ObjectV("union", ArrayV(sets));
  }

  /**
   * Creates a new Union function operating on the given sets.
   */
  public static Value Union(Value set1) {
    return Union(ImmutableList.of(set1));
  }

  /**
   * Creates a new Union function operating on the given sets.
   */
  public static Value Union(Value set1, Value set2) {
    return Union(ImmutableList.of(set1, set2));
  }

  /**
   * Creates a new Union function operating on the given sets.
   */
  public static Value Union(Value set1, Value set2, Value set3) {
    return Union(ImmutableList.of(set1, set2, set3));
  }

  /**
   * Creates a new Union function operating on the given sets.
   */
  public static Value Union(Value set1, Value set2, Value set3, Value set4) {
    return Union(ImmutableList.of(set1, set2, set3, set4));
  }

  /**
   * Creates a new Union function operating on the given sets.
   */
  public static Value Union(Value set1, Value set2, Value set3, Value set4, Value set5) {
    return Union(ImmutableList.of(set1, set2, set3, set4, set5));
  }

  /**
   * Creates a new Union function operating on the given sets.
   */
  public static Value Union(Value set1, Value set2, Value set3, Value set4, Value set5, Value set6) {
    return Union(ImmutableList.of(set1, set2, set3, set4, set5, set6));
  }

  /**
   * Creates a new Update function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-write_functions">FaunaDB Write Functions</a></p>
   */
  public static Value Update(Value ref, Value params) {
    return ObjectV("update", ref, "params", params);
  }

  /**
   * Creates a new Var type.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a>
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
   * Creates a new Add function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Add(ImmutableList<Value> terms) {
    return ObjectV("add", ArrayV(terms));
  }

  /**
   * Creates a new Add function operating on the given terms.
   */
  public static Value Add(Value term1) {
    return Add(ImmutableList.of(term1));
  }

  /**
   * Creates a new Add function operating on the given terms.
   */
  public static Value Add(Value term1, Value term2) {
    return Add(ImmutableList.of(term1, term2));
  }

  /**
   * Creates a new Add function operating on the given terms.
   */
  public static Value Add(Value term1, Value term2, Value term3) {
    return Add(ImmutableList.of(term1, term2, term3));
  }

  /**
   * Creates a new Add function operating on the given terms.
   */
  public static Value Add(Value term1, Value term2, Value term3, Value term4) {
    return Add(ImmutableList.of(term1, term2, term3, term4));
  }

  /**
   * Creates a new Add function operating on the given terms.
   */
  public static Value Add(Value term1, Value term2, Value term3, Value term4, Value term5) {
    return Add(ImmutableList.of(term1, term2, term3, term4, term5));
  }

  /**
   * Creates a new Add function operating on the given terms.
   */
  public static Value Add(Value term1, Value term2, Value term3, Value term4, Value term5, Value term6) {
    return Add(ImmutableList.of(term1, term2, term3, term4, term5, term6));
  }

  /**
   * Creates a new Subtract function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Subtract(ImmutableList<Value> terms) {
    return ObjectV("subtract", ArrayV(terms));
  }

  /**
   * Creates a new Subtract function operating on the given terms.
   */
  public static Value Subtract(Value term1) {
    return Subtract(ImmutableList.of(term1));
  }

  /**
   * Creates a new Subtract function operating on the given terms.
   */
  public static Value Subtract(Value term1, Value term2) {
    return Subtract(ImmutableList.of(term1, term2));
  }

  /**
   * Creates a new Subtract function operating on the given terms.
   */
  public static Value Subtract(Value term1, Value term2, Value term3) {
    return Subtract(ImmutableList.of(term1, term2, term3));
  }

  /**
   * Creates a new Subtract function operating on the given terms.
   */
  public static Value Subtract(Value term1, Value term2, Value term3, Value term4) {
    return Subtract(ImmutableList.of(term1, term2, term3, term4));
  }

  /**
   * Creates a new Subtract function operating on the given terms.
   */
  public static Value Subtract(Value term1, Value term2, Value term3, Value term4, Value term5) {
    return Subtract(ImmutableList.of(term1, term2, term3, term4, term5));
  }

  /**
   * Creates a new Subtract function operating on the given terms.
   */
  public static Value Subtract(Value term1, Value term2, Value term3, Value term4, Value term5, Value term6) {
    return Subtract(ImmutableList.of(term1, term2, term3, term4, term5, term6));
  }

  /**
   * Creates a new Divide function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Divide(ImmutableList<Value> terms) {
    return ObjectV("divide", ArrayV(terms));
  }

  /**
   * Creates a new Divide function operating on the given terms.
   */
  public static Value Divide(Value term1) {
    return Divide(ImmutableList.of(term1));
  }

  /**
   * Creates a new Divide function operating on the given terms.
   */
  public static Value Divide(Value term1, Value term2) {
    return Divide(ImmutableList.of(term1, term2));
  }

  /**
   * Creates a new Divide function operating on the given terms.
   */
  public static Value Divide(Value term1, Value term2, Value term3) {
    return Divide(ImmutableList.of(term1, term2, term3));
  }

  /**
   * Creates a new Divide function operating on the given terms.
   */
  public static Value Divide(Value term1, Value term2, Value term3, Value term4) {
    return Divide(ImmutableList.of(term1, term2, term3, term4));
  }

  /**
   * Creates a new Divide function operating on the given terms.
   */
  public static Value Divide(Value term1, Value term2, Value term3, Value term4, Value term5) {
    return Divide(ImmutableList.of(term1, term2, term3, term4, term5));
  }

  /**
   * Creates a new Divide function operating on the given terms.
   */
  public static Value Divide(Value term1, Value term2, Value term3, Value term4, Value term5, Value term6) {
    return Divide(ImmutableList.of(term1, term2, term3, term4, term5, term6));
  }
  /**
   * Creates a new Multiply function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Multiply(ImmutableList<Value> terms) {
    return ObjectV("multiply", ArrayV(terms));
  }

  /**
   * Creates a new Multiply function operating on the given terms.
   */
  public static Value Multiply(Value term1) {
    return Multiply(ImmutableList.of(term1));
  }

  /**
   * Creates a new Multiply function operating on the given terms.
   */
  public static Value Multiply(Value term1, Value term2) {
    return Multiply(ImmutableList.of(term1, term2));
  }

  /**
   * Creates a new Multiply function operating on the given terms.
   */
  public static Value Multiply(Value term1, Value term2, Value term3) {
    return Multiply(ImmutableList.of(term1, term2, term3));
  }

  /**
   * Creates a new Multiply function operating on the given terms.
   */
  public static Value Multiply(Value term1, Value term2, Value term3, Value term4) {
    return Multiply(ImmutableList.of(term1, term2, term3, term4));
  }

  /**
   * Creates a new Multiply function operating on the given terms.
   */
  public static Value Multiply(Value term1, Value term2, Value term3, Value term4, Value term5) {
    return Multiply(ImmutableList.of(term1, term2, term3, term4, term5));
  }

  /**
   * Creates a new Multiply function operating on the given terms.
   */
  public static Value Multiply(Value term1, Value term2, Value term3, Value term4, Value term5, Value term6) {
    return Multiply(ImmutableList.of(term1, term2, term3, term4, term5, term6));
  }

  /**
   * Creates a new Equals function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  }

  /**
   * Creates a new Equals function operating on the given terms.
   */
  public static Value Equals(Value term1) {
    return Equals(ImmutableList.of(term1));
  }

  /**
   * Creates a new Equals function operating on the given terms.
   */
  public static Value Equals(Value term1, Value term2) {
    return Equals(ImmutableList.of(term1, term2));
  }

  /**
   * Creates a new Equals function operating on the given terms.
   */
  public static Value Equals(Value term1, Value term2, Value term3) {
    return Equals(ImmutableList.of(term1, term2, term3));
  }

  /**
   * Creates a new Equals function operating on the given terms.
   */
  public static Value Equals(Value term1, Value term2, Value term3, Value term4) {
    return Equals(ImmutableList.of(term1, term2, term3, term4));
  }

  /**
   * Creates a new Equals function operating on the given terms.
   */
  public static Value Equals(Value term1, Value term2, Value term3, Value term4, Value term5) {
    return Equals(ImmutableList.of(term1, term2, term3, term4, term5));
  }

  /**
   * Creates a new Equals function operating on the given terms.
   */
  public static Value Equals(Value term1, Value term2, Value term3, Value term4, Value term5, Value term6) {
    return Equals(ImmutableList.of(term1, term2, term3, term4, term5, term6));
  }

  /**
   * Creates a new Concat function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Concat(ImmutableList<Value> terms) {
    return ObjectV("concat", ArrayV(terms));
  }

  /**
   * Creates a new Concat function operating on the given terms.
   */
  public static Value Concat(Value term1) {
    return Concat(ImmutableList.of(term1));
  }

  /**
   * Creates a new Concat function operating on the given terms.
   */
  public static Value Concat(Value term1, Value term2) {
    return Concat(ImmutableList.of(term1, term2));
  }

  /**
   * Creates a new Concat function operating on the given terms.
   */
  public static Value Concat(Value term1, Value term2, Value term3) {
    return Concat(ImmutableList.of(term1, term2, term3));
  }

  /**
   * Creates a new Concat function operating on the given terms.
   */
  public static Value Concat(Value term1, Value term2, Value term3, Value term4) {
    return Concat(ImmutableList.of(term1, term2, term3, term4));
  }

  /**
   * Creates a new Concat function operating on the given terms.
   */
  public static Value Concat(Value term1, Value term2, Value term3, Value term4, Value term5) {
    return Concat(ImmutableList.of(term1, term2, term3, term4, term5));
  }

  /**
   * Creates a new Concat function operating on the given terms.
   */
  public static Value Concat(Value term1, Value term2, Value term3, Value term4, Value term5, Value term6) {
    return Concat(ImmutableList.of(term1, term2, term3, term4, term5, term6));
  }

  /**
   * Creates a new Contains function.
   *
   * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a></p>
   */
  public static Value Contains(ImmutableList<Path> path, Value in) {
    ImmutableList.Builder<Value> pathValueBuilder = ImmutableList.builder();
    for (Path term : path) {
      pathValueBuilder.add(term.value());
    }

    return ObjectV("contains", ArrayV(pathValueBuilder.build()), "in", in);
  }

  /**
   * Helper for constructing a Path list with the given path terms.
   *
   * @see Path
   */
  public static ImmutableList<Path> Path(Path term1) {
    return ImmutableList.of(term1);
  }

  /**
   * Helper for constructing a Path list with the given path terms.
   *
   * @see Path
   */
  public static ImmutableList<Path> Path(Path term1, Path term2) {
    return ImmutableList.of(term1, term2);
  }

  /**
   * Helper for constructing a Path list with the given path terms.
   *
   * @see Path
   */
  public static ImmutableList<Path> Path(Path term1, Path term2, Path term3) {
    return ImmutableList.of(term1, term2, term3);
  }

  /**
   * Helper for constructing a Path list with the given path terms.
   *
   * @see Path
   */
  public static ImmutableList<Path> Path(Path term1, Path term2, Path term3, Path term4) {
    return ImmutableList.of(term1, term2, term3, term4);
  }

  /**
   * Helper for constructing a Path list with the given path terms.
   *
   * @see Path
   */
  public static ImmutableList<Path> Path(Path term1, Path term2, Path term3, Path term4, Path term5) {
    return ImmutableList.of(term1, term2, term3, term4, term5);
  }

  /**
   * Helper for constructing a Path list with the given path terms.
   *
   * @see Path
   */
  public static ImmutableList<Path> Path(Path term1, Path term2, Path term3, Path term4, Path term5, Path term6) {
    return ImmutableList.of(term1, term2, term3, term4, term5, term6);
  }
}
