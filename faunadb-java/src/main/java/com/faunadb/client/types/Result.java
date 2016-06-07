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

  public boolean isFailure() {
    return !value.isPresent();
  }

  public T get() {
    return value.or(new Supplier<T>() {
      @Override
      public T get() {
        throw new IllegalStateException(error.get());
      }
    });
  }

  public Optional<T> getOptional() {
    return value;
  }

  public T getOrElse(T defaultValue) {
    return value.or(defaultValue);
  }

  public <U> Result<U> map(Function<T, U> fn) {
    Optional<U> res = value.transform(fn);
    if (res.isPresent())
      return success(res.get());

    return fail(error.get());
  }

  public <U> Result<U> flatMap(Function<T, Result<U>> fn) {
    Optional<Result<U>> opt = value.transform(fn);
    if (opt.isPresent())
      return opt.get();

    return fail(error.get());
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
