package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.time.LocalDate;

import static java.lang.String.format;

/**
 * A {@link Value} that wraps a JSON response tree. This Value does not convert to a concrete type until one of its
 * type coercion methods is called.
 */
@JsonDeserialize(using = Codec.LazyValueDeserializer.class)
public final class LazyValue extends Value {

  private final JsonNode underlying;
  private final ObjectMapper json;

  private final Value rawJson = new Value() {
    @Override
    public String toString() {
      return underlying.toString();
    }
  };
  private Value lazy = rawJson;

  @JsonCreator
  LazyValue(JsonNode underlying, ObjectMapper json) {
    this.underlying = underlying;
    this.json = json;
  }

  @JsonValue
  private JsonNode underlying() {
    return underlying;
  }

  @Override
  public String asString() {
    return as(StringV.class).asString();
  }

  @Override
  public Optional<String> asStringOption() {
    return as(StringV.class).asStringOption();
  }

  @Override
  public Boolean asBoolean() {
    return as(BooleanV.class).asBoolean();
  }

  @Override
  public Optional<Boolean> asBooleanOption() {
    return as(BooleanV.class).asBooleanOption();
  }

  @Override
  public Long asLong() {
    return as(LongV.class).asLong();
  }

  @Override
  public Optional<Long> asLongOption() {
    return as(LongV.class).asLongOption();
  }

  @Override
  public Double asDouble() {
    return as(DoubleV.class).asDouble();
  }

  @Override
  public Optional<Double> asDoubleOption() {
    return as(DoubleV.class).asDoubleOption();
  }

  @Override
  public Instant asTs() {
    return as(TsV.class).asTs();
  }

  @Override
  public Optional<Instant> asTsOption() {
    return as(TsV.class).asTsOption();
  }

  @Override
  public LocalDate asDate() {
    return as(DateV.class).asDate();
  }

  @Override
  public Optional<LocalDate> asDateOption() {
    return as(DateV.class).asDateOption();
  }

  @Override
  public ImmutableList<Value> asArray() {
    return as(ArrayV.class).asArray();
  }

  @Override
  public Optional<ImmutableList<Value>> asArrayOption() {
    return as(ArrayV.class).asArrayOption();
  }

  @Override
  public ImmutableMap<String, Value> asObject() {
    return as(ObjectV.class).asObject();
  }

  @Override
  public Optional<ImmutableMap<String, Value>> asObjectOption() {
    return as(ObjectV.class).asObjectOption();
  }

  @Override
  public Ref asRef() {
    return as(Ref.class).asRef();
  }

  @Override
  public Optional<Ref> asRefOption() {
    return as(Ref.class).asRefOption();
  }

  @Override
  public SetRef asSetRef() {
    return as(SetRef.class).asSetRef();
  }

  @Override
  public Optional<SetRef> asSetRefOption() {
    return as(SetRef.class).asSetRefOption();
  }

  @Override
  public Value get(String key) {
    return asObject().get(key);
  }

  @Override
  public Value get(String... keys) {
    Value res = this;
    for (String key : keys)
      res = res.get(key);

    return res;
  }

  @Override
  public Optional<Value> getOption(String key) {
    Optional<ImmutableMap<String, Value>> object = asObjectOption();
    if (object.isPresent())
      return Optional.fromNullable(object.get().get(key));

    return Optional.absent();
  }

  @Override
  public Optional<Value> getOption(String... keys) {
    Optional<Value> res = Optional.<Value>of(this);
    for (String key : keys) {
      res = res.get().getOption(key);
      if (!res.isPresent()) break;
    }

    return res;
  }

  @Override
  public Value get(int index) {
    return asArray().get(index);
  }

  @Override
  public Optional<Value> getOption(int index) {
    Optional<ImmutableList<Value>> array = asArrayOption();

    if (array.isPresent()) {
      try {
        return Optional.of(array.get().get(index));
      } catch (IndexOutOfBoundsException ign) {
        return Optional.absent();
      }
    }

    return Optional.absent();
  }

  private Value as(Class<? extends Value> clazz) {
    if (lazy != rawJson)
      return lazy;

    try {
      lazy = json.convertValue(underlying, clazz);
    } catch (IllegalArgumentException ex) {
      //Failed to convert. Try again with other type.
    }

    return lazy;
  }

  @Override
  public boolean equals(Object other) {
    return other != null &&
      other instanceof LazyValue
      && underlying.equals(((LazyValue) other).underlying);
  }

  @Override
  public int hashCode() {
    return underlying.hashCode();
  }

  @Override
  public String toString() {
    return format("LazyValue(%s)", underlying.toString());
  }

}
