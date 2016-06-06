package com.faunadb.client.types;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;

public final class Result<T> {

  private final Optional<T> value;
  private final Optional<String> error;

  public static <T> Result<T> success(T value) {
    return new Result<>(Optional.of(value), Optional.<String>absent());
  }

  public static <T> Result<T> fail(String error) {
    return new Result<>(Optional.<T>absent(), Optional.of(error));
  }

  private Result(Optional<T> value, Optional<String> error) {
    this.value = value;
    this.error = error;
  }

  public boolean isSuccess() {
    return value.isPresent();
  }

  public Optional<T> asOpt() {
    return value;
  }

  public T getOrThrow() {
    return value.or(new Supplier<T>() {
      @Override
      public T get() {
        throw new IllegalStateException(error.get());
      }
    });
  }

  @SuppressWarnings("unchecked") // Only cast if result is a failure. Get will throw exception.
  public <U> Result<U> map(Function<T, Result<U>> fn) {
    if (value.isPresent())
      return fn.apply(value.get());

    return (Result<U>) this;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof Result))
      return false;

    Result otherResult = (Result) other;
    return this.value.equals(otherResult.value)
      && this.error.equals(otherResult.error);
  }

  @Override
  public int hashCode() {
    return value.hashCode() + error.hashCode();
  }

  @Override
  public String toString() {
    return value.isPresent()
      ? value.get().toString()
      : error.get();
  }
}
