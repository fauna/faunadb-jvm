package com.faunadb.client.types;

import com.faunadb.client.errors.FaunaException;
import com.faunadb.client.types.Types.SimpleType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;

class Properties {
  public interface Property {
    String getName();

    SimpleType getType();

    void set(Object instance, Object value);

    Object get(Object instance);
  }

  static Property[] getReadProperties(Class<?> clazz) {
    return getProperties(clazz, false);
  }

  static Property[] getWriteProperties(Class<?> clazz) {
    return getProperties(clazz, true);
  }

  private static Property[] getProperties(Class<?> clazz, boolean isWrite) {
    Map<String, Field> fields = newHashMap();

    for (Field field : clazz.getDeclaredFields()) {
      if ((field.getModifiers() & Modifier.STATIC) != 0)
        continue;

      if (isWrite && (field.getModifiers() & Modifier.FINAL) != 0)
        continue;

      if (field.isAnnotationPresent(FaunaIgnore.class))
        continue;

      FaunaField faunaField = field.getAnnotation(FaunaField.class);

      if (faunaField != null) {
        if (faunaField.value().length() > 0) {
          fields.put(faunaField.value(), field);
        } else {
          fields.put(field.getName(), field);
        }
      }
    }

    Map<String, Method> props = newHashMap();

    for (Method method : clazz.getMethods()) {
      if ((method.getModifiers() & (Modifier.PUBLIC & ~Modifier.STATIC)) == 0)
        continue;

      int offset = getPrefixOffset(method, isWrite);
      if (offset < 0)
        continue;

      String propertyName = removePrefix(method.getName(), offset);

      if ("class".equals(propertyName) || method.isAnnotationPresent(FaunaIgnore.class))
        continue;

      FaunaField faunaField = method.getAnnotation(FaunaField.class);

      if (faunaField != null && faunaField.value().length() > 0)
        propertyName = faunaField.value();

      if (!fields.containsKey(propertyName)) {
        props.put(propertyName, method);
      } else if (faunaField != null) {
        fields.remove(propertyName);
        props.put(propertyName, method);
      }
    }

    List<Property> allProps = newArrayList();

    for (Map.Entry<String, Field> entry : fields.entrySet())
      allProps.add(new FieldProperty(entry.getKey(), entry.getValue()));

    for (Map.Entry<String, Method> entry : props.entrySet()) {
      Method method = entry.getValue();

      Type propertyType = isWrite ? method.getGenericParameterTypes()[0] : method.getGenericReturnType();
      allProps.add(new MethodProperty(entry.getKey(), method, propertyType));
    }

    return allProps.toArray(new Property[allProps.size()]);
  }

  private static int getPrefixOffset(Method method, boolean isWrite) {
    if (isWrite && isSetter(method)) return 3;
    if (!isWrite && isGetter(method)) return 3;
    if (!isWrite && isBooleanGetter(method)) return 2;
    return -1;
  }

  private static String removePrefix(String methodName, int offset) {
    char[] chars = methodName.toCharArray();
    chars[offset] = Character.toLowerCase(chars[offset]);
    return new String(chars, offset, chars.length - offset);
  }

  private static boolean isGetter(Method method) {
    return method.getName().startsWith("get")
      && method.getParameterTypes().length == 0;
  }

  private static boolean isBooleanGetter(Method method) {
    Class<?> returnType = method.getReturnType();
    return method.getName().startsWith("is")
      && method.getParameterTypes().length == 0
      && (returnType == boolean.class || returnType == Boolean.class);
  }

  private static boolean isSetter(Method method) {
    return method.getName().startsWith("set")
      && method.getParameterTypes().length == 1;
  }

  static class FieldProperty implements Property {
    private final String name;
    private final Field field;
    private final SimpleType type;

    FieldProperty(String name, Field field) {
      this.name = name;
      this.field = field;
      this.type = Types.of(field.getGenericType());
      this.field.setAccessible(true);
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public SimpleType getType() {
      return type;
    }

    @Override
    public void set(Object instance, Object value) {
      try {
        field.set(instance, value);
      } catch (IllegalAccessException e) {
        throw new FaunaException(format("Error while setting field %s on object %s", field, instance), e);
      }
    }

    @Override
    public Object get(Object instance) {
      try {
        return field.get(instance);
      } catch (IllegalAccessException e) {
        throw new FaunaException(format("Error while getting field %s from object %s", field, instance), e);
      }
    }
  }

  static class MethodProperty implements Property {
    private final String name;
    private final Method method;
    private final SimpleType type;

    MethodProperty(String name, Method method, Type type) {
      this.name = name;
      this.method = method;
      this.type = Types.of(type);
      this.method.setAccessible(true);
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public SimpleType getType() {
      return type;
    }

    @Override
    public void set(Object instance, Object value) {
      try {
        method.invoke(instance, value);
      } catch (Exception e) {
        throw new FaunaException(format("Error while invoking method %s on object %s", method, instance), e);
      }
    }

    @Override
    public Object get(Object instance) {
      try {
        return method.invoke(instance);
      } catch (Exception e) {
        throw new FaunaException(format("Error while invoking method %s on object %s", method, instance), e);
      }
    }
  }
}
