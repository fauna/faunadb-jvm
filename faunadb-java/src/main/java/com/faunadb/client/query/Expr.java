package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonValue;
import com.faunadb.client.types.Result;
import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.ArrayV;
import com.faunadb.client.types.Value.NullV;
import com.faunadb.client.types.Value.ObjectV;
import com.faunadb.client.types.Value.ScalarValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

import static com.faunadb.client.types.Codec.ARRAY;
import static com.faunadb.client.types.Codec.OBJECT;
import static java.lang.String.format;

/**
 * A query language expression. Constructors for this class are at the {@link Language} class.
 */
public abstract class Expr {

  private static final Expr NULL = new ConcreteExpr(NullV.NULL);

  static class ConcreteExpr extends Expr {
    private final Value value;

    ConcreteExpr(Value value) {
      this.value = value;
    }

    @Override
    @JsonValue
    protected Value value() {
      return value;
    }
  }

  public static Expr wrap(Expr value) {
    if (value instanceof ConcreteExpr) return value;
    if (value instanceof ScalarValue) return new ConcreteExpr((Value) value);
    if (value == null || value instanceof NullV) return NULL;
    if (value instanceof Value) return escape((Value) value);

    return new ConcreteExpr(value.value());
  }

  private static Expr escape(Value value) {
    Result<ImmutableMap<String, Value>> object = value.as(OBJECT);
    if (object.isSuccess())
      return escapedObject(object.get());

    Result<ImmutableList<Value>> array = value.as(ARRAY);
    if (array.isSuccess())
      return escapedArray(array.get());

    return new ConcreteExpr(value);
  }

  static Expr escapedObject(Map<String, ? extends Expr> obj) {
    return new ConcreteExpr(new ObjectV(ImmutableMap.of(
      "object", wrapValues(obj)
    )));
  }

  static ObjectV wrapValues(Map<String, ? extends Expr> obj) {
    ImmutableMap.Builder<String, Value> values = ImmutableMap.builder();

    for (Map.Entry<String, ? extends Expr> kv : obj.entrySet())
      values.put(kv.getKey(), wrap(kv.getValue()).value());

    return new ObjectV(values.build());
  }

  static Expr escapedArray(List<? extends Expr> arr) {
    ImmutableList.Builder<Value> values = ImmutableList.builder();

    for (Expr value : arr)
      values.add(wrap(value).value());

    return new ConcreteExpr(new ArrayV(values.build()));
  }

  protected abstract Value value();

  @Override
  public boolean equals(Object obj) {
    return obj != null &&
      (obj instanceof Expr) &&
      value().equals(((Expr) obj).value());
  }

  @Override
  public int hashCode() {
    return value().hashCode();
  }

  @Override
  public String toString() {
    return format("Expr(%s)", value());
  }

}
