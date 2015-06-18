package com.faunadb.client.java.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.NullNode;
import com.faunadb.client.java.query.Expression;
import com.faunadb.client.java.query.Language;
import com.faunadb.client.java.response.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Represents any scalar or non-scalar value in the FaunaDB query language. FaunaDB value types consist of
 * all of the JSON value types, as well as the FaunaDB-specific types, {@link Ref} and {@link Set}.
 *
 * <p>Scalar values are {@link NumberV}, {@link StringV}, {@link DoubleV}, {@link BooleanV}, {@link NullV},
 * {@link Ref}, and {@link Set}.
 *
 * <p>Non-scalar values are {@link ObjectV} and {@link ArrayV}.</p>
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-values">FaunaDB Value Types</a></p>
 */
public class Value implements Expression {
  protected Value() { }

  /**
   * Represents an Object value in the FaunaDB query language. Objects are polymorphic dictionaries.
   *
   * @see Language#ObjectV
   */
  public final static class ObjectV extends Value {
    private final ImmutableMap<String, Value> values;

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
    public int hashCode() {
      return values.hashCode();
    }
  }

  /**
   * Represents an array value in the FaunaDB query language. Arrays are polymorphic ordered lists of other values.
   */
  public static final class ArrayV extends Value {
    private final ImmutableList<Value> values;

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
  public static class BooleanV extends Value {
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

  /**
   * Represents a Double value in the FaunadB query language.
   *
   * @see Language#DoubleV(double)
   */
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

  public static class NullV extends Value {
    public static final NullV Null = new NullV();

    NullV() { }

    @JsonValue
    public NullNode value() {
      return NullNode.getInstance();
    }
  }
}
