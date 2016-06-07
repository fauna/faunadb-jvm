package com.faunadb.client.types;

public final class Field<T> {

  public static <T> Field<T> to(Codec<T> codec) {
    return new Field<>(Path.empty(), codec);
  }

  public static Field<Value> at(String... keys) {
    return new Field<>(Path.from(keys), Codec.VALUE);
  }

  public static Field<Value> at(int... indexes) {
    return new Field<>(Path.from(indexes), Codec.VALUE);
  }

  private final Path path;
  private final Codec<T> codec;

  private Field(Path path, Codec<T> codec) {
    this.path = path;
    this.codec = codec;
  }

  public <A> Field<A> at(Field<A> other) {
    return new Field<>(path.subPath(other.path), other.codec);
  }

  public <A> Field<A> as(Codec<A> codec) {
    return new Field<>(path, codec);
  }

  Result<T> get(Value root) {
    return path.get(root).flatMap(codec);
  }

}