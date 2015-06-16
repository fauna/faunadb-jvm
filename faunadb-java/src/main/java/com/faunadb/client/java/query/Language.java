package com.faunadb.client.java.query;

import com.faunadb.client.java.types.Identifier;
import com.faunadb.client.java.types.Value;
import com.faunadb.client.java.types.Value.*;
import com.faunadb.client.java.types.Ref;
import com.faunadb.client.java.types.Var;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public abstract class Language {
  public static NullV NullV() {
    return NullV.Null;
  }

  public static Value.ObjectV ObjectV() {
    return ObjectV.empty();
  }

  public static ObjectV ObjectV(ImmutableMap<String, Value> values) {
    return ObjectV.create(values);
  }

  public static ObjectV ObjectV(String k1, Value v1) {
    return ObjectV.create(k1, v1);
  }

  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2) {
    return ObjectV.create(k1, v1, k2, v2);
  }

  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2, String k3, Value v3) {
    return ObjectV.create(k1, v1, k2, v2, k3, v3);
  }

  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4) {
    return ObjectV.create(k1, v1, k2, v2, k3, v3, k4, v4);
  }

  public static ObjectV ObjectV(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4, String k5, Value v5) {
    return ObjectV.create(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
  }

  public static ArrayV ArrayV() {
    return ArrayV.empty();
  }

  public static ArrayV ArrayV(ImmutableList<Value> values) {
    return ArrayV.create(values);
  }

  public static ArrayV ArrayV(Value v1) {
    return ArrayV.create(v1);
  }

  public static ArrayV ArrayV(Value v1, Value v2) {
    return ArrayV.create(v1, v2);
  }

  public static ArrayV ArrayV(Value v1, Value v2, Value v3) {
    return ArrayV.create(v1, v2, v3);
  }

  public static ArrayV ArrayV(Value v1, Value v2, Value v3, Value v4) {
    return ArrayV.create(v1, v2, v3, v4);
  }

  public static ArrayV ArrayV(Value v1, Value v2, Value v3, Value v4, Value v5) {
    return ArrayV.create(v1, v2, v3, v4, v5);
  }

  public static ArrayV ArrayV(Value v1, Value v2, Value v3, Value v4, Value v5, Value v6) {
    return ArrayV.create(v1, v2, v3, v4, v5, v6);
  }

  public static Ref Ref(String ref) {
    return Ref.create(ref);
  }


  public static StringV StringV(String value) {
    return StringV.create(value);
  }

  public static NumberV NumberV(long value) {
    return NumberV.create(value);
  }

  public static DoubleV DoubleV(double value) {
    return DoubleV.create(value);
  }

  public static BooleanV BooleanV(boolean value) {
    return BooleanV.create(value);
  }

  public static Count Count(Set set) {
    return Count.create(set);
  }

  public static Create Create(Identifier ref) {
    return Create.create(ref);
  }

  public static Create Create(Ref ref) {
    return Create.create(ref);
  }

  public static Create Create(Identifier ref, ObjectV params) {
    return Create.create(ref, params);
  }

  public static Create Create(Ref ref, ObjectV params) {
    return Create.create(ref, params);
  }

  public static Delete Delete(Identifier ref) {
    return Delete.create(ref);
  }

  public static Difference Difference(ImmutableList<Set> sets) {
    return Difference.create(sets);
  }

  public static Do Do(ImmutableList<Expression> expressions) {
    return Do.create(expressions);
  }

  public static Foreach Foreach(Lambda lambda, Expression collection) {
    return Foreach.create(lambda, collection);
  }

  public static Get Get(Identifier resource) {
    return Get.create(resource);
  }

  public static If If(Expression condition, Expression then, Expression elseExpression) {
    return If.create(condition, then, elseExpression);
  }

  public static Intersection Intersection(ImmutableList<Set> sets) {
    return Intersection.create(sets);
  }

  public static Join Join(Set source, Lambda target) {
    return Join.create(source, target);
  }

  public static Lambda Lambda(String argument, Expression expr) {
    return Lambda.create(argument, expr);
  }

  public static Let Let(ImmutableMap<String, Expression> vars, Expression in) {
    return Let.create(vars, in);
  }

  public static Map Map(Lambda lambda, Expression collection) {
    return Map.create(lambda, collection);
  }

  public static Match Match(Ref term, Ref index) {
    return Match.create(term, index);
  }

  public static Match Match(Value term, Ref index) {
    return Match.create(term, index);
  }

  public static Paginate Paginate(Identifier resource) {
    return Paginate.create(resource);
  }

  public static Quote Quote(Expression expression) {
    return Quote.create(expression);
  }

  public static Replace Replace(Identifier ref, ObjectV obj) {
    return Replace.create(ref, obj);
  }

  public static Select Select(ImmutableList<Path> path, Value from) {
    return Select.create(path, from);
  }

  public static Union Union(ImmutableList<Set> sets) {
    return Union.create(sets);
  }

  public static Update Update(Identifier ref, ObjectV params) {
    return Update.create(ref, params);
  }

  public static Var Var(String variable) {
    return Var.create(variable);
  }

  public static Cursor.Before Before(Value value) {
    return Cursor.Before.create(value);
  }

  public static Cursor.After After(Value value) {
    return Cursor.After.create(value);
  }

  public static Add Add(ImmutableList<Expression> terms) {
    return Add.create(terms);
  }
}
