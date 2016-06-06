package com.faunadb.client.types;

import com.faunadb.client.types.Value.*;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.LocalDate;

import static java.lang.String.format;

public interface Codec<T> extends Function<Value, Result<T>> {

  Codec<Value> IDENTITY = new Codec<Value>() {
    @Override
    public Result<Value> apply(Value input) {
      return Result.success(input);
    }
  };

  Codec<Ref> REF = Cast.to(Ref.class, Cast.<Ref>identity());
  Codec<SetRef> SET_REF = Cast.to(SetRef.class, Cast.<SetRef>identity());
  Codec<Long> LONG = Cast.to(LongV.class, Cast.<LongV, Long>scalarValue());
  Codec<Instant> TS = Cast.to(TsV.class, Cast.<TsV, Instant>scalarValue());
  Codec<String> STRING = Cast.to(StringV.class, Cast.<StringV, String>scalarValue());
  Codec<Double> DOUBLE = Cast.to(DoubleV.class, Cast.<DoubleV, Double>scalarValue());
  Codec<Boolean> BOOLEAN = Cast.to(BooleanV.class, Cast.<BooleanV, Boolean>scalarValue());
  Codec<LocalDate> DATE = Cast.to(DateV.class, Cast.<DateV, LocalDate>scalarValue());

  Codec<ImmutableList<Value>> ARRAY = Cast.to(ArrayV.class, new Function<ArrayV, Result<ImmutableList<Value>>>() {
    @Override
    public Result<ImmutableList<Value>> apply(ArrayV input) {
      return Result.success(input.values);
    }
  });

  Codec<ImmutableMap<String, Value>> OBJECT = Cast.to(ObjectV.class, new Function<ObjectV, Result<ImmutableMap<String, Value>>>() {
    @Override
    public Result<ImmutableMap<String, Value>> apply(ObjectV input) {
      return Result.success(input.values);
    }
  });
}

final class Cast {

  static <V extends Value, O> Codec<O> to(final Class<V> clazz, final Function<V, Result<O>> fn) {
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

  static <T extends ScalarValue<R>, R> Function<T, Result<R>> scalarValue() {
    return new Function<T, Result<R>>() {
      @Override
      public Result<R> apply(T input) {
        return Result.success(input.value);
      }
    };
  }

  static <T> Function<T, Result<T>> identity() {
    return new Function<T, Result<T>>() {
      @Override
      public Result<T> apply(T input) {
        return Result.success(input);
      }
    };
  }
}