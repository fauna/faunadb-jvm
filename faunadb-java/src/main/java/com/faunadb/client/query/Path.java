package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonValue;
import com.faunadb.client.types.Value;
import com.google.common.collect.ImmutableList;

/**
 * The base type for FaunaDB path expressions. This is uesd to reference a path in a FaunaDB expression tree.
 * Instances can be obtained through the {@code create} method on concrete subtypes, or through the helper functions.
 *
 * <p>The helper functions are intended to be imported statically:
 *
 * <p>{@code import static com.faunadb.client.java.query.Path.*; }</p>
 *
 * @see Language#Path(Path)
 * @see Language#Select(ImmutableList, Value)
 * @see Language#Contains(ImmutableList, Value)
 */
public abstract class Path {
  Path() { }

  public abstract Value value();

  /**
   * Helper function to construct an {@link Object} path.
   *
   * @see Object#create(String)
   */
  public static Object Object(String field) {
    return Object.create(field);
  }

  /**
   * A path into a dictionary. Wraps a field name.
   */
  public static final class Object extends Path {
    public static Object create(String field) {
      return new Object(field);
    }

    private final String field;

    Object(String field) {
      this.field = field;
    }

    @JsonValue
    public String field() {
      return field;
    }

    public Value value() {
      return Value.StringV.create(field);
    }
  }

  /**
   * Helper function to construct an {@link Array} path.
   *
   * @see Array#create(int)
   */
  public static Array Array(int field) {
    return Array.create(field);
  }

  /**
   * A path into an array. Wraps an array index.
   */
  public static final class Array extends Path {
    public static Array create(int index) {
      return new Array(index);
    }

    private final int field;

    Array(int field) {
      this.field = field;
    }

    @JsonValue
    public int field() {
      return field;
    }

    public Value value() {
      return Value.LongV.create(field);
    }
  }
}
