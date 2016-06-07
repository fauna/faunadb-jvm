package com.faunadb.client.types;

import com.faunadb.client.types.Value.*;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.LocalDate;

import static java.lang.String.format;

/**
 * Codec is a function that represents an attempt to coerce a {@link Value} to a concrete type.
 * There are pre-defined codecs for each FaunaDB primitive types: {@link Codec#VALUE}, {@link Codec#STRING},
 * {@link Codec#LONG}, {@link Codec#DOUBLE}, {@link Codec#DATE}, {@link Codec#TS}, {@link Codec#REF},
 * {@link Codec#SET_REF}, {@link Codec#ARRAY}, and {@link Codec#OBJECT}.
 * <p>
 * Codecs return a {@link Result} of the coercion attempt. If it fails to coerce, {@link Result}
 * will contain an error message.
 * <p>
 * It is also possible to create customized codecs to handle complex objects:
 * <pre>{@code
 * class Person {
 *   static final Codec<Person> PERSON = new Codec<Person>() {
 *     @Override public Result<Person> apply(Value value) {
 *       return Result.success(new Person(
 *         value.at("data", "firstName").as(Codec.STRING),
 *         value.at("data", "lastName").as(Codec.STRING)
 *       ));
 *     }
 *   }
 *
 *   static Person fromValue(Value value) {
 *     return value.as(PERSON);
 *   }
 *
 *   final String firstName, lastName;
 *
 *   Person(String firstName, String lastName) {
 *     this.firstName = firstName;
 *     this.lastName = lastName;
 *   }
 * }
 * }</pre>
 *
 * @param <T> desired resulting type
 * @see Result
 */
public interface Codec<T> extends Function<Value, Result<T>> {

  /**
   * Coerce a {@link Value} to itself or fail value is an instance of {@link NullV}.
   */
  Codec<Value> VALUE = new Codec<Value>() {
    @Override
    public Result<Value> apply(Value value) {
      if (value == NullV.NULL)
        return Result.fail("Value is null");

      return Result.success(value);
    }
  };

  /**
   * Coerce a {@link Value} to an instance of {@link Ref}
   */
  Codec<Ref> REF = Cast.mapTo(Ref.class, Functions.<Ref>identity());

  /**
   * Coerce a {@link Value} to an instance of {@link SetRef}
   */
  Codec<SetRef> SET_REF = Cast.mapTo(SetRef.class, Functions.<SetRef>identity());

  /**
   * Coerce a {@link Value} to a {@link Long}
   */
  Codec<Long> LONG = Cast.mapTo(LongV.class, Cast.<LongV, Long>scalarValue());

  /**
   * Coerce a {@link Value} to an {@link Instant}
   */
  Codec<Instant> TS = Cast.mapTo(TsV.class, Cast.<TsV, Instant>scalarValue());

  /**
   * Coerce a {@link Value} to a {@link String}
   */
  Codec<String> STRING = Cast.mapTo(StringV.class, Cast.<StringV, String>scalarValue());

  /**
   * Coerce a {@link Value} to a {@link Double}
   */
  Codec<Double> DOUBLE = Cast.mapTo(DoubleV.class, Cast.<DoubleV, Double>scalarValue());

  /**
   * Coerce a {@link Value} to a {@link Boolean}
   */
  Codec<Boolean> BOOLEAN = Cast.mapTo(BooleanV.class, Cast.<BooleanV, Boolean>scalarValue());

  /**
   * Coerce a {@link Value} to a {@link LocalDate}
   */
  Codec<LocalDate> DATE = Cast.mapTo(DateV.class, Cast.<DateV, LocalDate>scalarValue());

  /**
   * Coerce a {@link Value} to an {@link ImmutableList} of {@link Value}
   */
  Codec<ImmutableList<Value>> ARRAY = Cast.mapTo(ArrayV.class, new Function<ArrayV, ImmutableList<Value>>() {
    @Override
    public ImmutableList<Value> apply(ArrayV input) {
      return input.values;
    }
  });

  /**
   * Coerce a {@link Value} to an {@link ImmutableMap} of {@link Value}
   */
  Codec<ImmutableMap<String, Value>> OBJECT = Cast.mapTo(ObjectV.class, new Function<ObjectV, ImmutableMap<String, Value>>() {
    @Override
    public ImmutableMap<String, Value> apply(ObjectV input) {
      return input.values;
    }
  });
}

final class Cast {

  static <V extends Value, O> Codec<O> mapTo(final Class<V> clazz, final Function<V, O> extractValue) {
    return new Codec<O>() {
      @Override
      public Result<O> apply(Value input) {
        return cast(clazz, input).map(extractValue);
      }
    };
  }

  private static <T> Result<T> cast(Class<T> clazz, Value value) {
    if (value.getClass() == clazz)
      return Result.success(clazz.cast(value));

    return Result.fail(
      format("Can not convert %s to %s", value.getClass().getSimpleName(), clazz.getSimpleName()));
  }

  static <T extends ScalarValue<R>, R> Function<T, R> scalarValue() {
    return new Function<T, R>() {
      @Override
      public R apply(T input) {
        return input.value;
      }
    };
  }

}