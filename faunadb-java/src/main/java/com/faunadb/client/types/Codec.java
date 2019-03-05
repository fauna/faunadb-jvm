package com.faunadb.client.types;

import com.faunadb.client.types.Value.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.String.format;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Codec instances are used to convert a {@link Value} to a concrete type.
 * There are pre-defined codecs for each FaunaDB primitive types.
 *
 * Each Codec implementation returns a {@link Result} instance for the conversion attempt.
 * If the conversion attempt fails, the {@link Result} instance will contain an error message.
 *
 * Custom codecs can be created by implementing the Codec interface.
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 * class User {
 *   static final Codec<User> USER_CODEC = new Codec<User>() {
 *     public Result<User> decode(Value value) {
 *       return Result.success(new User(
 *         value.at("username").to(String.class).getOrElse("<no name>"),
 *         value.at("password").to(String.class).getOrElse(null)
 *       ));
 *     }
 *
 *     public Result<Value> encode(User user) {
 *       return Value.from(ImmutableMap.of(
 *         "username", user.username,
 *         "password", person.password
 *       ));
 *     }
 *   }
 *
 *   final String username, password;
 *
 *   Person(String username, String password) {
 *     this.firstName = firstName;
 *     this.lastName = lastName;
 *   }
 * }
 *
 * //...
 * Value result = client.query(getUser).get();
 * Result<User> user = result.at("data").to(User.USER_CODEC);
 * }</pre>
 *
 * <p>It is possible to annotate classes and let the internal framework encode/decode instances of the class automatically.</p>
 * <p>Refer to the annotations {@link FaunaField}, {@link FaunaConstructor}, {@link FaunaIgnore} and {@link FaunaEnum} for more details.</p>
 *
 * @param <T> desired resulting type
 * @see FaunaField
 * @see FaunaConstructor
 * @see FaunaEnum
 * @see FaunaIgnore
 * @see Encoder
 * @see Decoder
 * @see com.faunadb.client.query.Language#Value(Object)
 * @see Result
 */
public interface Codec<T> {
  Result<T> decode(Value value);
  Result<Value> encode(T value);

  /**
   * Convert a {@link Value} to itself. Returns a failed {@link Result} if null.
   */
  Codec<Value> VALUE = new Codec<Value>() {
    @Override
    public Result<Value> decode(Value value) {
      if (value == NullV.NULL)
        return Result.fail("Value is null");

      return Result.success(value);
    }

    @Override
    public Result<Value> encode(Value value) {
      if (value == NullV.NULL)
        return Result.fail("Value is null");

      return Result.success(value);
    }
  };

  /**
   * Converts a {@link Value} to a {@link RefV}
   */

  Codec<RefV> REF = Transformations.mapTo(RefV.class, Function.identity(), Transformations.upCast());
  /**
   * Converts a {@link Value} to a {@link SetRefV}
   */
  Codec<SetRefV> SET_REF = Transformations.mapTo(SetRefV.class, Function.identity(), Transformations.upCast());

  /**
   * Converts a {@link Value} to a {@link Long}
   */
  Codec<Long> LONG = Transformations.mapTo(LongV.class, Transformations.scalarValue(), Transformations.LONG_TO_VALUE);

  /**
   * Converts a {@link Value} to a {@link Integer}
   */
  Codec<Integer> INTEGER = Transformations.mapWith(LONG, Transformations.LONG_TO_INTEGER, Transformations.INTEGER_TO_LONG);

  /**
   * Converts a {@link Value} to a {@link Short}
   */
  Codec<Short> SHORT = Transformations.mapWith(LONG, Transformations.LONG_TO_SHORT, Transformations.SHORT_TO_LONG);

  /**
   * Converts a {@link Value} to a {@link Byte}
   */
  Codec<Byte> BYTE = Transformations.mapWith(LONG, Transformations.LONG_TO_BYTE, Transformations.BYTE_TO_LONG);

  /**
   * Converts a {@link Value} to a {@link Character}
   */
  Codec<Character> CHAR = Transformations.mapWith(LONG, Transformations.LONG_TO_CHAR, Transformations.CHAR_TO_LONG);

  /**
   * Converts a {@link Value} to an {@link Instant}
   */
  Codec<Instant> TIME = Transformations.mapTo(TimeV.class, Transformations.VALUE_TO_INSTANT, Transformations.INSTANT_TO_VALUE);

  /**
   * Converts a {@link Value} to a {@link String}
   */
  Codec<String> STRING = Transformations.mapTo(StringV.class, Transformations.scalarValue(), Transformations.STRING_TO_VALUE);

  /**
   * Converts a {@link Value} to a {@link Double}
   */
  Codec<Double> DOUBLE = Transformations.mapTo(DoubleV.class, Transformations.scalarValue(), Transformations.DOUBLE_TO_VALUE);

  /**
   * Converts a {@link Value} to a {@link Float}
   */
  Codec<Float> FLOAT = Transformations.mapWith(DOUBLE, Transformations.DOUBLE_TO_FLOAT, Transformations.FLOAT_TO_DOUBLE);

  /**
   * Converts a {@link Value} to a {@link Boolean}
   */
  Codec<Boolean> BOOLEAN = Transformations.mapTo(BooleanV.class, Transformations.scalarValue(), Transformations.BOOLEAN_TO_VALUE);

  /**
   * Converts a {@link Value} to a {@link LocalDate}
   */
  Codec<LocalDate> DATE = Transformations.mapTo(DateV.class, Transformations.scalarValue(), Transformations.LOCAL_DATE_TO_VALUE);

  /**
   * Converts a {@link Value} to an {@link List} of {@link Value}
   */
  Codec<List<Value>> ARRAY = Transformations.mapTo(ArrayV.class, Transformations.VALUE_TO_LIST, Transformations.LIST_TO_VALUE);

  /**
   * Converts a {@link Value} to a {@link Map} of {@link String} to {@link Value}
   */
  Codec<Map<String, Value>> OBJECT = Transformations.mapTo(ObjectV.class, Transformations.VALUE_TO_MAP, Transformations.MAP_TO_VALUE);

  /**
   * Converts a {@link Value} to an array of bytes
   */
  Codec<byte[]> BYTES = Transformations.mapTo(BytesV.class, Transformations.scalarValue(), Transformations.BYTES_TO_VALUE);
}

final class Transformations {

  static <V extends Value, O> Codec<O> mapTo(final Class<V> clazz, final Function<V, O> extractValue, final Function<O, Value> wrapValue) {
    return new Codec<O>() {
      @Override
      public Result<O> decode(Value input) {
        try {
          return cast(clazz, input).map(extractValue);
        } catch (Exception ex) {
          return Result.fail(ex.getMessage(), ex);
        }
      }

      @Override
      public Result<Value> encode(O value) {
        try {
          return Result.success(wrapValue.apply(value));
        } catch (Exception ex) {
          return Result.fail(ex.getMessage(), ex);
        }
      }
    };
  }

  static <I, O> Codec<O> mapWith(final Codec<I> codec, final Function<I, O> mapFunction, final Function<O, I> unmapFunction) {
    return new Codec<O>() {
      @Override
      public Result<O> decode(Value input) {
        try {
          return codec.decode(input).map(mapFunction);
        } catch (Exception ex) {
          return Result.fail(ex.getMessage(), ex);
        }
      }

      @Override
      public Result<Value> encode(O value) {
        try {
          return codec.encode(unmapFunction.apply(value));
        } catch (Exception ex) {
          return Result.fail(ex.getMessage(), ex);
        }
      }
    };
  }

  private static <T> Result<T> cast(Class<T> clazz, Value value) {
    if (clazz.isInstance(value))
      return Result.success(clazz.cast(value));

    return Result.fail(
      format("Can not convert %s to %s", value.getClass().getSimpleName(), clazz.getSimpleName()));
  }

  static <T extends ScalarValue<R>, R> Function<T, R> scalarValue() {
    return input -> input.value;
  }

  @SuppressWarnings("unchecked")
  static <I extends O, O> Function<I, O> upCast() {
    return (Function) Function.identity();
  }

  /// Cast functions

  final static Function<Long, Integer> LONG_TO_INTEGER = Long::intValue;

  final static Function<Integer, Long> INTEGER_TO_LONG = Integer::longValue;

  final static Function<Long, Short> LONG_TO_SHORT = Long::shortValue;

  final static Function<Short, Long> SHORT_TO_LONG = Short::longValue;

  final static Function<Long, Byte> LONG_TO_BYTE = Long::byteValue;

  final static Function<Byte, Long> BYTE_TO_LONG = Byte::longValue;

  final static Function<Long, Character> LONG_TO_CHAR = input -> (char) input.longValue();

  final static Function<Character, Long> CHAR_TO_LONG = Long::valueOf;

  final static Function<Double, Float> DOUBLE_TO_FLOAT = Double::floatValue;

  final static Function<Float, Double> FLOAT_TO_DOUBLE = Float::doubleValue;

  /// Wrap functions

  final static Function<Long, Value> LONG_TO_VALUE = LongV::new;

  final static Function<Double, Value> DOUBLE_TO_VALUE = DoubleV::new;

  final static Function<String, Value> STRING_TO_VALUE = StringV::new;

  final static Function<Boolean, Value> BOOLEAN_TO_VALUE = BooleanV::valueOf;

  final static Function<Instant, Value> INSTANT_TO_VALUE = TimeV::new;

  final static Function<LocalDate, Value> LOCAL_DATE_TO_VALUE = DateV::new;

  final static Function<Map<String, Value>, Value> MAP_TO_VALUE = ObjectV::new;

  final static Function<List<Value>, Value> LIST_TO_VALUE = ArrayV::new;

  final static Function<byte[], Value> BYTES_TO_VALUE = BytesV::new;

  /// Unwrap functions

  final static Function<TimeV, Instant> VALUE_TO_INSTANT = TimeV::truncated;

  final static Function<ArrayV, List<Value>> VALUE_TO_LIST = input -> input.values;

  final static Function<ObjectV, Map<String, Value>> VALUE_TO_MAP = input -> input.values;
}
