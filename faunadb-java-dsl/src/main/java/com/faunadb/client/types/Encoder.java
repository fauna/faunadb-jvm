package com.faunadb.client.types;

import com.faunadb.client.errors.FaunaException;
import com.faunadb.client.types.Value.ArrayV;
import com.faunadb.client.types.Value.ObjectV;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import static java.lang.String.format;

/**
 * FaunaDB object to {@link Value} encoder.
 *
 * The {@link Encoder} is capable of encoding user defined classes as long
 * as properly annotated with: {@link FaunaField}, {@link FaunaConstructor}, {@link FaunaIgnore}, and {@link FaunaEnum}.
 *
 * @see Encoder#encode(Object)
 * @see FaunaField
 * @see FaunaConstructor
 * @see FaunaEnum
 * @see FaunaIgnore
 * @see Decoder
 * @see com.faunadb.client.query.Language#Value(Object)
 */
@SuppressWarnings("unchecked")
public final class Encoder {
  private Encoder() {
  }

  private final Stack<Object> stack = new Stack<>();

  /**
   * Encode the specified object into a {@link Value} instance.
   *
   * @param obj Any instance of user defined classes, primitive values or any
   *            generic collection like {@link java.util.List}, {@link java.util.Set} or {@link java.util.Map}
   * @return A FaunaDB {@link Value} corresponding to the given argument
   * @see com.faunadb.client.query.Language#Value(Object)
   */
  public static Result<Value> encode(Object obj) {
    try {
      return Result.success(new Encoder().encodeImpl(obj));
    } catch (Exception ex) {
      return Result.fail(ex.getMessage(), ex);
    }
  }

  Value encodeImpl(Object obj) {
    if (obj == null)
      return Value.NullV.NULL;

    if (obj instanceof Value)
      return (Value) obj;

    if (contains(obj))
      throw new FaunaException(format("Self reference loop detected for object \"%s\"", obj));

    try {
      stack.push(obj);

      return encodeIntern(obj);
    } finally {
      stack.pop();
    }
  }

  private boolean contains(Object value) {
    for (Object obj : stack) {
      if (obj == value)
        return true;
    }

    return false;
  }

  private Value encodeIntern(Object obj) {
    Class<?> clazz = obj.getClass();

    if (clazz.isEnum())
      return Enums.getEncodingMap((Class<Enum>) clazz).get(obj);

    if (clazz == byte[].class)
      return Codec.BYTES.encode((byte[]) obj).get();

    if (clazz.isArray())
      return wrapArray(obj);

    if (Iterable.class.isAssignableFrom(clazz))
      return wrapIterable((Iterable<?>) obj);

    if (Map.class.isAssignableFrom(clazz))
      return wrapMap((Map<?, ?>) obj);

    return Codecs.getEncoder(this, clazz).encode(obj).get();
  }

  private Value wrapMap(Map<?, ?> obj) {
    Map<String, Value> values = new HashMap<>();

    for (Entry<?, ?> entry : obj.entrySet()) {
      values.put(entry.getKey().toString(), encodeImpl(entry.getValue()));
    }

    return new ObjectV(values);
  }

  private Value wrapIterable(Iterable<?> obj) {
    Iterator<?> iterator = obj.iterator();

    List<Value> values = new ArrayList<>();
    while (iterator.hasNext()) {
      values.add(encodeImpl(iterator.next()));
    }

    return new ArrayV(values);
  }

  private Value wrapArray(Object obj) {
    int length = Array.getLength(obj);
    List<Value> values = new ArrayList<>(length);

    for (int i = 0; i < length; i++) {
      values.add(encodeImpl(Array.get(obj, i)));
    }

    return new ArrayV(values);
  }

}
