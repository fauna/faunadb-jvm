package com.faunadb.client.types;

import com.faunadb.client.errors.FaunaException;
import com.faunadb.client.types.Value.ObjectV;
import com.google.common.base.Function;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.faunadb.client.types.Constructors.createDecoder;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;

final class Codecs {
  private Codecs() {}

  interface EncoderEntryCache {
    Value encode(Encoder encoder, Object value);
  }

  private static final Map<Class<?>, Codec<?>> CODECS = newHashMap();
  private static final ConcurrentHashMap<Class<?>, EncoderEntryCache> ENCODERS = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<Class<?>, Function<Value, Object>> DECODERS = new ConcurrentHashMap<>();

  static {
    CODECS.put(String.class, Codec.STRING);
    CODECS.put(Boolean.class, Codec.BOOLEAN);
    CODECS.put(boolean.class, Codec.BOOLEAN);
    CODECS.put(Long.class, Codec.LONG);
    CODECS.put(long.class, Codec.LONG);
    CODECS.put(Integer.class, Codec.INTEGER);
    CODECS.put(int.class, Codec.INTEGER);
    CODECS.put(Short.class, Codec.SHORT);
    CODECS.put(short.class, Codec.SHORT);
    CODECS.put(Byte.class, Codec.BYTE);
    CODECS.put(byte.class, Codec.BYTE);
    CODECS.put(Character.class, Codec.CHAR);
    CODECS.put(char.class, Codec.CHAR);
    CODECS.put(Float.class, Codec.FLOAT);
    CODECS.put(float.class, Codec.FLOAT);
    CODECS.put(Double.class, Codec.DOUBLE);
    CODECS.put(double.class, Codec.DOUBLE);
    CODECS.put(LocalDate.class, Codec.DATE);
    CODECS.put(Instant.class, Codec.TIME);
  }

  @SuppressWarnings("unchecked")
  public static Codec<Object> getDecoder(Class<?> type) {
    Codec<?> codec = CODECS.get(type);
    if (codec != null)
      return (Codec<Object>) codec;

    Function<Value, Object> decoder = DECODERS.get(type);

    if (decoder == null) {
      decoder = createDecoder(type);

      DECODERS.put(type, decoder);
    }

    return new ObjectDecoder(decoder);
  }

  @SuppressWarnings("unchecked")
  public static Codec<Object> getEncoder(Encoder encoder, Class<?> type) {
    Codec<?> codec = CODECS.get(type);
    if (codec != null)
      return (Codec<Object>) codec;

    EncoderEntryCache encoderEntryCache = ENCODERS.get(type);

    if (encoderEntryCache == null) {
      encoderEntryCache = createEncoder(type);

      ENCODERS.put(type, encoderEntryCache);
    }

    return new ObjectEncoder(encoder, encoderEntryCache);
  }

  private static EncoderEntryCache createEncoder(Class<?> type) {
    final Properties.Property[] readProperties = Properties.getReadProperties(type);

    return new EncoderEntryCache() {
      @Override
      public Value encode(Encoder encoder, Object value) {
        Map<String, Value> fields = newHashMap();

        for (Properties.Property property : readProperties) {
          try {
            fields.put(property.getName(), encoder.encodeImpl(property.get(value)));
          } catch (Exception ex) {
            throw new FaunaException(format("Could not encode field \"%s\". Reason: %s", property.getName(), ex.getMessage()));
          }
        }

        return new ObjectV(fields);
      }
    };
  }

  private static class ObjectDecoder implements Codec<Object> {
    private final Function<Value, Object> decoder;

    ObjectDecoder(Function<Value, Object> decoder) {
      this.decoder = decoder;
    }

    @Override
    public Result<Object> decode(Value value) {
      return Result.success(decoder.apply(value));
    }

    @Override
    public Result<Value> encode(Object value) {
      throw new FaunaException("Operation not permitted: calling encode() in a decoding only codec.");
    }
  }

  private static class ObjectEncoder implements Codec<Object> {
    private final Encoder encoder;
    private final EncoderEntryCache encoderEntryCache;

    ObjectEncoder(Encoder encoder, EncoderEntryCache encoderEntryCache) {
      this.encoder = encoder;
      this.encoderEntryCache = encoderEntryCache;
    }

    @Override
    public Result<Object> decode(Value value) {
      throw new FaunaException("Operation not permitted: calling decode() in an encoding only codec.");
    }

    @Override
    public Result<Value> encode(Object value) {
      return Result.success(encoderEntryCache.encode(encoder, value));
    }
  }
}
