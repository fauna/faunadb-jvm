package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.NullNode;
import com.faunadb.client.java.types.Ref;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public interface Value extends Expression {
  public static class ObjectV implements Value {
    private final ImmutableMap<String, Value> values;

    public static ObjectV empty() {
      return new ObjectV(ImmutableMap.<String, Value>of());
    }

    public static ObjectV create(String k1, Value v1) {
      return new ObjectV(ImmutableMap.<String, Value>of(k1, v1));
    }

    public static ObjectV create(String k1, Value v1, String k2, Value v2) {
      return new ObjectV(ImmutableMap.<String, Value>of(k1, v1, k2, v2));
    }

    public static ObjectV create(String k1, Value v1, String k2, Value v2, String k3, Value v3) {
      return new ObjectV(ImmutableMap.<String, Value>of(k1, v1, k2, v2, k3, v3));
    }

    public static ObjectV create(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4) {
      return new ObjectV(ImmutableMap.<String, Value>of(k1, v1, k2, v2, k3, v3, k4, v4));
    }

    public static ObjectV create(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4, String k5, Value v5) {
      return new ObjectV(ImmutableMap.<String, Value>of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
    }

    public static ObjectV create(ImmutableMap<String, Value> values) {
      return new ObjectV(values);
    }

    ObjectV(ImmutableMap<String, Value> values) {
      this.values = values;
    }

    @JsonProperty("object")
    public ImmutableMap<String, Value> values() {
      return values;
    }

    @Override
    public int hashCode() {
      return values.hashCode();
    }
  }

  public static class ArrayV implements Value {
    private final ImmutableList<Value> values;

    public static ArrayV empty() {
      return new ArrayV(ImmutableList.<Value>of());
    }

    public static ArrayV create(Value v1) {
      return new ArrayV(ImmutableList.<Value>of(v1));
    }

    public static ArrayV create(Value v1, Value v2) {
      return new ArrayV(ImmutableList.<Value>of(v1, v2));
    }

    public static ArrayV create(Value v1, Value v2, Value v3) {
      return new ArrayV(ImmutableList.<Value>of(v1, v2, v3));
    }

    public static ArrayV create(Value v1, Value v2, Value v3, Value v4) {
      return new ArrayV(ImmutableList.<Value>of(v1, v2, v3, v4));
    }

    public static ArrayV create(Value v1, Value v2, Value v3, Value v4, Value v5) {
      return new ArrayV(ImmutableList.<Value>of(v1, v2, v3, v4, v5));
    }

    public static ArrayV create(Value v1, Value v2, Value v3, Value v4, Value v5, Value v6) {
      return new ArrayV(ImmutableList.<Value>of(v1, v2, v3, v4, v5, v6));
    }

    public static ArrayV create(ImmutableList<Value> values) {
      return new ArrayV(values);
    }

    ArrayV(ImmutableList<Value> values) {
      this.values = values;
    }

    @JsonValue
    public ImmutableList<Value> values() {
      return values;
    }

    @Override
    public int hashCode() {
      return values.hashCode();
    }
  }

  public static class BooleanV implements Value {
    private final Boolean value;

    public final static BooleanV True = BooleanV.create(true);
    public final static BooleanV False = BooleanV.create(false);

    public static BooleanV create(boolean value) {
      return new BooleanV(value);
    }

    BooleanV(boolean value) {
      this.value = value;
    }

    @JsonValue
    public boolean value() {
      return value;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }
  }

  public static class DoubleV implements Value {
    private final Double value;

    public static DoubleV create(double value) {
      return new DoubleV(value);
    }

    DoubleV(double value) {
      this.value = value;
    }

    @JsonValue
    public double value() {
      return value;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }
  }

  public static class NumberV implements Value {
    private final Long value;

    public static NumberV create(long value) {
      return new NumberV(value);
    }

    NumberV(long value) {
      this.value = value;
    }

    @JsonValue
    public long value() {
      return value;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }
  }

  public static class StringV implements Value {
    private final String value;

    public static StringV create(String value) {
      return new StringV(value);
    }

    StringV(String value) {
      this.value = value;
    }

    @JsonValue
    public String value() {
      return value;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }
  }

  public static class RefV implements Value, Identifier {
    private final Ref value;

    public static RefV create(String value) {
      return new RefV(Ref.create(value));
    }

    public static RefV create(Ref value) {
      return new RefV(value);
    }

    RefV(Ref value) {
      this.value = value;
    }

    @JsonValue
    public Ref ref() {
      return value;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }
  }

  public static class VarV implements Value, Identifier {
    public static VarV create(String value) {
      return new VarV(value);
    }

    @JsonProperty("var")
    private final String value;

    VarV(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  public static class NullV implements Value, Identifier {
    public static final NullV Null = new NullV();

    NullV() { }

    @JsonValue
    public NullNode value() {
      return NullNode.getInstance();
    }
  }
}
