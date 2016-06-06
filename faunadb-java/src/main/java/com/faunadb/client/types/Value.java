package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.NullNode;
import com.faunadb.client.query.Expr;
import com.faunadb.client.query.Language;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.faunadb.client.types.Codec.*;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
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
@JsonDeserialize(using = Deserializer.ValueDeserializer.class)
public abstract class Value extends Expr {

  @Override
  protected Value value() {
    return this;
  }

  /**
   * Coerces this node into a {@link String}.
   *
   * @return the string value of this node.
   * @throws ClassCastException if can not coerced to {@link String}.
   */
  public final String asString() {
    return get(Field.to(STRING));
  }

  /**
   * Attempts to coerce this node into a {@link String}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  public final Optional<String> asStringOption() {
    return getOpt(Field.to(STRING));
  }

  /**
   * Coerces this node into a {@link Boolean}.
   *
   * @return the boolean value of this node.
   * @throws ClassCastException if can not coerced to {@link Boolean}.
   */
  public final Boolean asBoolean() {
    return get(Field.to(BOOLEAN));
  }

  /**
   * Attempts to coerce this node into a {@link Boolean}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  public final Optional<Boolean> asBooleanOption() {
    return getOpt(Field.to(BOOLEAN));
  }

  /**
   * Coerces this node into a {@link Long}.
   *
   * @return the boolean value of this node.
   * @throws ClassCastException if can not coerced to {@link Long}.
   */
  public final Long asLong() {
    return get(Field.to(LONG));
  }

  /**
   * Attempts to coerce this node into a {@link Long}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  public final Optional<Long> asLongOption() {
    return getOpt(Field.to(LONG));
  }

  /**
   * Coerces this node into a {@link Double}.
   *
   * @return the boolean value of this node.
   * @throws ClassCastException if can not coerced to {@link Double}.
   */
  public final Double asDouble() {
    return get(Field.to(DOUBLE));
  }

  /**
   * Attempts to coerce this node into a {@link Double}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  public final Optional<Double> asDoubleOption() {
    return getOpt(Field.to(DOUBLE));
  }

  /**
   * Coerces this node into a {@link Instant}.
   *
   * @return the boolean value of this node.
   * @throws ClassCastException if can not coerced to {@link Instant}.
   */
  public final Instant asTs() {
    return get(Field.to(TS));
  }

  /**
   * Attempts to coerce this node into a {@link Instant}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  public final Optional<Instant> asTsOption() {
    return getOpt(Field.to(TS));
  }

  /**
   * Coerces this node into a {@link LocalDate}.
   *
   * @return the boolean value of this node.
   * @throws ClassCastException if can not coerced to {@link LocalDate}.
   */
  public final LocalDate asDate() {
    return get(Field.to(DATE));
  }

  /**
   * Attempts to coerce this node into a {@link LocalDate}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  public final Optional<LocalDate> asDateOption() {
    return getOpt(Field.to(DATE));
  }

  /**
   * Coerces this node into a {@link ImmutableList} of nodes.
   *
   * @return a immutable list of nodes.
   * @throws ClassCastException if can not coerced to {@link ImmutableList}.
   */
  public final ImmutableList<Value> asArray() {
    return get(Field.to(ARRAY));
  }

  /**
   * Attempts to coerce this node into a {@link ImmutableList} of nodes.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  public final Optional<ImmutableList<Value>> asArrayOption() {
    return getOpt(Field.to(ARRAY));
  }

  /**
   * Coerces this node into a {@link ImmutableMap} of nodes.
   *
   * @return a immutable map of nodes.
   * @throws ClassCastException if can not coerced to {@link ImmutableMap}.
   */
  public final ImmutableMap<String, Value> asObject() {
    return get(Field.to(OBJECT));
  }

  /**
   * Attempts to coerce this node into a {@link ImmutableMap} of nodes.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  public final Optional<ImmutableMap<String, Value>> asObjectOption() {
    return getOpt(Field.to(OBJECT));
  }

  /**
   * Coerces this node into a {@link Ref}.
   *
   * @return a immutable map of nodes.
   * @throws ClassCastException if can not coerced to {@link Ref}.
   */
  public final Ref asRef() {
    return get(Field.to(REF));
  }

  /**
   * Attempts to coerce this node into a {@link Ref}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  public final Optional<Ref> asRefOption() {
    return getOpt(Field.to(REF));
  }

  /**
   * Coerces this node into a {@link SetRef}.
   *
   * @return a immutable map of nodes.
   * @throws ClassCastException if can not coerced to {@link SetRef}.
   */
  public final SetRef asSetRef() {
    return get(Field.to(SET_REF));
  }

  /**
   * Attempts to coerce this node into a {@link SetRef}.
   *
   * @return an {@link Optional} type with the coerced value.
   */
  public final Optional<SetRef> asSetRefOption() {
    return getOpt(Field.to(SET_REF));
  }

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
  public final Value get(String... keys) {
    return Field.at(keys).get(this).getOrThrow();
  }

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
  public final Optional<Value> getOpt(String... keys) {
    return Field.at(keys).get(this).asOpt();
  }

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
  public final Value get(int... index) {
    return Field.at(index).get(this).getOrThrow();
  }

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
  public final Optional<Value> getOption(int... index) {
    return Field.at(index).get(this).asOpt();
  }

  public final <T> T get(Field<T> field) {
    return field.get(this).getOrThrow();
  }

  public final <T> Optional<T> getOpt(Field<T> field) {
    return field.get(this).asOpt();
  }

  public final <T> List<T> collect(Field<T> field) {
    ImmutableList.Builder<T> res = ImmutableList.builder();
    for (Value value : get(Field.to(ARRAY)))
      res.add(value.get(field));

    return res.build();
  }

  @SuppressWarnings("unused") // Used by Jackson to serialize the value to JSON
  abstract Object toJson();

  /**
   * See {@link Value}
   */
  @JsonDeserialize(using = JsonDeserializer.None.class)
  public static abstract class ScalarValue<T> extends Value {

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
  @JsonDeserialize(using = Deserializer.ObjectDeserializer.class)
  public static final class ObjectV extends Value {

    final ImmutableMap<String, Value> values;

    public ObjectV(Map<String, ? extends Value> values) {
      requireNonNull(values);
      this.values = ImmutableMap.copyOf(values);
    }

    @Override
    @JsonValue
    ImmutableMap<String, Value> toJson() {
      return values;
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
  @JsonDeserialize(using = Deserializer.ArrayDeserializer.class)
  public static final class ArrayV extends Value {

    final ImmutableList<Value> values;

    public ArrayV(List<? extends Value> values) {
      requireNonNull(values);
      this.values = ImmutableList.copyOf(values);
    }

    @Override
    @JsonValue
    ImmutableList<Value> toJson() {
      return values;
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
  public static final class BooleanV extends ScalarValue<Boolean> {

    public final static BooleanV TRUE = new BooleanV(true);
    public final static BooleanV FALSE = new BooleanV(false);

    public static BooleanV valueOf(boolean value) {
      return value ? TRUE : FALSE;
    }

    private BooleanV(Boolean value) {
      super(value);
    }

    @Override
    @JsonValue
    Boolean toJson() {
      return value;
    }

  }

  /**
   * Represents a Double value in the FaunadB query language.
   *
   * @see Language#Value(double)
   */
  public static final class DoubleV extends ScalarValue<Double> {
    public DoubleV(double value) {
      super(value);
    }

    @Override
    @JsonValue
    Double toJson() {
      return value;
    }
  }

  /**
   * Represents a Long value in the FaunadB query language.
   *
   * @see Language#Value(long)
   */
  public static final class LongV extends ScalarValue<Long> {
    public LongV(long value) {
      super(value);
    }

    @Override
    @JsonValue
    Long toJson() {
      return value;
    }
  }

  /**
   * Represents a String value in the FaunadB query language.
   *
   * @see Language#Value(String)
   */
  public static final class StringV extends ScalarValue<String> {
    public StringV(String value) {
      super(value);
    }

    @Override
    @JsonValue
    String toJson() {
      return value;
    }
  }

  /**
   * Represents a null value in the FaunadB query language.
   *
   * @see Language#Null()
   */
  public static final class NullV extends Value {

    public static final NullV NULL = new NullV();

    private NullV() {
    }

    @Override
    @JsonValue
    NullNode toJson() {
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
  public static final class TsV extends ScalarValue<Instant> {

    public TsV(Instant value) {
      super(value);
    }

    @JsonCreator
    private TsV(@JsonProperty("@ts") String value) {
      super(ZonedDateTime.parse(value, ISO_OFFSET_DATE_TIME).toInstant());
    }

    @Override
    @JsonProperty("@ts")
    String toJson() {
      return value.toString();
    }

  }

  /**
   * Represents a Date value in the FaunadB query language.
   *
   * @see Language#Value(LocalDate)
   */
  public static final class DateV extends ScalarValue<LocalDate> {

    public DateV(LocalDate value) {
      super(value);
    }

    @JsonCreator
    private DateV(@JsonProperty("@date") String value) {
      super(LocalDate.parse(value));
    }

    @Override
    @JsonProperty("@date")
    String toJson() {
      return value.toString();
    }
  }

  /**
   * A FaunaDB set literal.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values-special_types">FaunaDB Special Types</a>
   */
  public static final class SetRef extends ScalarValue<ImmutableMap<String, Value>> {

    public SetRef(@JsonProperty("@set") ImmutableMap<String, Value> parameters) {
      super(parameters);
    }

    /**
     * Extact SetRef structure
     *
     * @return SetRef structure
     */
    public ImmutableMap<String, Value> parameters() {
      return value;
    }

    @Override
    @JsonProperty("@set")
    ImmutableMap<String, Value> toJson() {
      return value;
    }
  }

  /**
   * A FaunaDB ref type.
   *
   * @see <a href="https://faunadb.com/documentation/queries#values-special_types">FaunaDB Special Types</a>
   */
  public static final class Ref extends Value.ScalarValue<String> {

    @JsonCreator
    public Ref(@JsonProperty("@ref") String value) {
      super(value);
    }

    @Override
    @JsonProperty("@ref")
    String toJson() {
      return value;
    }

    /**
     * Extracts its string value.
     *
     * @return a string with the ref value
     */
    public String strValue() {
      return value;
    }
  }

}
