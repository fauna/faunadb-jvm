package com.faunadb.client.types;

/**
 * A field extractor for a FaunaDB {@link Value}
 *
 * @see Value
 * @see Codec
 */
public final class Field<T> {

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
  public static <T> Field<T> to(Codec<T> codec) {
    return new Field<>(Path.empty(), codec);
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
   * Creates a field extractor that coerce's its value using the codec passed
   *
   * @param codec codec to be used to coerce the field's value
   * @return a new field that coerces its value using the codec passed
   */
  public <A> Field<A> as(Codec<A> codec) {
    return new Field<>(path, codec);
  }

  Result<T> get(Value root) {
    return path.get(root).flatMap(codec);
  }

}