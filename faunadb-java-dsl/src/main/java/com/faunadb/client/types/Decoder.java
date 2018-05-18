package com.faunadb.client.types;

import com.faunadb.client.errors.FaunaException;
import com.faunadb.client.types.Types.CollectionType;
import com.faunadb.client.types.Types.MapType;
import com.faunadb.client.types.Types.SimpleType;
import com.faunadb.client.types.Value.BytesV;
import com.faunadb.client.types.Value.NullV;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.*;

import static com.google.common.base.Defaults.defaultValue;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isAbstract;

/**
 * FaunaDB {@link Value} to object decoder.
 *
 * The {@link Decoder} is capable of decoding user defined classes as long
 * as properly annotated with: {@link FaunaField}, {@link FaunaConstructor}, {@link FaunaIgnore}, and {@link FaunaEnum}.
 *
 * @see Decoder#decode(Value, Type)
 * @see Decoder#decode(Value, Class)
 * @see FaunaField
 * @see FaunaConstructor
 * @see FaunaEnum
 * @see FaunaIgnore
 * @see Encoder
 * @see Value#to(Class)
 */
@SuppressWarnings("unchecked")
public final class Decoder {
  private Decoder() {
  }

  /**
   * Decode a FaunaDB {@link Value} to a specified type.
   *
   * <p>This method is useful if you need to decode collections or key/value maps.</p>
   *
   * <pre>{@code
   *     Result<List<String>> listStrings = Decoder.decode(result, Types.arrayListOf(String.class));
   *     Result<Set<String>> setStrings = Decoder.decode(result, Types.hashSetOf(String.class));
   * }</pre>
   *
   * @param <T>     The return type of the method.
   * @param value   The FaunaDB {@link Value} to be decoded.
   * @param dstType The {@link Type} in which value should be decoded.
   * @return A {@link Result} instance of type {@link T}
   * @see Types
   * @see Value#asCollectionOf(Class)
   * @see Value#asMapOf(Class)
   */
  public static <T> Result<T> decode(Value value, Type dstType) {
    if (value == null || value == NullV.NULL)
      return Result.fail("Value is null");

    try {
      return Result.success((T)decodeImpl(value, Types.of(dstType)));
    } catch (Exception ex) {
      return Result.fail(ex.getMessage(), ex);
    }
  }

  /**
   * Decode a FaunaDB {@link Value} to a specified type.
   *
   * <p>Use this method to decode user defined types like:</p>
   *
   * <pre>{@code
   *     Result<User> user = Decoder.decode(result, User.class);
   * }</pre>
   *
   * <p>It's possible to decode primitive types and arrays of primitive types</p>
   *
   * <pre>{@code
   *     Result<String> string = Decoder.decode(result, String.class);
   *     Result<long> longValue = Decoder.decode(result, long.class);
   *     Result<int> intValue =  Decoder.decode(result, int.class);
   *     Result<int[]> arrayInt = Decoder.decode(result, int[].class);
   * }</pre>
   *
   * @param <T>     The return type of the method.
   * @param value   The FaunaDB {@link Value} to be decoded.
   * @param dstType The {@link Class} in which value should be decoded.
   * @return A {@link Result} instance of type {@link T}
   * @see Value#to(Class)
   */
  public static <T> Result<T> decode(Value value, Class<T> dstType) {
    return decode(value, (Type)dstType);
  }

  static Object decodeImpl(Value value, SimpleType dstType) {
    Class<?> rawType = dstType.getRawClass();

    if (value == null || value == NullV.NULL)
      return defaultValue(rawType);

    if (Value.class.isAssignableFrom(rawType))
      return toValue(value, rawType);

    if (rawType.isEnum())
      return toEnum(value, rawType);

    if (rawType == byte[].class && value.getClass() == BytesV.class)
      return value.to(Codec.BYTES).get();

    if (rawType.isArray())
      return toArray(value, rawType);

    if (dstType.getClass() == CollectionType.class)
      return toCollection(value, (CollectionType) dstType);

    if (dstType.getClass() == MapType.class)
      return toMap(value, (MapType) dstType);

    return Codecs.getDecoder(rawType).decode(value).get();
  }

  private static Enum toEnum(Value value, Class<?> enumClass) {
    Map<String, Enum> enumMap = Enums.getDecodingMap((Class<Enum>) enumClass);
    return enumMap.get(value.to(Codec.STRING).get());
  }

  private static Map<String, Object> toMap(Value value, MapType dstType) {
    try {
      ImmutableMap<String, Value> values = value.to(Codec.OBJECT).get();

      Map<String, Object> map = (Map<String, Object>) getConcreteClass(dstType.getRawClass()).newInstance();

      SimpleType valueType = dstType.getValueType();

      for (Map.Entry<String, Value> entry : values.entrySet()) {
        Object result = decodeImpl(entry.getValue(), valueType);

        map.put(entry.getKey(), result);
      }

      return map;
    } catch (InstantiationException ex) {
      return couldNotInstantiateMap(dstType, ex);
    } catch (IllegalAccessException ex) {
      return couldNotInstantiateMap(dstType, ex);
    }
  }

  private static Map<String, Object> couldNotInstantiateMap(Type dstType, Exception ex) {
    throw new FaunaException(format("Could not instantiate map of type %s", dstType), ex);
  }

  private static Collection<Object> toCollection(Value value, CollectionType dstType) {
    try {
      ImmutableList<Value> values = value.to(Codec.ARRAY).get();

      SimpleType elementType = dstType.getElementType();

      Collection<Object> collection = (Collection<Object>) getConcreteClass(dstType.getRawClass()).newInstance();

      for (Value v : values) {
        Object result = decodeImpl(v, elementType);

        collection.add(result);
      }

      return collection;
    } catch (InstantiationException ex) {
      return couldNotInstantiateCollection(dstType, ex);
    } catch (IllegalAccessException ex) {
      return couldNotInstantiateCollection(dstType, ex);
    }
  }

  private static <T> T couldNotInstantiateCollection(Type dstType, Exception ex) {
    throw new FaunaException(format("Could not instantiate collection of type %s", dstType), ex);
  }

  private static Class<?> getConcreteClass(Class<?> rawClass) {
    if (!isAbstract(rawClass.getModifiers()) && !rawClass.isInterface())
      return rawClass;

    if (List.class.isAssignableFrom(rawClass))
      return ArrayList.class;

    if (Set.class.isAssignableFrom(rawClass))
      return HashSet.class;

    if (Map.class.isAssignableFrom(rawClass))
      return HashMap.class;

    throw new FaunaException(format("Abstract class not supported: $%s", rawClass));
  }

  private static Object toArray(Value value, Class<?> dstType) {
    ImmutableList<Value> values = value.to(Codec.ARRAY).get();

    SimpleType componentType = Types.of(dstType.getComponentType());

    int length = values.size();

    Object array = Array.newInstance(dstType.getComponentType(), length);

    for (int i = 0; i < length; i++) {
      Object result = decodeImpl(values.get(i), componentType);

      Array.set(array, i, result);
    }

    return array;
  }

  private static Value toValue(Value value, Class<?> dstType) {
    if (dstType.isAssignableFrom(value.getClass()))
      return value;

    throw new FaunaException(format("Cannot cast %s to %s", value.getClass(), dstType));
  }
}
