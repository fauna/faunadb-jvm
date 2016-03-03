package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonValue;
import com.faunadb.client.types.Ref;
import com.faunadb.client.types.SetRef;
import com.faunadb.client.types.Value;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static java.lang.String.format;

/**
 * A query language expression. Constructors for this class are at the {@link Language} class.
 */
public abstract class Expr implements Value {

  static class Literal extends Expr {
    Literal(Value tree) {
      super(tree);
    }

    @Override
    Value unwrap() {
      return tree;
    }
  }

  private static class EscapedObject extends Expr {
    private EscapedObject(Value tree) {
      super(new ObjectV(ImmutableMap.of("object", tree)));
    }

    @Override
    Value unwrap() {
      return tree.get("object");
    }
  }

  public static Expr create(Value expr) {
    if (expr == null) return new Literal(NullV.NULL);
    if (expr instanceof ObjectV) return escapedObject((ObjectV) expr);
    if (expr instanceof ArrayV) return escapedArray((ArrayV) expr);
    if (expr instanceof Expr) return (Expr) expr;

    return new Literal(expr);
  }

  private static Expr escapedObject(ObjectV obj) {
    ImmutableMap.Builder<String, Value> values = ImmutableMap.builder();
    for (Map.Entry<String, Value> kv : obj.asObject().entrySet())
      values.put(kv.getKey(), Expr.create(kv.getValue()));

    return new EscapedObject(new ObjectV(values.build()));
  }

  private static Expr escapedArray(ArrayV arr) {
    ImmutableList.Builder<Expr> values = ImmutableList.builder();
    for (Value value : arr.asArray())
      values.add(Expr.create(value));

    return new Literal(new ArrayV(values.build()));
  }

  final Value tree;

  private Expr(Value tree) {
    this.tree = tree;
  }

  @JsonValue
  private Value tree() {
    return tree;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null &&
      (obj instanceof Expr) &&
      tree.equals(((Expr) obj).tree);
  }

  @Override
  public int hashCode() {
    return tree.hashCode();
  }

  @Override
  public String toString() {
    return format("Expr(%s)", tree);
  }

  // Unwraps inner value for consistent coersion
  abstract Value unwrap();

  @Override
  public String asString() {
    return unwrap().asString();
  }

  @Override
  public Optional<String> asStringOption() {
    return unwrap().asStringOption();
  }

  @Override
  public Boolean asBoolean() {
    return unwrap().asBoolean();
  }

  @Override
  public Optional<Boolean> asBooleanOption() {
    return unwrap().asBooleanOption();
  }

  @Override
  public Long asLong() {
    return unwrap().asLong();
  }

  @Override
  public Optional<Long> asLongOption() {
    return unwrap().asLongOption();
  }

  @Override
  public Double asDouble() {
    return unwrap().asDouble();
  }

  @Override
  public Optional<Double> asDoubleOption() {
    return unwrap().asDoubleOption();
  }

  @Override
  public Instant asTs() {
    return unwrap().asTs();
  }

  @Override
  public Optional<Instant> asTsOption() {
    return unwrap().asTsOption();
  }

  @Override
  public LocalDate asDate() {
    return unwrap().asDate();
  }

  @Override
  public Optional<LocalDate> asDateOption() {
    return unwrap().asDateOption();
  }

  @Override
  public ImmutableList<Value> asArray() {
    return unwrap().asArray();
  }

  @Override
  public Optional<ImmutableList<Value>> asArrayOption() {
    return unwrap().asArrayOption();
  }

  @Override
  public ImmutableMap<String, Value> asObject() {
    return unwrap().asObject();
  }

  @Override
  public Optional<ImmutableMap<String, Value>> asObjectOption() {
    return unwrap().asObjectOption();
  }

  @Override
  public Ref asRef() {
    return unwrap().asRef();
  }

  @Override
  public Optional<Ref> asRefOption() {
    return unwrap().asRefOption();
  }

  @Override
  public SetRef asSetRef() {
    return unwrap().asSetRef();
  }

  @Override
  public Optional<SetRef> asSetRefOption() {
    return unwrap().asSetRefOption();
  }

  @Override
  public Value get(String key) {
    return unwrap().get(key);
  }

  @Override
  public Value get(String... keys) {
    return unwrap().get(keys);
  }

  @Override
  public Optional<Value> getOption(String key) {
    return unwrap().getOption(key);
  }

  @Override
  public Optional<Value> getOption(String... keys) {
    return unwrap().getOption(keys);
  }

  @Override
  public Value get(int index) {
    return unwrap().get(index);
  }

  @Override
  public Optional<Value> getOption(int index) {
    return unwrap().getOption(index);
  }

}
