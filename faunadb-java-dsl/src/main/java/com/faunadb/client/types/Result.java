package com.faunadb.client.types;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * Represents the result of an operation. Usually a conversion operation.
 *
 * @see Codec
 */
public abstract class Result<T> {

  private static final class Success<A> extends Result<A> {

    private final A value;

    private Success(A value) {
      this.value = value;
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public boolean isFailure() {
      return false;
    }

    @Override
    public A get() {
      return value;
    }

    @Override
    public Optional<A> getOptional() {
      return Optional.of(value);
    }

    @Override
    public A getOrElse(A defaultValue) {
      return value;
    }

    @Override
    public <U> Result<U> map(Function<A, U> fn) {
      return new Success<>(fn.apply(value));
    }

    @Override
    public <U> Result<U> flatMap(Function<A, Result<U>> fn) {
      return fn.apply(value);
    }

    @Override
    public boolean equals(Object other) {
      return other != null &&
        other instanceof Success &&
        this.value.equals(((Success) other).value);
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

  private static final class Failure<A> extends Result<A> {

    private final String error;
    private final Throwable cause;

    private Failure(String error, Throwable cause) {
      this.error = error;
      this.cause = cause;
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public boolean isFailure() {
      return true;
    }

    @Override
    public A get() {
      throw new IllegalStateException(error, cause);
    }

    @Override
    public Optional<A> getOptional() {
      return Optional.absent();
    }

    @Override
    public A getOrElse(A defaultValue) {
      return defaultValue;
    }

    @Override
    public <U> Result<U> map(Function<A, U> fn) {
      return new Failure<>(error, cause);
    }

    @Override
    public <U> Result<U> flatMap(Function<A, Result<U>> fn) {
      return new Failure<>(error, cause);
    }

    @Override
    public boolean equals(Object other) {
      return other != null &&
        other instanceof Failure &&
        this.error.equals(((Failure) other).error);
    }

    @Override
    public int hashCode() {
      return error.hashCode();
    }

    @Override
    public String toString() {
      return error;
    }
  }

  /**
   * Creates a successful result
   *
   * @param <T> the type of the result
   * @param value result's value
   * @return a successful result
   */
  public static <T> Result<T> success(T value) {
    return new Success<>(value);
  }

  /**
   * Creates failure result
   *
   * @param <T> the type of the result
   * @param error the error message
   * @return a failure result
   */
  public static <T> Result<T> fail(String error) {
    return new Failure<>(error, null);
  }

  /**
   * Creates failure result with an exception
   *
   * @param <T> the type of the result
   * @param error the error message
   * @param cause the exception that caused this failure
   * @return a failure result
   */
  public static <T> Result<T> fail(String error, Throwable cause) {
    return new Failure<>(error, cause);
  }

  private Result() {
  }

  /**
   * @return true if the operation was successful
   */
  public abstract boolean isSuccess();

  /**
   * @return true if the operation has failed
   */
  public abstract boolean isFailure();

  /**
   * Extracts the resulting value or throw an exception if the operation has failed.
   *
   * @return the result value
   * @throws IllegalStateException if the operation has failed
   */
  public abstract T get();

  /**
   * Gets an {@link Optional} type containing the result value if the operation was successful.
   *
   * @return an {@link Optional} with the result value, if success
   */
  public abstract Optional<T> getOptional();

  /**
   * Gets the result value or return the a default value if the operation has failed
   *
   * @param defaultValue default value to return case this represents a failure
   * @return the result value of the default value
   */
  public abstract T getOrElse(T defaultValue);

  /**
   * Apply the function passed on the result value.
   *
   * @param <U> the type of the result
   * @param fn the map function to be applied
   * @return if this is a successful result, return a new successful result with the map function result.
   * If this is a failure, returns a new failure with the same error message.
   */
  public abstract <U> Result<U> map(Function<T, U> fn);

  /**
   * Apply the function passed on the result value.
   *
   * @param <U> the type of the result
   * @param fn the map function to be applied
   * @return if this is a successful result, returns the map function result.
   * If this is a failure, returns a new failure with the same error message.
   */
  public abstract <U> Result<U> flatMap(Function<T, Result<U>> fn);

  /**
   * Extracts the resulting value or returns null if the operation has failed.
   *
   * @return the result value or null
   */
  public T orNull() {
    return getOptional().orNull();
  }

}
