package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.faunadb.client.java.types.Ref;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public abstract class Value {
  public static class ObjectV extends Value {
    private final ImmutableMap<String, Value> values;

    public static ObjectV empty() {
      return new ObjectV(ImmutableMap.<String, Value>of());
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

  public static class ArrayV extends Value {
    private final ImmutableList<Value> values;

    public static ArrayV empty() {
      return new ArrayV(ImmutableList.<Value>of());
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

  public static class BooleanV extends Value {
    private final Boolean value;

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

  public static class DoubleV extends Value {
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

  public static class NumberV extends Value {
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

  public static class StringV extends Value {
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

  public static class RefV extends Value implements Identifier {
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
}
