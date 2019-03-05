package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.NullNode;
import com.faunadb.client.query.Expr;
import com.faunadb.client.query.Language;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;

/**
 * {@link Value} represents query responses from FaunaDB.
 * Instances of {@link Value} should be treated as opaque vales until converted to a native type.
 *
 * Available conversion methods are:
 * <ul>
 *   <li>{@link Value#to(Class)}</li>
 *   <li>{@link Value#to(Codec)}</li>
 *   <li>{@link Value#get(Field)}</li>
 *   <li>{@link Value#getOptional(Field)}</li>
 *   <li>{@link Value#collect(Field)}</li>
 *   <li>{@link Value#asCollectionOf(Class)}</li>
 *   <li>{@link Value#asMapOf(Class)}</li>
 * </ul>
 *
 * Value trees can be traversed using one of the following methods:
 * <ul>
 *   <li>{@link Value#at(int...)}</li>
 *   <li>{@link Value#at(String...)}</li>
 * </ul>
 *
 * <p>Examples:</p>
 *
 * Using the traversal API:
 * <pre>{@code
 *   Value result = client.query(getUserQuery).get();
 *
 *   String name = result
 *     .at("data", "name")
 *     .to(String.class)
 *     .get();
 * }</pre>
 *
 * Using a field extractor:
 * <pre>{@code
 *   Field<String> name = Field.at("data", "name").to(String.class);
 *   Value result = client.query(getUserQuery).get();
 *   String name = result.get(name);
 * }</pre>
 *
 * @see <a href="https://app.fauna.com/documentation/reference/queryapi#simple-type">FaunaDB Value Types</a>
 * @see Field
 * @see Codec
 */
@JsonDeserialize(using = Deserializer.ValueDeserializer.class)
public abstract class Value extends Expr {

  private Value() {
  }

  /**
   * Attempts to convert the value using the {@link Codec} passed.
   *
   * @param <T> the type to convert to
   * @param codec codec function to attempt conversion
   * @return the {@link Result} of the conversion
   * @see Codec
   */
  public final <T> Result<T> to(Codec<T> codec) {
    return codec.decode(this);
  }

  /**
   * Attempts to decode the value using the reflection {@link Decoder}.
   *
   * @param <T> the type to convert to
   * @param clazz a class type to convert
   * @return the {@link Result} of the conversion
   * @see Decoder
   */
  public final <T> Result<T> to(Class<T> clazz) {
    return Decoder.decode(this, Types.of(clazz));
  }

  /**
   * Attempts to encode an object as a {@link Value} using the reflection {@link Encoder}.
   *
   * @param <T> the type to convert from
   * @param obj the object instance to encode
   * @return the {@link Result} of the conversion
   */
  public static <T> Result<Value> from(T obj) {
    return Encoder.encode(obj);
  }

  /**
   * Attempts to convert the value to a {@link Map}.
   *
   * @param <T> the type of the values on {@link Map}
   * @param valueType the type of the desired {@link Map}'s values.
   * @return a {@link Result} containing the resulting {@link Map}.
   * @see Decoder
   * @see Types
   */
  public final <T> Result<Map<String, T>> asMapOf(Class<T> valueType) {
    return Decoder.decode(this, Types.hashMapOf(valueType));
  }

  /**
   * Attempts to convert the value to a {@link Map}.
   *
   * @param <T> the type of the values in the {@link Map}
   * @param valueType the type of the values in the {@link Map}
   * @return a {@link Map}
   * @see Decoder
   * @see Types
   */
  public final <T> Map<String, T> toMap(Class<T> valueType) {
    return asMapOf(valueType).get();
  }

  /**
   * Attempts to convert the value to a {@link Collection}.
   *
   * @param <T> the type of the elements on the {@link Collection}
   * @param elementType the type of the elements in the {@link Collection}.
   * @return a {@link Result} containing the resulting {@link Collection}.
   * @see Decoder
   * @see Types
   */
  public final <T> Result<Collection<T>> asCollectionOf(Class<T> elementType) {
    return Decoder.decode(this, Types.arrayListOf(elementType));
  }

  /**
   * Attempts to convert the value to a {@link Collection}.
   *
   * @param <T> the type of the elements in the {@link Collection}
   * @param elementType the type of the elements in the {@link Collection}
   * @return a new collection
   * @see Decoder
   * @see Types
   */
  public final <T> Collection<T> collect(Class<T> elementType) {
    return asCollectionOf(elementType).get();
  }

  /**
   * Extract a {@link Field} from the value.
   *
   * @param <T> the type of returned field
   * @param field the {@link Field} to extract
   * @return the resulting value of extracting the {@link Field} from this value
   * @throws IllegalStateException if {@link Field} does not exists on this value
   * @see Field
   */
  public final <T> T get(Field<T> field) {
    return field.get(this).get();
  }

  /**
   * Attempts to decode the value using the reflection {@link Decoder}.
   *
   * @param <T> the type to convert to
   * @param clazz a class type to convert
   * @return the converted result
   * @see Decoder
   */
  public final <T> T get(Class<T> clazz) {
    return to(clazz).get();
  }

  /**
   * Safely attempts to extract a {@link Field} from this value.
   *
   * @param <T> the type of returned field
   * @param field {@link Field} to extract
   * @return An {@link Optional} containing the resulting value, if the field extraction was successful.
   * It returns {@link Optional#empty()}, otherwise.
   * @see Field
   */
  public final <T> Optional<T> getOptional(Field<T> field) {
    return field.get(this).getOptional();
  }

  /**
   * Converts this into an {@link Optional}.
   *
   * @return An {@link Optional} containing this value, if present.
   */
  @JsonIgnore
  public Optional<Value> getOptional() {
    return Optional.of(this);
  }

  /**
   * Returns this value, or returns null if no value is present.
   *
   * @return the value or null
   */
  public final Value orNull() {
    return getOptional().orElse(null);
  }

  /**
   * Assuming the underlying value is a collection, it collects the {@link Field} provided
   * for all elements in the collection.
   *
   * <p>For example:</p>
   *
   * <pre>{@code
   *   Field<String> userName = Field.at("name").to(String.class);
   *   Value result = client.query(getAllUsersQuery).get();
   *   List<String> userNames = result.at("data").collect(userName);
   * }</pre>
   *
   * @param <T> the type of the elements in the resulting {@link List}
   * @param field the {@link Field} to extract from each element in the underlying collection
   * @return a {@link List} with the collected fields
   * @see Field
   */
  public final <T> List<T> collect(Field<T> field) {
    return Field.root().collect(field).get(this).get();
  }

  /**
   * Assuming the underlying value is a key/value map, it traverses to a desired path.
   *
   * <p>For example:</p>
   *
   * <pre>{@code
   *   Value result = client.query(getUser).get();
   *   Value zipCode = result.at("data", "address", "zipCode");
   * }</pre>
   *
   * @param keys the object keys to traverse
   * @return the {@link Value} under the path provided
   */
  public final Value at(String... keys) {
    return Field.at(keys).get(this).getOrElse(NullV.NULL);
  }

  /**
   * Assuming the underlying value is a collection, it traverses to a desired path.
   *
   * <p>For example:</p>
   *
   * <pre>{@code
   *   Value result = client.query(getAllUsers).get();
   *   Value firstUser = result.at("data").at(0);
   * }</pre>
   *
   * @param indexes the collection indexes to traverse
   * @return the {@link Value} under the path provided
   */
  public final Value at(int... indexes) {
    return Field.at(indexes).get(this).getOrElse(NullV.NULL);
  }

  /**
   * Represents a scalar value at the FaunaDB query language.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see Value
   */
  @JsonDeserialize(using = JsonDeserializer.None.class) // Disables generic value deserializer for scalar values
  static abstract class ScalarValue<T> extends Value {

    @JsonIgnore
    final T value;

    ScalarValue(T value) {
      this.value = Objects.requireNonNull(value);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ScalarValue &&
        this.value.equals(((ScalarValue) other).value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public String toString() {
      return value.toString();
    }

  }

  /**
   * Represents an Object value in the FaunaDB query language.
   * Objects are polymorphic dictionaries.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see Language#Obj
   * @see Value
   */
  @JsonDeserialize(using = Deserializer.ObjectDeserializer.class)
  public static final class ObjectV extends Value {

    final Map<String, Value> values;

    public ObjectV(Map<String, ? extends Value> values) {
      this.values = Collections.unmodifiableMap(values);
    }

    @Override
    @JsonValue
    protected Map<String, Map<String, Value>> toJson() {
        return Collections.singletonMap("object", values);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ObjectV &&
        this.values.equals(((ObjectV) other).values);
    }

    @Override
    public int hashCode() {
      return values.hashCode();
    }

    @Override
    public String toString() {
        return values.entrySet()
            .stream()
            .map(e -> String.join(": ", Arrays.asList(e.getKey(), String.valueOf(e.getValue()))))
            .collect(Collectors.joining(", ", "{", "}"));
    }
  }

  /**
   * Represents an array value in the FaunaDB query language.
   * Arrays are polymorphic ordered lists of other values.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see Language#Arr
   * @see Value
   */
  @JsonDeserialize(using = Deserializer.ArrayDeserializer.class)
  public static final class ArrayV extends Value {

    final List<Value> values;

    public ArrayV(List<? extends Value> values) {
      this.values = Collections.unmodifiableList(values);
    }

    @Override
    @JsonValue
    protected List<Value> toJson() {
      return values;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ArrayV &&
        this.values.equals(((ArrayV) other).values);
    }

    @Override
    public int hashCode() {
      return values.hashCode();
    }

    @Override
    public String toString() {
        Iterable<String> vals = values.stream()
            .map(String::valueOf)
            .collect(Collectors.toList());

        return format("[%s]", String.join(", ", vals));
    }
  }

  /**
   * Represents a boolean value in the FaunaDB query language.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see Language#Value(boolean)
   * @see Value
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
   * Represents a double value in the FaunaDB query language.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see Language#Value(double)
   * @see Value
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
   * Represents a long value in the FaunaDB query language.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see Language#Value(long)
   * @see Value
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
   * Represents a string value in the FaunaDB query language.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see Language#Value(String)
   * @see Value
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

    @Override
    public String toString() {
        return format("\"%s\"", value);
    }
  }

  /**
   * Represents a null value in the FaunaDB query language.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see Language#Null()
   * @see Value
   */
  public static final class NullV extends Value {

    public static final NullV NULL = new NullV();

    private NullV() {
    }

    @Override
    @JsonIgnore
    public Optional<Value> getOptional() {
      return Optional.empty();
    }

    @Override
    @JsonValue
    protected NullNode toJson() {
      return NullNode.getInstance();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof NullV;
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
   * Represents a timestamp value in the FaunaDB query language.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see Language#Value(Instant)
   * @see Value
   */
  public static final class TimeV extends ScalarValue<Instant> {

    public TimeV(Instant value) {
      super(value);
    }

    @JsonCreator
    private TimeV(@JsonProperty("@ts") String value) {
      this(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(value)));
    }

    Instant truncated() {
      return value;
    }

    @Override
    @JsonProperty("@ts")
    protected String toJson() {
      return value.toString();
    }

  }

  /**
   * Represents a date value in the FaunaDB query language.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see Language#Value(LocalDate)
   * @see Value
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
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see <a href="https://app.fauna.com/documentation/reference/queryapi#special-type">FaunaDB Special Types</a>
   * @see Value
   */
  public static final class SetRefV extends ScalarValue<Map<String, Value>> {

    public SetRefV(@JsonProperty("@set") Map<String, Value> parameters) {
      super(parameters);
    }

    /**
     * Extract SetRefV structure
     *
     * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
     *
     * @return SetRefV internal structure
     */
    public Map<String, Value> parameters() {
      return value;
    }

    @Override
    @JsonProperty("@set")
    protected Map<String, Value> toJson() {
      return value;
    }

    @Override
    public String toString() {
        String set = value.entrySet()
            .stream()
            .map(e -> String.join(": ", Arrays.asList(e.getKey(), String.valueOf(e.getValue()))))
            .collect(Collectors.joining(", ", "{", "}"));

        return format("{@set = %s}", set);
    }
  }

  /**
   * The {@link RefV} internal ID representation.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see RefV
   */
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  public static class RefID {
    @JsonProperty("id")       private final String id;
    @JsonProperty("class")    private final RefV clazz;
    @JsonProperty("database") private final RefV database;

    private RefID(String id, RefV clazz, RefV database) {
      this.id = id;
      this.clazz = clazz;
      this.database = database;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof RefID))
        return false;

      RefID other = (RefID) obj;

      return Objects.equals(id, other.id) &&
        Objects.equals(clazz, other.clazz) &&
        Objects.equals(database, other.database);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, clazz, database);
    }
  }

  /**
   * A FaunaDB reference type.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see <a href="https://app.fauna.com/documentation/reference/queryapi#special-type">FaunaDB Special Types</a>
   * @see Language#Ref(String)
   * @see Language#Ref(Expr, Expr)
   * @see Language#Ref(Expr, String)
   * @see Value
   */
  public static final class RefV extends Value.ScalarValue<RefID> {

    public RefV(String id, RefV clazz, RefV database) {
      super(new RefID(id, clazz, database));
    }

    public RefV(String id, RefV clazz) {
      this(id, clazz, null);
    }

    @JsonIgnore
    public String getId() {
      return value.id;
    }

    @JsonIgnore
    public Optional<RefV> getClazz() {
      return Optional.ofNullable(value.clazz);
    }

    @JsonIgnore
    public Optional<RefV> getDatabase() {
      return Optional.ofNullable(value.database);
    }

    @Override
    @JsonProperty("@ref")
    protected Object toJson() {
      return value;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof RefV))
        return false;

      RefV other = (RefV) obj;

      return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public String toString() {
      String cls = value.clazz != null ? format(", class = %s", value.clazz) : "";
      String db = value.database != null ? format(", database = %s", value.database) : "";
      return format("ref(id = \"%s\"%s%s)", value.id, cls, db);
    }
  }

  /**
   * Builtin reference types.
   *
   * @see Language#Ref(Expr, Expr)
   * @see Language#Ref(Expr, String)
   * @see RefV
   */
  public static final class Native {
    private Native() {}

    public static final RefV CLASSES = new RefV("classes", null, null);
    public static final RefV INDEXES = new RefV("indexes", null, null);
    public static final RefV DATABASES = new RefV("databases", null, null);
    public static final RefV KEYS = new RefV("keys", null, null);
    public static final RefV FUNCTIONS = new RefV("functions", null, null);

    public static RefV fromName(String name) {
      switch (name) {
        case "classes": return CLASSES;
        case "indexes": return INDEXES;
        case "databases": return DATABASES;
        case "keys": return KEYS;
        case "functions": return FUNCTIONS;
      }

      return new RefV(name, null, null);
    }
  }

  /**
   * Represents a blob value in the FaunaDB query language.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see <a href="https://app.fauna.com/documentation/reference/queryapi#special-type">FaunaDB Special Types</a>
   * @see Language#Value(byte[])
   * @see Value
   */
  public static final class BytesV extends ScalarValue<byte[]> {

    public BytesV(byte[] bytes) {
      super(bytes);
    }

    @JsonCreator
    public BytesV(@JsonProperty("@bytes") String urlSafeBase64) {
      super(Base64.getUrlDecoder().decode(urlSafeBase64));
    }

    @Override
    @JsonProperty("@bytes")
    protected Object toJson() {
      return Base64.getUrlEncoder().encodeToString(value);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof BytesV &&
        Arrays.equals(this.value, ((BytesV) other).value);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return IntStream.range(0, value.length)
            .mapToObj(i -> format("0x%02x", value[i]))
            .collect(Collectors.joining(" ", "[", "]"));
    }
  }

  /**
   * Represents a query value in the FaunaDB query language.
   *
   * <p><b>WARNING:</b> Internal API. Must not be used in production code.</p>
   *
   * @see <a href="https://app.fauna.com/documentation/reference/queryapi#special-type">FaunaDB Special Types</a>
   * @see Value
   */
  @JsonDeserialize(using = JsonDeserializer.None.class) // Disables generic value deserializer for query values
  public static final class QueryV extends Value {

    private Map<String, Object> lambda;

    private QueryV(@JsonProperty("@query") Map<String, Object> lambda) {
      this.lambda = lambda;
    }

    @Override
    @JsonProperty("@query")
    protected Map<String, Object> toJson() {
      return lambda;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof QueryV && lambda.equals(((QueryV)obj).lambda);
    }

    @Override
    public int hashCode() {
      return lambda.hashCode();
    }
  }
}
