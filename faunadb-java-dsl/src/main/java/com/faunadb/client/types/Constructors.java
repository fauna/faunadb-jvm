package com.faunadb.client.types;

import com.faunadb.client.errors.FaunaException;
import com.faunadb.client.types.Properties.Property;
import com.faunadb.client.types.Types.SimpleType;
import com.faunadb.client.types.Value.ObjectV;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static com.faunadb.client.types.Decoder.decodeImpl;
import static java.lang.String.format;

final class Constructors {
  private Constructors() {}

  static Function<Value, Object> createDecoder(Class<?> clazz) {
    Function<Value, Object> decoder = getStaticFactoryMethodDecoder(clazz);

    if (decoder == null)
      decoder = getAnnotatedConstructorDecoder(clazz);

    if (decoder == null)
      decoder = getDefaultConstructorDecoder(clazz);

    if (decoder != null)
      return decoder;

    throw new FaunaException(
      format("No suitable constructor or factory method found for type %s. Ensure that a factory method or constructor is annotated with @%s",
        clazz.getName(), FaunaConstructor.class.getSimpleName()));
  }

  private static Function<Value, Object> getStaticFactoryMethodDecoder(Class<?> clazz) {
    for (Method method : clazz.getDeclaredMethods()) {
      if (!method.isAnnotationPresent(FaunaConstructor.class))
        continue;

      if ((method.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) == 0)
        continue;

      if (method.getParameterTypes().length == 0)
        return new NoArgsStaticFactoryMethodDecoder(method);

      return new StaticFactoryMethodDecoder(method);
    }

    return null;
  }

  private static Function<Value, Object> getAnnotatedConstructorDecoder(Class<?> clazz) {
    for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
      if (constructor.isAnnotationPresent(FaunaConstructor.class)) {
        if (constructor.getParameterTypes().length == 0)
          return new DefaultConstructorDecoder(constructor);

        return new ConstructorDecoder(constructor);
      }
    }

    return null;
  }

  private static Function<Value, Object> getDefaultConstructorDecoder(Class<?> clazz) {
    for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
      if (constructor.getParameterTypes().length == 0) {
        return new DefaultConstructorDecoder(constructor);
      }
    }
    return null;
  }

  static private abstract class AbstractConstructorDecoder implements Function<Value, Object> {
    final private Class<?> rawClass;
    final private String[] parameterNames;
    final private SimpleType[] parameterTypes;
    final private Property[] writeProperties;

    AbstractConstructorDecoder(Constructor<?> constructor) {
      this.rawClass = constructor.getDeclaringClass();
      this.parameterNames = getParameterNames(constructor.getParameterAnnotations());
      this.parameterTypes = getParameterTypes(constructor.getGenericParameterTypes());
      this.writeProperties = filterProperties(Properties.getWriteProperties(constructor.getDeclaringClass()), this.parameterNames);
    }

    AbstractConstructorDecoder(Method method) {
      this.rawClass = method.getDeclaringClass();
      this.parameterNames = getParameterNames(method.getParameterAnnotations());
      this.parameterTypes = getParameterTypes(method.getGenericParameterTypes());
      this.writeProperties = filterProperties(Properties.getWriteProperties(method.getDeclaringClass()), this.parameterNames);
    }

    private SimpleType[] getParameterTypes(Type[] genericParameterTypes) {
      SimpleType[] parameterTypes = new SimpleType[genericParameterTypes.length];

      for (int i = 0; i < genericParameterTypes.length; i++) {
        parameterTypes[i] = Types.of(genericParameterTypes[i]);
      }

      return parameterTypes;
    }

    private static Property[] filterProperties(Property[] properties, String[] names) {
      if (names.length == 0)
        return properties;

      final Set<String> namesToRemove = new HashSet<>(Arrays.asList(names));

      return Arrays.stream(properties)
          .filter(input -> !namesToRemove.contains(input.getName()))
          .toArray(Property[]::new);
    }

    private String[] getParameterNames(Annotation[][] parameterAnnotations) {
      String[] parameterNames = new String[parameterAnnotations.length];

      for (int i = 0; i < parameterAnnotations.length; i++) {
        Annotation[] annotations = parameterAnnotations[i];

        for (Annotation annotation : annotations) {
          if (annotation.annotationType() == FaunaField.class) {
            parameterNames[i] = ((FaunaField) annotation).value();
            break;
          }
        }

        if (parameterNames[i] == null)
          throw new FaunaException(format("All constructor or factory method arguments must be annotated with @%s", FaunaField.class.getSimpleName()));
      }

      return parameterNames;
    }

    @Override
    public Object apply(Value value) {
      try {
        ObjectV object = (ObjectV) value;
        Object instance = newInstance(buildArguments(object));

        for (Property property : writeProperties) {
          Object result = decodeImpl(object.values.get(property.getName()), property.getType());

          property.set(instance, result);
        }

        return instance;
      } catch (Exception ex) {
        throw new FaunaException(format("Could not instantiate object of class %s", rawClass.getName()), ex);
      }
    }

    protected Object[] buildArguments(ObjectV value) {
      Object[] arguments = new Object[parameterTypes.length];

      for (int i = 0; i < parameterTypes.length; i++) {
        arguments[i] = decodeImpl(value.values.get(parameterNames[i]), parameterTypes[i]);
      }

      return arguments;
    }

    protected abstract Object newInstance(Object[] arguments);
  }

  static private class ConstructorDecoder extends AbstractConstructorDecoder {
    private final Constructor<?> constructor;

    private ConstructorDecoder(Constructor<?> constructor) {
      super(constructor);
      this.constructor = constructor;
      this.constructor.setAccessible(true);
    }

    @Override
    protected Object newInstance(Object[] arguments) {
      try {
        return constructor.newInstance(arguments);
      } catch (Exception ex) {
        throw new FaunaException(format("Error while invoking constructor %s", constructor), ex);
      }
    }
  }

  static private class StaticFactoryMethodDecoder extends AbstractConstructorDecoder {
    private final Method method;

    private StaticFactoryMethodDecoder(Method method) {
      super(method);
      this.method = method;
      this.method.setAccessible(true);
    }

    @Override
    protected Object newInstance(Object[] arguments) {
      try {
        return method.invoke(null, arguments);
      } catch (Exception ex) {
        throw new FaunaException(format("Error while invoking static method %s", method), ex);
      }
    }
  }

  static private Object[] EMPTY_ARGUMENTS = new Object[0];

  static private class DefaultConstructorDecoder extends ConstructorDecoder {
    private DefaultConstructorDecoder(Constructor<?> constructor) {
      super(constructor);
    }

    @Override
    protected Object[] buildArguments(ObjectV value) {
      return EMPTY_ARGUMENTS;
    }
  }

  static private class NoArgsStaticFactoryMethodDecoder extends StaticFactoryMethodDecoder {
    private NoArgsStaticFactoryMethodDecoder(Method method) {
      super(method);
    }

    @Override
    protected Object[] buildArguments(ObjectV value) {
      return EMPTY_ARGUMENTS;
    }
  }
}
