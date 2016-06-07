package com.faunadb.client.types;

import com.faunadb.client.types.Value.*;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.LocalDate;

import static java.lang.String.format;

public interface Codec<T> extends Function<Value, Result<T>> {

  Codec<Value> VALUE = new Codec<Value>() {
    @Override
    public Result<Value> apply(Value value) {
      if (value == NullV.NULL)
        return Result.fail("Value is null");

      return Result.success(value);
    }
  };

  Codec<Ref> REF = Cast.to(Ref.class, Functions.<Ref>identity());
  Codec<SetRef> SET_REF = Cast.to(SetRef.class, Functions.<SetRef>identity());
  Codec<Long> LONG = Cast.to(LongV.class, Cast.<LongV, Long>scalarValue());
  Codec<Instant> TS = Cast.to(TsV.class, Cast.<TsV, Instant>scalarValue());
  Codec<String> STRING = Cast.to(StringV.class, Cast.<StringV, String>scalarValue());
  Codec<Double> DOUBLE = Cast.to(DoubleV.class, Cast.<DoubleV, Double>scalarValue());
  Codec<Boolean> BOOLEAN = Cast.to(BooleanV.class, Cast.<BooleanV, Boolean>scalarValue());
  Codec<LocalDate> DATE = Cast.to(DateV.class, Cast.<DateV, LocalDate>scalarValue());

  Codec<ImmutableList<Value>> ARRAY = Cast.to(ArrayV.class, new Function<ArrayV, ImmutableList<Value>>() {
    @Override
    public ImmutableList<Value> apply(ArrayV input) {
      return input.values;
    }
  });

  Codec<ImmutableMap<String, Value>> OBJECT = Cast.to(ObjectV.class, new Function<ObjectV, ImmutableMap<String, Value>>() {
    @Override
    public ImmutableMap<String, Value> apply(ObjectV input) {
      return input.values;
    }
  });
}

final class Cast {

  static <V extends Value, O> Codec<O> to(final Class<V> clazz, final Function<V, O> fn) {
    return new Codec<O>() {
      @Override
      public Result<O> apply(Value input) {
        return cast(clazz, input).map(fn);
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