package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.NullNode;
import com.faunadb.client.query.Expression;
import com.faunadb.client.query.Language;
import com.faunadb.client.response.*;
import com.faunadb.client.response.Class;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Represents any scalar or non-scalar value in the FaunaDB query language. FaunaDB value types consist of
 * all of the JSON value types, as well as the FaunaDB-specific types, {@link Ref} and {@link Set}.
 *
 * <p>Scalar values are {@link LongV}, {@link StringV}, {@link DoubleV}, {@link BooleanV}, {@link NullV},
 * {@link Ref}, and {@link Set}.
 *
 * <p>Non-scalar values are {@link ObjectV} and {@link ArrayV}.</p>
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Value Types</a></p>
 */
public interface Value extends Expression {
  String asString();
  Boolean asBoolean();
  Long asLong();
  Double asDouble();
  ImmutableList<Value> asArray();
  ImmutableMap<String, Value> asObject();
  Ref asRef();
  Set asSet();
  Page asPage();
  Instance asInstance();
  Key asKey();
  Database asDatabase();
  com.faunadb.client.response.Class asClass();
  Index asIndex();
  Event asEvent();
  Value get(String key);
  Value get(int index);

  abstract class ConcreteValue implements Value {
    @Override
    public String asString() {
      return null;
    }

    @Override
    public Boolean asBoolean() {
      return null;
    }

    @Override
    public Long asLong() {
      return null;
    }

    @Override
    public Double asDouble() {
      return null;
    }

    @Override
    public ImmutableList<Value> asArray() {
      return null;
    }

    @Override
    public ImmutableMap<String, Value> asObject() {
      return null;
    }

    @Override
    public Ref asRef() {
      return null;
    }

    @Override
    public Value get(int index) {
      return null;
    }

    @Override
    public Value get(String key) {
      return null;
    }

    @Override
    public Page asPage() {
      return null;
    }

    @Override
    public Instance asInstance() {
      return null;
    }

    @Override
    public Key asKey() {
      return null;
    }

    @Override
    public Database asDatabase() {
      return null;
    }

    @Override
    public Class asClass() {
      return null;
    }

    @Override
    public Index asIndex() {
      return null;
    }

    @Override
    public Event asEvent() {
      return null;
    }

    @Override
    public Set asSet() {
      return null;
    }
  }

  /**
   * Represents an Object value in the FaunaDB query language. Objects are polymorphic dictionaries.
   *
   * @see Language#ObjectV
   */
  final class ObjectV extends ConcreteValue {
    private final ImmutableMap<String, Value> values;

    @Override
    public ImmutableMap<String, Value> asObject() {
      return values;
    }

    /**
     * Constructs an empty object value.
     * @see Language#ObjectV()
     */
    public static ObjectV empty() {
      return new ObjectV(ImmutableMap.<String, Value>of());
    }

    /**
     * Constructs an object value containing the specified key/value pair.
     * @see Language#ObjectV(String, Value)
     */
    public static ObjectV create(String k1, Value v1) {
      return new ObjectV(ImmutableMap.of(k1, v1));
    }

    /**
     * Constructs an object value containing the specified key/value pairs.
     * @see Language#ObjectV(String, Value, String, Value)
     */
    public static ObjectV create(String k1, Value v1, String k2, Value v2) {
      return new ObjectV(ImmutableMap.of(k1, v1, k2, v2));
    }

    /**
     * Constructs an object value containing the specified key/value pairs.
     * @see Language#ObjectV(String, Value, String, Value, String, Value)
     */
    public static ObjectV create(String k1, Value v1, String k2, Value v2, String k3, Value v3) {
      return new ObjectV(ImmutableMap.of(k1, v1, k2, v2, k3, v3));
    }

    /**
     * Constructs an object value containing the specified key/value pairs.
     * @see Language#ObjectV(String, Value, String, Value, String, Value, String, Value)
     */
    public static ObjectV create(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4) {
      return new ObjectV(ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4));
    }

    /**
     * Constructs an object value containing the specified key/value pairs.
     * @see Language#ObjectV(String, Value, String, Value, String, Value, String, Value, String, Value)
     */
    public static ObjectV create(String k1, Value v1, String k2, Value v2, String k3, Value v3, String k4, Value v4, String k5, Value v5) {
      return new ObjectV(ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
    }

    /**
     * Constructs an object value wrapping the given dictionary.
     * @see Language#ObjectV(ImmutableMap)
     */
    public static ObjectV create(ImmutableMap<String, Value> values) {
      return new ObjectV(values);
    }

    ObjectV(ImmutableMap<String, Value> values) {
      this.values = values;
    }

    @JsonValue
    public ImmutableMap<String, Value> values() {
      return values;
    }

    @Override
    public Value get(String key) {
      return values.get(key);
    }

    @Override
    public int hashCode() {
      return values.hashCode();
    }
  }

  /**
   * Represents an array value in the FaunaDB query language. Arrays are polymorphic ordered lists of other values.
   */
  final class ArrayV extends ConcreteValue {
    private final ImmutableList<Value> values;

    @Override
    public ImmutableList<Value> asArray() {
      return values;
    }

    /**
     * Returns an empty array value.
     *
     * @see Language#ArrayV()
     */
    public static ArrayV empty() {
      return new ArrayV(ImmutableList.<Value>of());
    }

    /**
     * Constructs an array value containing the specified value.
     *
     * @see Language#ArrayV(Value)
     */
    public static ArrayV create(Value v1) {
      return new ArrayV(ImmutableList.of(v1));
    }

    /**
     * Constructs an array value containing the specified values.
     *
     * @see Language#ArrayV(Value, Value)
     */
    public static ArrayV create(Value v1, Value v2) {
      return new ArrayV(ImmutableList.of(v1, v2));
    }

    /**
     * Constructs an array value containing the specified values.
     *
     * @see Language#ArrayV(Value, Value, Value)
     */
    public static ArrayV create(Value v1, Value v2, Value v3) {
      return new ArrayV(ImmutableList.of(v1, v2, v3));
    }

    /**
     * Constructs an array value containing the specified values.
     *
     * @see Language#ArrayV(Value, Value, Value, Value)
     */
    public static ArrayV create(Value v1, Value v2, Value v3, Value v4) {
      return new ArrayV(ImmutableList.of(v1, v2, v3, v4));
    }

    /**
     * Constructs an array value containing the specified values.
     *
     * @see Language#ArrayV(Value, Value, Value, Value, Value)
     */
    public static ArrayV create(Value v1, Value v2, Value v3, Value v4, Value v5) {
      return new ArrayV(ImmutableList.<Value>of(v1, v2, v3, v4, v5));
    }

    /**
     * Constructs an array value containing the specified values.
     *
     * @see Language#ArrayV(Value, Value, Value, Value, Value, Value)
     */
    public static ArrayV create(Value v1, Value v2, Value v3, Value v4, Value v5, Value v6) {
      return new ArrayV(ImmutableList.<Value>of(v1, v2, v3, v4, v5, v6));
    }

    /**
     * Constructs an array value wrapping the provided list of values.
     *
     * @see Language#ArrayV(ImmutableList)
     */
    public static ArrayV create(ImmutableList<Value> values) {
      return new ArrayV(values);
    }

    ArrayV(ImmutableList<Value> values) {
      this.values = values;
    }

    @Override
    public Value get(int index) {
      return values.get(index);
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

  /**
   * Represents a Boolean value in the FaunaDB query language.
   *
   * @see Language#BooleanV(boolean)
   */
  final class BooleanV extends ConcreteValue {
    private final Boolean value;

    public final static BooleanV True = BooleanV.create(true);
    public final static BooleanV False = BooleanV.create(false);

    public static BooleanV create(boolean value) {
      return new BooleanV(value);
    }

    @Override
    public boolean equals(Object obj) {
      return super.equals(obj);
    }

    @Override
    public Boolean asBoolean() {
      return value;
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

  /**
   * Represents a Double value in the FaunadB query language.
   *
   * @see Language#DoubleV(double)
   */
  final class DoubleV extends ConcreteValue {
    private final Double value;

    public static DoubleV create(double value) {
      return new DoubleV(value);
    }

    @Override
    public Double asDouble() {
      return value;
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

  final class LongV extends ConcreteValue {
    private final Long value;

    public static LongV create(long value) {
      return new LongV(value);
    }

    @Override
    public Long asLong() {
      return value;
    }

    LongV(long value) {
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

  final class StringV extends ConcreteValue {
    private final String value;

    public static StringV create(String value) {
      return new StringV(value);
    }

    @Override
    public String asString() {
      return value;
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

  final class NullV extends ConcreteValue {
    public static final NullV Null = new NullV();

    NullV() { }

    @JsonValue
    public NullNode value() {
      return NullNode.getInstance();
    }
  }
}
