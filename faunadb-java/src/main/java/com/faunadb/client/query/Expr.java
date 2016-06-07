package com.faunadb.client.query;

import com.faunadb.client.types.Value;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

/**
 * A query language expression. Constructors for this class are at the {@link Language} class.
 */
public abstract class Expr {

  static ImmutableMap<String, Value> unwrapValues(Map<String, ? extends Expr> obj) {
    ImmutableMap.Builder<String, Value> values = ImmutableMap.builder();

    for (Map.Entry<String, ? extends Expr> kv : obj.entrySet())
      values.put(kv.getKey(), unwrapValue(kv.getValue()));

    return values.build();
  }

  static ImmutableList<Value> unwrapValues(List<? extends Expr> arr) {
    ImmutableList.Builder<Value> values = ImmutableList.builder();

    for (Expr value : arr)
      values.add(unwrapValue(value));

    return values.build();
  }

  private static Value unwrapValue(Expr value) {
    if (value instanceof Value)
      return (Value) value;

    throw new IllegalArgumentException("Can not create an Expr from value: " + value);
  }

  // To be used by Jackson
  protected abstract Object toJson();

  @Override
  public String toString() {
    return String.format("%s(%s)", getClass().getSimpleName(), toJson());
  }
}
