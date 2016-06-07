package com.faunadb.client.types;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;

/**
 * Represents the result of an operation. Usually a coercion operation.
 *
 * @see Codec
 */
public final class Result<T> {

  private final Optional<T> value;
  private final Optional<String> error;

  /**
   * Creates a successful result
   *
   * @param value result's value
   * @return a successful result
   */
  public static <T> Result<T> success(T value) {
    return new Result<>(Optional.of(value), Optional.<String>absent());
  }

  /**
   * Creates failure result
   *
   * @param error the error message
   * @return a failure result
   */
  public static <T> Result<T> fail(String error) {
    return new Result<>(Optional.<T>absent(), Optional.of(error));
  }

  private Result(Optional<T> value, Optional<String> error) {
    this.value = value;
    this.error = error;
  }

  /**
   * @return true if the operation was successful
   */
  public boolean isSuccess() {
    return value.isPresent();
  }

  /**
   * @return true if the operation has failed
   */
  public boolean isFailure() {
    return !value.isPresent();
  }

  /**
   * Extracts the resulting value or throw an exception if the operation has failed.
   *
   * @return the result value
   * @throws IllegalStateException if the operation has failed
   */
  public T get() {
    return value.or(new Supplier<T>() {
      @Override
      public T get() {
        throw new IllegalStateException(error.get());
      }
    });
  }

  /**
   * Gets an {@link Optional} type containing the result value if the operation was successful.
   *
   * @return an {@link Optional} with the result value, if success
   */
  public Optional<T> getOptional() {
    return value;
  }

  /**
   * Gets the result value or return the a default value if the operation has failed
   *
   * @return the result value of the default value
   */
  public T getOrElse(T defaultValue) {
    return value.or(defaultValue);
  }

  /**
   * Apply the function passed on the result value.
   *
   * @param fn the map function to be applied
   * @return if this is a successful result, return a new successful result with the map function result.
   * If this is a failure, returns a new faulure with the name error message.
   */
  public <U> Result<U> map(Function<T, U> fn) {
    Optional<U> res = value.transform(fn);
    if (res.isPresent())
      return success(res.get());

    return fail(error.get());
  }

  /**
   * Apply the function passed on the result value.
   *
   * @param fn the map function to be applied
   * @return if this is a successful result, returns the map function result.
   * If this is a failure, returns a new failure with the same error message.
   */
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
