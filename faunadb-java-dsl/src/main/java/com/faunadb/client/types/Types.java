package com.faunadb.client.types;

import com.faunadb.client.errors.FaunaException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * Helper class which contains convenience methods for creating types used to decode types.
 *
 * @see Decoder
 */
public final class Types {
  private Types() {
  }

  private final static SimpleType STRING_TYPE = new SimpleType(String.class);
  private final static SimpleType OBJ_TYPE = new SimpleType(Object.class);
  private final static SimpleType[] EMPTY_BINDING = {OBJ_TYPE, OBJ_TYPE};

  private final static ConcurrentHashMap<Type, SimpleType> KNOWN_TYPES = new ConcurrentHashMap<>();

  static {
    KNOWN_TYPES.put(String.class, STRING_TYPE);

    // primitives
    KNOWN_TYPES.put(boolean.class, new SimpleType(boolean.class));
    KNOWN_TYPES.put(long.class, new SimpleType(long.class));
    KNOWN_TYPES.put(int.class, new SimpleType(int.class));
    KNOWN_TYPES.put(short.class, new SimpleType(short.class));
    KNOWN_TYPES.put(byte.class, new SimpleType(byte.class));
    KNOWN_TYPES.put(char.class, new SimpleType(char.class));
    KNOWN_TYPES.put(float.class, new SimpleType(float.class));
    KNOWN_TYPES.put(double.class, new SimpleType(double.class));
    // wrappers
    KNOWN_TYPES.put(Boolean.class, new SimpleType(Boolean.class));
    KNOWN_TYPES.put(Long.class, new SimpleType(Long.class));
    KNOWN_TYPES.put(Integer.class, new SimpleType(Integer.class));
    KNOWN_TYPES.put(Short.class, new SimpleType(Short.class));
    KNOWN_TYPES.put(Byte.class, new SimpleType(Byte.class));
    KNOWN_TYPES.put(Character.class, new SimpleType(Character.class));
    KNOWN_TYPES.put(Float.class, new SimpleType(Float.class));
    KNOWN_TYPES.put(Double.class, new SimpleType(Double.class));
  }

  /**
   * <p>Creates a generic collection type representation</p>
   *
   * <pre>{@code
   * Types.collectionOf(LinkedList.class, User.class)
   * }</pre>
   *
   * @param collectionType A concrete collection type
   * @param elementType    An element type of the collection
   * @return A type representing a collection
   */
  public static CollectionType collectionOf(Class<? extends Collection> collectionType, Type elementType) {
    return new CollectionType(collectionType, of(elementType));
  }

  /**
   * <p>Creates a collection type specialized for {@link ArrayList}</p>
   *
   * <pre>{@code
   * Types.arrayListOf(User.class)
   * Types.arrayListOf(String.class)
   * Types.arrayListOf(int.class)
   * }</pre>
   *
   * @param elementType An element type of the collection
   * @return A type representing an {@link ArrayList}
   */
  public static CollectionType arrayListOf(Type elementType) {
    return collectionOf(ArrayList.class, elementType);
  }

  /**
   * <p>Creates a collection type specialized for {@link HashSet}</p>
   *
   * <pre>{@code
   * Types.hashSetOf(User.class)
   * Types.hashSetOf(String.class)
   * Types.hashSetOf(int.class)
   * }</pre>
   *
   * @param elementType An element type of the collection
   * @return A type representing a {@link HashSet}
   */
  public static CollectionType hashSetOf(Type elementType) {
    return collectionOf(HashSet.class, elementType);
  }

  /**
   * <p>Creates a generic map type representation where the key is a string</p>
   *
   * <pre>{@code
   * Types.mapOf(Hashtable.class, User.class)
   * }</pre>
   *
   * @param mapType   A concrete map type
   * @param valueType The type of the value in the map
   * @return A type representing a map
   */
  public static MapType mapOf(Class<? extends Map> mapType, Type valueType) {
    return new MapType(mapType, of(String.class), of(valueType));
  }

  /**
   * <p>Creates a map type specialized for {@link HashMap}</p>
   *
   * <pre>{@code
   * Types.hashMapOf(User.class)
   * }</pre>
   *
   * @param valueType The type of the value in the map
   * @return A type representing a {@link HashMap}
   */
  public static MapType hashMapOf(Type valueType) {
    return mapOf(HashMap.class, valueType);
  }

  static SimpleType of(Type type) {
    SimpleType simpleType = KNOWN_TYPES.get(type);

    if (simpleType != null)
      return simpleType;

    if (type instanceof Class<?>)
      return fromClass((Class<?>) type);

    if (type instanceof ParameterizedType)
      return fromParameterizedType((ParameterizedType) type);

    if (type instanceof SimpleType)
      return (SimpleType) type;

    throw new FaunaException(format("Unknown java type: %s", type));
  }

  @SuppressWarnings("unchecked")
  private static SimpleType fromClass(Class<?> type, SimpleType... bindings) {
    if (bindings.length == 0) bindings = EMPTY_BINDING;

    if (Collection.class.isAssignableFrom(type))
      return collectionOf((Class<? extends Collection>) type, bindings[0]);

    if (Map.class.isAssignableFrom(type)) {
      SimpleType keyType = bindings[0];

      if (keyType != STRING_TYPE)
        throw new FaunaException("Only string keys are supported for maps");

      return mapOf((Class<? extends Map>) type, bindings[1]);
    }

    SimpleType simpleType = new SimpleType(type);
    KNOWN_TYPES.put(type, simpleType);
    return simpleType;
  }

  private static SimpleType fromParameterizedType(ParameterizedType type) {
    Class<?> rawType = (Class<?>) type.getRawType();
    Type[] actualTypeArguments = type.getActualTypeArguments();
    SimpleType[] bindings = new SimpleType[actualTypeArguments.length];

    for (int i = 0; i < actualTypeArguments.length; i++) {
      bindings[i] = of(actualTypeArguments[i]);
    }

    return fromClass(rawType, bindings);
  }

  static class SimpleType implements Type {
    private final Class<?> rawClass;

    SimpleType(Class<?> rawClass) {
      this.rawClass = rawClass;
    }

    Class<?> getRawClass() {
      return rawClass;
    }

    @Override
    public String toString() {
      return rawClass.getName();
    }
  }

  static class CollectionType extends SimpleType {
    private final SimpleType elementType;

    CollectionType(Class<?> collectionType, SimpleType elementType) {
      super(collectionType);
      this.elementType = elementType;
    }

    SimpleType getElementType() {
      return elementType;
    }

    @Override
    public String toString() {
      return format("%s<%s>", getRawClass().getName(), elementType);
    }
  }

  static class MapType extends SimpleType {
    private final SimpleType keyType;
    private final SimpleType valueType;

    MapType(Class<?> mapType, SimpleType keyType, SimpleType valueType) {
      super(mapType);
      this.keyType = keyType;
      this.valueType = valueType;
    }

    SimpleType getKeyType() {
      return keyType;
    }

    SimpleType getValueType() {
      return valueType;
    }

    @Override
    public String toString() {
      return format("%s<%s,%s>", getRawClass().getName(), keyType, valueType);
    }
  }
}
