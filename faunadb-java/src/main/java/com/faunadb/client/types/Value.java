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
 * <p>
 * <b>Example</b>: Consider the {@link Value} node modeling the root of the tree:
 * <pre>{@code
 * {
 *   "ref": { "@ref": "some/ref" },
 *   "data": { "someKey": "string1", "someKey2": 123 }
 * }}</pre>
 * <p>
 * The result tree can be accessed using:
 * <pre>{@code
 *   Field<Ref> ref = Field.at("ref").as(Codec.REF);
 *   Field<String> someKey = Field.at("data", "someKey").as(Codec.STRING);
 *   Field<String> nonExistingKey = Field.at("non-existing-key").as(Codec.LONG);
 *
 *   node.get(ref); // Ref("some/ref")
 *   node.get(someKey); // "string1"
 *   node.getOptional(nonExistingKey) // Optional.absent()
 * }</pre>
 * <p>
 * The interface also has helpers to transverse values without {@link Field} references:
 * <pre>{@code
 *   node.at("ref").as(Codec.REF).get(); // Ref("some/ref")
 *   node.at("data", "someKey").as(Codec.STRING).get() // "string1"
 *   node.at("non-existing-key").as(Codec.LONG).getOptional() // Optional.absent()
 * }</pre>
 *
 * @see <a href="https://faunadb.com/documentation/queries#values">FaunaDB Value Types</a>
 * @see Field
 * @see Codec
 */
@JsonDeserialize(using = Deserializer.ValueDeserializer.class)
public abstract class Value extends Expr {

  /**
   * Attempts to coerce this value using the {@link Codec} passed
   *
   * @param codec codec function to attempt coercion
   * @return the {@link Result} of the coercion
   * @see Codec
   */
  public final <T> Result<T> as(Codec<T> codec) {
    return codec.apply(this);
  }

  /**
   * Extract a {@link Field} from this node
   *
   * @param field field to extract
   * @return the resulting value of extracting the {@link Field} from this node
   * @throws IllegalStateException if {@link Field} does not exists on this node
   * @see Field
   */
  public final <T> T get(Field<T> field) {
    return field.get(this).get();
  }

  /**
   * Attempts to extact a {@link Field} from this node
   *
   * @param field field to extract
   * @return An {@link Optional} with the resulting value if the field's extraction was successful
   * @see Field
   */
  public final <T> Optional<T> getOptional(Field<T> field) {
    return field.get(this).getOptional();
  }

  /**
   * Loop through this node collecting the {@link Field} passed, assuming the node is an instance of {@link ArrayV}
   * <p>
   * <b>Example</b>: Consider the {@link Value} node modeling the root of the tree:
   * <pre>{@code
   * {
   *   "data": {
   *     "arrayOfStrings": ["Jhon", "Bill"],
   *     "arrayOfObjects": [ {"name": "Jhon"}, {"name": "Bill"} ]
   *    }
   * }}</pre>
   * <p>
   * The result tree can be accessed using:
   * <pre>{@code
   *   node.get("arrayOfStrings").collect(Field.to(Codec.STRING)); // ["Jhon", "Bill"]
   *
   *   Field<String> name = Field.at("name").as(Codec.STRING);
   *   node.get("arrayOfObjects").collect(name); // ["Jhon", "Bill"]
   * }</pre>
   *
   * @param field field to extract from each array value
   * @return a {@link ImmutableList} with the collected {@link Field}s
   * @see Field
   */
  public final <T> ImmutableList<T> collect(Field<T> field) {
    return Field.root().collect(field).get(this).get();
  }

  /**
   * Navigate through object's keys, assuming value is an instance of {@link ObjectV}.
   *
   * @param keys path to navigate to
   * @return {@link Value} under the path or {@link NullV}
   */
  public final Value at(String... keys) {
    return Field.at(keys).get(this).getOrElse(NullV.NULL);
  }

  /**
   * Navigate through array's indexes, assuming value is an instance of {@link ArrayV}
   *
   * @param indexes path to navigate to
   * @return {@link Value} under the path or {@link NullV}
   */
  public final Value at(int... indexes) {
    return Field.at(indexes).get(this).getOrElse(NullV.NULL);
  }

  /**
   * Represents a scalar value at the FaunaDB query language.
   * See {@link Value}
   */
  @JsonDeserialize(using = JsonDeserializer.None.class) // Disables generic value deserializer for scalar values
  private static abstract class ScalarValue<T> extends Value {

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
    protected ImmutableMap<String, ImmutableMap<String, Value>> toJson() {
      return ImmutableMap.of("object", values);
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
    protected ImmutableList<Value> toJson() {
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
    protected Boolean toJson() {
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
    protected Double toJson() {
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
    protected Long toJson() {
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
    protected String toJson() {
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
    protected NullNode toJson() {
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
      return "NullV";
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
    protected String toJson() {
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
    protected String toJson() {
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
    protected ImmutableMap<String, Value> toJson() {
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
    protected String toJson() {
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
