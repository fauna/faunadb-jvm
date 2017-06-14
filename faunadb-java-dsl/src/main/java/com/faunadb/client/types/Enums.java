package com.faunadb.client.types;

import com.faunadb.client.types.Value.StringV;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.collect.Maps.newEnumMap;
import static com.google.common.collect.Maps.newHashMap;

final class Enums {
  private Enums() {}

  private final static ConcurrentHashMap<Class<?>, Map<Enum, StringV>> ENCODING_MAP = new ConcurrentHashMap<>();
  private final static ConcurrentHashMap<Class<?>, Map<String, Enum>> DECODING_MAP = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public static Map<Enum, StringV> getEncodingMap(Class<Enum> enumClass) {
    Map<Enum, StringV> enumMap = ENCODING_MAP.get(enumClass);

    if (enumMap == null) {
      enumMap = newEnumMap(enumClass);

      for (Enum en : enumClass.getEnumConstants()) {
        try {
          java.lang.reflect.Field field = enumClass.getField(en.name());
          FaunaEnum faunaEnum = field.getAnnotation(FaunaEnum.class);
          String enumName = faunaEnum != null ? faunaEnum.value() : en.name();
          enumMap.put(en, new StringV(enumName));
        } catch (NoSuchFieldException ex) {
          //will never happen since we are iterating over the enum constants,
          //it just exist because NoSuchFieldException is a checked exception
        }
      }

      ENCODING_MAP.put(enumClass, enumMap);
    }

    return enumMap;
  }

  public static Map<String, Enum> getDecodingMap(Class<Enum> enumClass) {
    Map<String, Enum> enumMap = DECODING_MAP.get(enumClass);

    if (enumMap == null) {
      enumMap = newHashMap();

      for (Map.Entry<Enum, StringV> entry : getEncodingMap(enumClass).entrySet()) {
        enumMap.put(entry.getValue().value, entry.getKey());
      }

      DECODING_MAP.put(enumClass, enumMap);
    }

    return enumMap;
  }
}
