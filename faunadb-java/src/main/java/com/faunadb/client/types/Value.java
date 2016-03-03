package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.NullNode;
import com.faunadb.client.query.Language;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Represents any scalar or non-scalar value in the FaunaDB query language. FaunaDB value types consist of
 * all of the JSON value types, as well as the FaunaDB-specific types, {@link Ref} and {@link SetRef}.
 * <p>
 * Scalar values are {@link LongV}, {@link StringV}, {@link DoubleV}, {@link BooleanV}, {@link NullV},
 * {@link Ref}, and {@link SetRef}.
 * <p>
 * Non-scalar values are {@link ObjectV} and {@link ArrayV}.
 * <p>
 * This interface itself does not have any directly accessible data. It must first be coerced into a type before
 * its data can be accessed.
 * Coercion functions will throw a {@link ClassCastException} if this node can not be coerced into the requested type.
 * Every coersion function has a safe version, suffixed with "Option", that returns an {@link Optional} type.
 * <p>
 * <b>Example</b>: Consider the {@code Value node} modeling the root of the tree:
 * <pre>{@code
 * {
 *   "ref": { "@ref": "some/ref" },
 *   "data": { "someKey": "string1", "someKey2": 123 }
 * }}</pre>
 * <p>
 * The result tree can be accessed using:
 * <pre>{@code
 *   node.get("ref").asRef(); // {@link Ref}("some/ref")
 *   node.get("data", "someKey").asString() // "string1"
 *   node.getOption("non-existing-key") // Optional.absent()
 * }</pre>
 *
 * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Value Types</a>
 */
public interface Value {

  /**
   * Coerces this node into a {@link String}.
   *
   * @return the string value of this node.
   * @throws ClassCastException if can not coerced to {@link String}.
   */
  String asString();

  /**
   * Attempts to coerce this node into a {@link String}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  Optional<String> asStringOption();

  /**
   * Coerces this node into a {@link Boolean}.
   *
   * @return the boolean value of this node.
   * @throws ClassCastException if can not coerced to {@link Boolean}.
   */
  Boolean asBoolean();

  /**
   * Attempts to coerce this node into a {@link Boolean}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  Optional<Boolean> asBooleanOption();

  /**
   * Coerces this node into a {@link Long}.
   *
   * @return the boolean value of this node.
   * @throws ClassCastException if can not coerced to {@link Long}.
   */
  Long asLong();

  /**
   * Attempts to coerce this node into a {@link Long}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  Optional<Long> asLongOption();

  /**
   * Coerces this node into a {@link Double}.
   *
   * @return the boolean value of this node.
   * @throws ClassCastException if can not coerced to {@link Double}.
   */
  Double asDouble();

  /**
   * Attempts to coerce this node into a {@link Double}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  Optional<Double> asDoubleOption();

  /**
   * Coerces this node into a {@link Instant}.
   *
   * @return the boolean value of this node.
   * @throws ClassCastException if can not coerced to {@link Instant}.
   */
  Instant asTs();

  /**
   * Attempts to coerce this node into a {@link Instant}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  Optional<Instant> asTsOption();

  /**
   * Coerces this node into a {@link LocalDate}.
   *
   * @return the boolean value of this node.
   * @throws ClassCastException if can not coerced to {@link LocalDate}.
   */
  LocalDate asDate();

  /**
   * Attempts to coerce this node into a {@link LocalDate}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  Optional<LocalDate> asDateOption();

  /**
   * Coerces this node into a {@link ImmutableList} of nodes.
   *
   * @return a immutable list of nodes.
   * @throws ClassCastException if can not coerced to {@link ImmutableList}.
   */
  ImmutableList<Value> asArray();

  /**
   * Attempts to coerce this node into a {@link ImmutableList} of nodes.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  Optional<ImmutableList<Value>> asArrayOption();

  /**
   * Coerces this node into a {@link ImmutableMap} of nodes.
   *
   * @return a immutable map of nodes.
   * @throws ClassCastException if can not coerced to {@link ImmutableMap}.
   */
  ImmutableMap<String, Value> asObject();

  /**
   * Attempts to coerce this node into a {@link ImmutableMap} of nodes.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  Optional<ImmutableMap<String, Value>> asObjectOption();

  /**
   * Coerces this node into a {@link Ref}.
   *
   * @return a immutable map of nodes.
   * @throws ClassCastException if can not coerced to {@link Ref}.
   */
  Ref asRef();

  /**
   * Attempts to coerce this node into a {@link Ref}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  Optional<Ref> asRefOption();

  /**
   * Coerces this node into a {@link SetRef}.
   *
   * @return a immutable map of nodes.
   * @throws ClassCastException if can not coerced to {@link SetRef}.
   */
  SetRef asSetRef();

  /**
   * Attempts to coerce this node into a {@link SetRef}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  Optional<SetRef> asSetRefOption();

  /**
   * Extract a specific field from this node.
   * <p>
   * <b>Example:</b>
   * <pre>{@code
   * // node = { "name": "jhon" }
   * node.get("name").asString() // "jhon"
   * }</pre>
   *
   * @return the value under the key.
   * @throws IllegalArgumentException if the field does not exists.
   */
  Value get(String key);

  /**
   * Attempts to extract a specific field from this node.
   * <p>
   * <b>Example:</b>
   * <pre>{@code
   * // node = { "name": "jhon" }
   * node.getOption("name") // Optional.of(Value("jhon"))
   * node.getOption("data") // Optional.absent()
   * }</pre>
   *
   * @return an {@link Optional} type containing the value of the field.
   */
  Optional<Value> getOption(String key);

  /**
   * Extract a path from this node.
   * <p>
   * <b>Example:</b>
   * <pre>{@code
   * // node = { "data": { "name": "jhon" } }
   * node.get("data", "name").asString() // "jhon"
   * }</pre>
   *
   * @return the value under the path.
   * @throws IllegalArgumentException if path does not exists.
   */
  Value get(String... keys);

  /**
   * Attempts to extract a path from this node.
   * <p>
   * <b>Example:</b>
   * <pre>{@code
   * // node = { "data": { "name": "jhon" } }
   * node.getOption("data", "name") // Optional.of(Value("jhon"))
   * node.getOption("data", "age") // Optional.absent()
   * }</pre>
   *
   * @return an {@link Optional} type containing the value under the path.
   */
  Optional<Value> getOption(String... keys);

  /**
   * Accesses the value of the specified element if this is an array node.
   * <p>
   * <b>Example:</b>
   * <pre>{@code
   * // node = ["jhon", "ale"]
   * node.get(0).asString() // "jhon"
   * node.get(1).asString() // "ale"
   * }</pre>
   *
   * @return the value on the index.
   * @throws ArrayIndexOutOfBoundsException if index is out of boundaries.
   */
  Value get(int index);

  /**
   * Attempts to accesses the value of the specified element if this is an array node.
   * <p>
   * <b>Example:</b>
   * <pre>{@code
   * // node = ["jhon", "ale"]
   * node.getOption(0) // Optional.of(Value("jhon"))
   * node.getOption(1) // Optional.of(Value("ale"))
   * node.getOption(2) // Optional.absent()
   * }</pre>
   *
   * @return an {@link Optional} type containing the value on the index.
   */
  Optional<Value> getOption(int index);

  /**
   * See {@link Value}
   */
  abstract class ConcreteValue implements Value {
    @Override
    public String asString() {
      throw failOnConvertTo("String");
    }

    @Override
    public Optional<String> asStringOption() {
      return Optional.absent();
    }

    @Override
    public Boolean asBoolean() {
      throw failOnConvertTo("Boolean");
    }

    @Override
    public Optional<Boolean> asBooleanOption() {
      return Optional.absent();
    }

    @Override
    public Long asLong() {
      throw failOnConvertTo("Long");
    }

    @Override
    public Optional<Long> asLongOption() {
      return Optional.absent();
    }

    @Override
    public Double asDouble() {
      throw failOnConvertTo("Double");
    }

    @Override
    public Optional<Double> asDoubleOption() {
      return Optional.absent();
    }

    @Override
    public Instant asTs() {
      throw failOnConvertTo("Instant");
    }

    @Override
    public Optional<Instant> asTsOption() {
      return Optional.absent();
    }

    @Override
    public LocalDate asDate() {
      throw failOnConvertTo("LocalDate");
    }

    @Override
    public Optional<LocalDate> asDateOption() {
      return Optional.absent();
    }

    @Override
    public ImmutableList<Value> asArray() {
      throw failOnConvertTo("ImmutableList<Value>");
    }

    @Override
    public Optional<ImmutableList<Value>> asArrayOption() {
      return Optional.absent();
    }

    @Override
    public ImmutableMap<String, Value> asObject() {
      throw failOnConvertTo("ImmutableMap<String, Value>");
    }

    @Override
    public Optional<ImmutableMap<String, Value>> asObjectOption() {
      return Optional.absent();
    }

    @Override
    public Ref asRef() {
      throw failOnConvertTo("Ref");
    }

    @Override
    public Optional<Ref> asRefOption() {
      return Optional.absent();
    }

    @Override
    public SetRef asSetRef() {
      throw failOnConvertTo("SetRef");
    }

    @Override
    public Optional<SetRef> asSetRefOption() {
      return Optional.absent();
    }

    @Override
    public Value get(String key) {
      throw new IllegalArgumentException(
        format("Can't get key %s on a non-object value", key));
    }

    @Override
    public Optional<Value> getOption(String key) {
      return Optional.absent();
    }

    @Override
    public Value get(String... keys) {
      Value res = this;
      for (String key : keys)
        res = res.get(key);

      return res;
    }

    @Override
    public Optional<Value> getOption(String... keys) {
      Optional<Value> res = Optional.<Value>of(this);

      for (String key : keys) {
        res = res.get().getOption(key);
        if (!res.isPresent()) break;
      }

      return res;
    }

    @Override
    public Value get(int index) {
      throw new IndexOutOfBoundsException(
        format("Can't get index %s on a non-array value", index));
    }

    @Override
    public Optional<Value> getOption(int index) {
      return Optional.absent();
    }

    private ClassCastException failOnConvertTo(String desiredType) {
      return new ClassCastException(
        format("Can't convert value to %s. Contained value is: %s", desiredType, toString()));
    }
  }

  /**
   * See {@link Value}
   */
  abstract class ScalarValue<T> extends ConcreteValue {

    final T value;

    ScalarValue(T value) {
      this.value = requireNonNull(value);
    }

    @Override
    public boolean equals(Object other) {
      return other != null && other instanceof ScalarValue &&
        this.value.equals(((ScalarValue) other).value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public String toString() {
      return format("%s(%s)", getClass().getSimpleName(), value);
    }

  }

  /**
   * Represents an Object value in the FaunaDB query language. Objects are polymorphic dictionaries.
   *
   * @see Language#Obj
   */
  @JsonDeserialize(using = Codec.ObjectDeserializer.class)
  final class ObjectV extends ConcreteValue {

    private final ImmutableMap<String, Value> values;

    public ObjectV(Map<String, ? extends Value> values) {
      requireNonNull(values);
      this.values = ImmutableMap.copyOf(values);
    }

    @JsonValue
    @Override
    public ImmutableMap<String, Value> asObject() {
      return values;
    }

    @Override
    public Value get(String key) {
      return values.get(key);
    }

    @Override
    public Optional<Value> getOption(String key) {
      return Optional.fromNullable(get(key));
    }

    @Override
    public boolean equals(Object other) {
      return other != null && other instanceof ObjectV &&
        this.values.equals(((ObjectV) other).values);
    }

    @Override
    public int hashCode() {
      return values.hashCode();
    }

    @Override
    public String toString() {
      return format("ObjectV(%s)", values);
    }
  }

  /**
   * Represents an array value in the FaunaDB query language. Arrays are polymorphic ordered lists of other values.
   *
   * @see Language#Arr
   */
  @JsonDeserialize(using = Codec.ArrayDeserializer.class)
  final class ArrayV extends ConcreteValue {

    private final ImmutableList<Value> values;

    @JsonValue
    @Override
    public ImmutableList<Value> asArray() {
      return values;
    }

    public ArrayV(List<? extends Value> values) {
      requireNonNull(values);
      this.values = ImmutableList.copyOf(values);
    }

    @Override
    public Value get(int index) {
      return values.get(index);
    }

    @Override
    public Optional<Value> getOption(int index) {
      try {
        return Optional.of(values.get(index));
      } catch (IndexOutOfBoundsException ign) {
        return Optional.absent();
      }
    }

    @Override
    public boolean equals(Object other) {
      return other != null
        && other instanceof ArrayV &&
        this.values.equals(((ArrayV) other).values);
    }

    @Override
    public int hashCode() {
      return values.hashCode();
    }

    @Override
    public String toString() {
      return format("Arr(%s)", values);
    }
  }

  /**
   * Represents a Boolean value in the FaunaDB query language.
   *
   * @see Language#Value(boolean)
   */
  final class BooleanV extends ScalarValue<Boolean> {

    public final static BooleanV TRUE = new BooleanV(true);
    public final static BooleanV FALSE = new BooleanV(false);

    private BooleanV(Boolean value) {
      super(value);
    }

    @JsonValue
    @Override
    public Boolean asBoolean() {
      return value;
    }

    @Override
    public Optional<Boolean> asBooleanOption() {
      return Optional.of(value);
    }

  }

  /**
   * Represents a Double value in the FaunadB query language.
   *
   * @see Language#Value(double)
   */
  final class DoubleV extends ScalarValue<Double> {

    public DoubleV(double value) {
      super(value);
    }

    @JsonValue
    @Override
    public Double asDouble() {
      return value;
    }

    @Override
    public Optional<Double> asDoubleOption() {
      return Optional.of(value);
    }

  }

  /**
   * Represents a Long value in the FaunadB query language.
   *
   * @see Language#Value(long)
   */
  final class LongV extends ScalarValue<Long> {

    public LongV(long value) {
      super(value);
    }

    @JsonValue
    @Override
    public Long asLong() {
      return value;
    }

    @Override
    public Optional<Long> asLongOption() {
      return Optional.of(value);
    }
  }

  /**
   * Represents a String value in the FaunadB query language.
   *
   * @see Language#Value(String)
   */
  final class StringV extends ScalarValue<String> {

    public StringV(String value) {
      super(value);
    }

    @JsonValue
    @Override
    public String asString() {
      return value;
    }

    @Override
    public Optional<String> asStringOption() {
      return Optional.of(value);
    }
  }

  /**
   * Represents a null value in the FaunadB query language.
   *
   * @see Language#Null()
   */
  final class NullV extends ConcreteValue {

    public static final NullV NULL = new NullV();

    private NullV() {
    }

    @JsonValue
    private NullNode value() {
      return NullNode.getInstance();
    }

    @Override
    public boolean equals(Object other) {
      return other != null
        && other instanceof NullV;
    }

    @Override
    public int hashCode() {
      return -1;
    }

    @Override
    public String toString() {
      return "null";
    }

  }

  /**
   * Represents a Timestamp value in the FaunadB query language.
   *
   * @see Language#Value(Instant)
   */
  final class TsV extends ScalarValue<Instant> {

    public TsV(Instant value) {
      super(value);
    }

    @JsonCreator
    private TsV(@JsonProperty("@ts") String value) {
      super(ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
    }

    @JsonProperty("@ts")
    private String value() {
      return value.toString();
    }

    @Override
    public Instant asTs() {
      return value;
    }

    @Override
    public Optional<Instant> asTsOption() {
      return Optional.of(value);
    }

  }

  /**
   * Represents a Date value in the FaunadB query language.
   *
   * @see Language#Value(LocalDate)
   */
  final class DateV extends ScalarValue<LocalDate> {

    public DateV(LocalDate value) {
      super(value);
    }

    @JsonCreator
    private DateV(@JsonProperty("@date") String value) {
      super(LocalDate.parse(value));
    }

    @JsonProperty("@date")
    private String strValue() {
      return value.toString();
    }

    @Override
    public LocalDate asDate() {
      return value;
    }

    @Override
    public Optional<LocalDate> asDateOption() {
      return Optional.of(value);
    }

  }

}
