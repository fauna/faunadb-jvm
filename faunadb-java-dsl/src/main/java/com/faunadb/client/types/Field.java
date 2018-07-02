package com.faunadb.client.types;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * A field extractor for a FaunaDB {@link Value}.
 *
 * <p>The field extractor can be used to extract field's values from key/value maps
 * or collections returned by FaunaDB.</p>
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 * Field<String> userNameField = Field.at("data", "name").to(String.class);
 * Value result = client.query(getUser).get();
 * String name = result.get(userNameField);
 * }</pre>
 *
 * @see Value
 */
public final class Field<T> {

  private final class CollectionCodec<A> implements Codec<List<A>> {
    private final Path path;
    private final Field<A> field;

    public CollectionCodec(Path path, Field<A> field) {
      this.path = path;
      this.field = field;
    }

    @Override
    public Result<List<A>> decode(Value input) {
      return input.to(ARRAY).<List<A>>flatMap(toList);
    }

    @Override
    public Result<Value> encode(List<A> value) {
      throw new IllegalArgumentException("not implemented");
    }

    private final Function<List<Value>, Result<List<A>>> toList =
      new Function<List<Value>, Result<List<A>>>() {
        @Override
        public Result<List<A>> apply(List<Value> values) {
          List<A> success = new ArrayList<>();
          List<String> failures = new ArrayList<>();

          for (int i = 0; i < values.size(); i++) {
            Result<A> res = field.get(values.get(i));

            if (res.isSuccess()) {
              success.add(res.get());
            } else {
              Path subPath = path.subPath(Path.from(i)).subPath(field.path);
              failures.add(format("\"%s\" %s", subPath, res));
            }
          }

          if (!failures.isEmpty())
            return Result.fail(format("Failed to collect values: %s", Joiner.on(", ").join(failures)));

          return Result.<List<A>>success(success);
        }
      };
  }

  /**
   * Creates a field that extracts the underlying value from the path provided, assuming the {@link Value} instance
   * is a key/value map.
   *
   * @param keys the path to the desired field
   * @return a new {@link Field} instance
   * @see Field
   * @see Value
   */
  public static Field<Value> at(String... keys) {
    return new Field<>(Path.from(keys), Codec.VALUE);
  }

  /**
   * Creates a field that extracts the underlying value from the indexes provided, assuming the {@link Value} instance
   * is a collection or nested collections.
   *
   * @param indexes the path to the desired field
   * @return a new {@link Field} instance
   */
  public static Field<Value> at(int... indexes) {
    return new Field<>(Path.from(indexes), Codec.VALUE);
  }

  /**
   * Creates a field that converts its underlying value using the {@link Codec} provided.
   *
   * @param <T> the desired final type
   * @param codec {@link Codec} used to convert the field's value
   * @return a new {@link Field} instance
   */
  public static <T> Field<T> as(Codec<T> codec) {
    return new Field<>(Path.empty(), codec);
  }

  static Field<Value> root() {
    return new Field<>(Path.empty(), Codec.VALUE);
  }

  private final Path path;
  private final Codec<T> codec;
  private final Function<Value, Result<T>> codecFn = new Function<Value, Result<T>>() {
    @Override
    public Result<T> apply(Value input) {
      return codec.decode(input);
    }
  };

  private Field(Path path, Codec<T> codec) {
    this.path = path;
    this.codec = codec;
  }

  /**
   * Creates a field extractor composed with another nested {@link Field} instance.
   *
   * @param <A> the desired final type
   * @param other a nested {@link Field} to compose with
   * @return a new {@link Field} instance
   */
  public <A> Field<A> at(Field<A> other) {
    return new Field<>(path.subPath(other.path), other.codec);
  }

  /**
   * Creates a field extractor that converts its value using the codec provided.
   *
   * @param <A> the desired final type
   * @param codec the {@link Codec} to be used to convert the field's underlying value
   * @return a new {@link Field} instance
   */
  public <A> Field<A> to(Codec<A> codec) {
    return new Field<>(path, codec);
  }

  /**
   * Creates a field extractor that converts its value to the class provided.
   *
   * @param <A> the desired final type
   * @param type the class to be used to convert the field's value
   * @return a new {@link Field} instance
   */
  public <A> Field<A> to(final Class<A> type) {
    return new Field<>(path, new Codec<A>() {
      @Override
      public Result<A> decode(Value value) {
        return Decoder.decode(value, type);
      }

      @Override
      public Result<Value> encode(A value) {
        return Encoder.encode(value);
      }
    });
  }

  /**
   * Assuming the {@link Value} instance is a collection, creates a field extractor that collects
   * the {@link Field} provided for each element in the underlying collection.
   *
   * @param <A> the desired final type for each collection element
   * @param field the {@link Field} to be extracted from each collection element
   * @return a new {@link Field} instance
   */
  public <A> Field<List<A>> collect(Field<A> field) {
    return new Field<>(path, new CollectionCodec<>(path, field));
  }

  Result<T> get(Value root) {
    return path.get(root).flatMap(codecFn);
  }

  @Override
  public boolean equals(Object other) {
    return other != null &&
      other instanceof Field &&
      this.path.equals(((Field) other).path);
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }

  @Override
  public String toString() {
    return path.toString();
  }
}
