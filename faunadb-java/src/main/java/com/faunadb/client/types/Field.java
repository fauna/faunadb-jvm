package com.faunadb.client.types;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import static java.lang.String.format;

/**
 * A field extractor for a FaunaDB {@link Value}
 *
 * @see Value
 * @see Codec
 */
public final class Field<T> {

  private final class CollectionCodec<A> implements Codec<ImmutableList<A>> {
    private final Path path;
    private final Field<A> field;

    public CollectionCodec(Path path, Field<A> field) {
      this.path = path;
      this.field = field;
    }

    @Override
    public Result<ImmutableList<A>> apply(Value input) {
      return input.to(ARRAY).flatMap(toList);
    }

    private final Function<ImmutableList<Value>, Result<ImmutableList<A>>> toList =
      new Function<ImmutableList<Value>, Result<ImmutableList<A>>>() {
        @Override
        public Result<ImmutableList<A>> apply(ImmutableList<Value> values) {
          ImmutableList.Builder<A> success = ImmutableList.builder();
          ImmutableList.Builder<String> failures = ImmutableList.builder();

          for (int i = 0; i < values.size(); i++) {
            Result<A> res = field.get(values.get(i));

            if (res.isSuccess()) {
              success.add(res.get());
            } else {
              Path subPath = path.subPath(Path.from(i)).subPath(field.path);
              failures.add(format("\"%s\" %s", subPath, res));
            }
          }

          ImmutableList<String> allErrors = failures.build();
          if (!allErrors.isEmpty())
            return Result.fail(format("Failed to collect values: %s", Joiner.on(", ").join(allErrors)));

          return Result.success(success.build());
        }
      };
  }

  /**
   * Creates a field that extracts its value from a object path, assuming the value
   * is an intance of {@link com.faunadb.client.types.Value.ObjectV}.
   *
   * @param keys path to the field
   * @return the field extractor
   */
  public static Field<Value> at(String... keys) {
    return new Field<>(Path.from(keys), Codec.VALUE);
  }

  /**
   * Creates a field that extracts its value from a array index, assuming the value
   * is an intance of {@link com.faunadb.client.types.Value.ArrayV}.
   *
   * @param indexes path to the field
   * @return the field extractor
   */
  public static Field<Value> at(int... indexes) {
    return new Field<>(Path.from(indexes), Codec.VALUE);
  }

  /**
   * Creates a field that coerces its value using the codec passaed
   *
   * @param codec codec used to coerce the field's value
   * @return the field extractor
   */
  public static <T> Field<T> as(Codec<T> codec) {
    return new Field<>(Path.empty(), codec);
  }

  static Field<Value> root() {
    return new Field<>(Path.empty(), Codec.VALUE);
  }

  private final Path path;
  private final Codec<T> codec;

  private Field(Path path, Codec<T> codec) {
    this.path = path;
    this.codec = codec;
  }

  /**
   * Creates a field extractor composed with another nested field
   *
   * @param other nested field to compose with
   * @return a new field extractor with the nested field
   */
  public <A> Field<A> at(Field<A> other) {
    return new Field<>(path.subPath(other.path), other.codec);
  }

  /**
   * Creates a field extractor that coerces its value using the codec passed
   *
   * @param codec codec to be used to coerce the field's value
   * @return a new field that coerces its value using the codec passed
   */
  public <A> Field<A> to(Codec<A> codec) {
    return new Field<>(path, codec);
  }

  /**
   * Creates a field extractor that collects each inner value of an array using the nested field passed,
   * assuming the root value is an instance of {@link com.faunadb.client.types.Value.ArrayV}
   *
   * @param field field to be extracted from each array's element
   * @return a new field that collects each innver value using the field passed
   */
  public <A> Field<ImmutableList<A>> collect(Field<A> field) {
    return new Field<>(path, new CollectionCodec<>(path, field));
  }

  Result<T> get(Value root) {
    return path.get(root).flatMap(codec);
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