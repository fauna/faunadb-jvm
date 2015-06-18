package com.faunadb.client.java.query;

import com.faunadb.client.java.types.Identifier;
import com.faunadb.client.java.types.Value;
import com.faunadb.client.java.types.Value.*;
import com.faunadb.client.java.types.Ref;
import com.faunadb.client.java.types.Var;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Helper methods for the FaunaDB query language. This class is intended to be statically imported into your code:
 *
 * <p>{@code import static com.faunadb.client.java.query.Language.*;}</p>
 *
 * <p>Calling these static methods is more concise than creating the various data types manually.</p>
 */
public abstract class Language {
  Language() { }

  /**
   * Returns the Null value.
   */
  public static NullV NullV() {
    return NullV.Null;
  }

  /**
   * Creates a new Object function, wrapping an empty object value.
   */
  public static Object Object() {
    return Object.create(ObjectV());
  }

  /**
   * Creates a new Object function, wrapping the provided object value.
   */
  public static Object Object(ObjectV value) {
    return Object.create(value);
  }

  /**
   * Returns an empty object value.
   */
  public static Value.ObjectV ObjectV() {
    return ObjectV.empty();
  }

  /**
   * Creates a new object value, wrapping the provided dictionary of values.
   */
  public static ObjectV ObjectV(ImmutableMap<String, Value> values) {
    return ObjectV.create(values);
  }

  /**
   * Creates a new object value containing the given entries.
   */
  public static ObjectV ObjectV(String k1, Value v1) {
    return ObjectV.create(k1, v1);
  }

  /**
   * Creates a new object value containing the given entries.
   */
  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2) {
    return ObjectV.create(k1, v1, k2, v2);
  }

  /**
   * Creates a new object value containing the given entries.
   */
  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2, String k3, Value v3) {
    return ObjectV.create(k1, v1, k2, v2, k3, v3);
  }

  /**
   * Creates a new object value containing the given entries.
   */
  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4) {
    return ObjectV.create(k1, v1, k2, v2, k3, v3, k4, v4);
  }

  /**
   * Creates a new object value containing the given entries.
   */
  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4, String k5, Value v5) {
    return ObjectV.create(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
  }

  /**
   * Creates an empty array value.
   */
  public static ArrayV ArrayV() {
    return ArrayV.empty();
  }

  /**
   * Creates a new array value containing the provided list of values.
   */
  public static ArrayV ArrayV(ImmutableList<Value> values) {
    return ArrayV.create(values);
  }

  /**
   * Creates a new array value containing the given entry.
   */
  public static ArrayV ArrayV(Value v1) {
    return ArrayV.create(v1);
  }

  /**
   * Creates a new array value containing the given entries.
   */
  public static ArrayV ArrayV(Value v1, Value v2) {
    return ArrayV.create(v1, v2);
  }

  /**
   * Creates a new array value containing the given entries.
   */
  public static ArrayV ArrayV(Value v1, Value v2, Value v3) {
    return ArrayV.create(v1, v2, v3);
  }

  /**
   * Creates a new array value containing the given entries.
   */
  public static ArrayV ArrayV(Value v1, Value v2, Value v3, Value v4) {
    return ArrayV.create(v1, v2, v3, v4);
  }

  /**
   * Creates a new array value containing the given entries.
   */
  public static ArrayV ArrayV(Value v1, Value v2, Value v3, Value v4, Value v5) {
    return ArrayV.create(v1, v2, v3, v4, v5);
  }

  /**
   * Creates a new array value containing the given entries.
   */
  public static ArrayV ArrayV(Value v1, Value v2, Value v3, Value v4, Value v5, Value v6) {
    return ArrayV.create(v1, v2, v3, v4, v5, v6);
  }

  /**
   * Creates a new Ref value.
   */
  public static Ref Ref(String ref) {
    return Ref.create(ref);
  }

  /**
   * Creates a new String value.
   */
  public static StringV StringV(String value) {
    return StringV.create(value);
  }

  /**
   * Creates a new Number value.
   */
  public static NumberV NumberV(long value) {
    return NumberV.create(value);
  }

  /**
   * Create aa new Double value.
   */
  public static DoubleV DoubleV(double value) {
    return DoubleV.create(value);
  }

  /**
   * Creates a new Boolean value.
   */
  public static BooleanV BooleanV(boolean value) {
    return BooleanV.create(value);
  }

  /**
   * Creates a new Count function.
   */
  public static Count Count(Set set) {
    return Count.create(set);
  }

  /**
   * Creates a new Create function.
   *
   * @see Create#create(Identifier)
   */
  public static Create Create(Identifier ref) {
    return Create.create(ref);
  }

  /**
   * Creates a new Create function.
   *
   * @see Create#create(Identifier, Expression)
   */
  public static Create Create(Identifier ref, Expression params) {
    return Create.create(ref, params);
  }

  /**
   * Creates a new Delete function.
   *
   * @see Delete#create(Identifier)
   */
  public static Delete Delete(Identifier ref) {
    return Delete.create(ref);
  }

  /**
   * Creates a new Difference set.
   *
   * @see Difference#create
   */
  public static Difference Difference(ImmutableList<Set> sets) {
    return Difference.create(sets);
  }

  /**
   * Creates a new Do function.
   *
   * @see Do#create(ImmutableList)
   */
  public static Do Do(ImmutableList<Expression> expressions) {
    return Do.create(expressions);
  }

  /**
   * Creates a new Foreach function.
   *
   * @see Foreach#create(Lambda, Expression)
   */
  public static Foreach Foreach(Lambda lambda, Expression collection) {
    return Foreach.create(lambda, collection);
  }

  /**
   * Creates a new Get function.
   *
   * @see Get#create(Identifier)
   */
  public static Get Get(Identifier resource) {
    return Get.create(resource);
  }

  /**
   * Creates a new If function.
   *
   * @see If#create
   */
  public static If If(Expression condition, Expression then, Expression elseExpression) {
    return If.create(condition, then, elseExpression);
  }

  /**
   * Obtains a new Intersection set representation.
   *
   * @see Intersection#create(ImmutableList)
   */
  public static Intersection Intersection(ImmutableList<Set> sets) {
    return Intersection.create(sets);
  }

  /**
   * Obtains a new Join set representation.
   *
   * @see Join#create(Set, Lambda)
   */
  public static Join Join(Set source, Lambda target) {
    return Join.create(source, target);
  }

  /**
   * Obtains a new Lambda expression representation.
   *
   * @see Lambda#create(String, Expression)
   */
  public static Lambda Lambda(String argument, Expression expr) {
    return Lambda.create(argument, expr);
  }

  /**
   * Obtains a new Let expression representation.
   *
   * @see Let#create(ImmutableMap, Expression)
   */
  public static Let Let(ImmutableMap<String, Expression> vars, Expression in) {
    return Let.create(vars, in);
  }

  /**
   * Obtains a new Map function representation.
   *
   * @see Map#create(Lambda, Expression)
   */
  public static Map Map(Lambda lambda, Expression collection) {
    return Map.create(lambda, collection);
  }

  /**
   * Obtains a new Match set representation.
   *
   * @see Match#create(Value, Ref)
   */
  public static Match Match(Value term, Ref index) {
    return Match.create(term, index);
  }

  /**
   * Obtains a new Paginate function representation.
   *
   * @see Paginate#create(Identifier)
   */
  public static Paginate Paginate(Identifier resource) {
    return Paginate.create(resource);
  }

  /**
   * Obtains a new Quote function representation.
   *
   * @see Quote#create(Expression)
   */
  public static Quote Quote(Expression expression) {
    return Quote.create(expression);
  }

  /**
   * Obtains a new Replace function representation.
   *
   * @see Replace#create(Identifier, Expression)
   */
  public static Replace Replace(Identifier ref, Expression obj) {
    return Replace.create(ref, obj);
  }

  /**
   * Obtains a new Select function representation.
   *
   * @see Select#create(ImmutableList, Value)
   */
  public static Select Select(ImmutableList<Path> path, Value from) {
    return Select.create(path, from);
  }

  /**
   * Obtains a new Union set representation.
   *
   * @see Union#create(ImmutableList)
   */
  public static Union Union(ImmutableList<Set> sets) {
    return Union.create(sets);
  }

  /**
   * Obtains a new Update function representation.
   *
   * @see Update#create(Identifier, Expression)
   */
  public static Update Update(Identifier ref, Expression params) {
    return Update.create(ref, params);
  }

  /**
   * Obtains a new Var expression representation.
   *
   * @see Var#create(String)
   */
  public static Var Var(String variable) {
    return Var.create(variable);
  }

  /**
   * Obtains a new Before cursor representation.
   *
   * @see Cursor.Before#create(Value)
   */
  public static Cursor.Before Before(Value value) {
    return Cursor.Before.create(value);
  }

  /**
   * Obtains a new After cursor representation.
   *
   * @see Cursor.After#create
   */
  public static Cursor.After After(Value value) {
    return Cursor.After.create(value);
  }

  public static Add Add(ImmutableList<Expression> terms) {
    return Add.create(terms);
  }
}
