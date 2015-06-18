package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The base type for FaunaDB path expressions. This is uesd to reference a path in a FaunaDB expression tree.
 * Instances can be obtained through the {@code create} method on concrete subtypes, or through the helper functions.
 *
 * <p>The helper functions are intended to be imported statically:
 *
 * <p>{@code import static com.faunadb.client.java.query.Path.*; }</p>
 *
 * @see Select
 */
public abstract class Path {
  Path() {

  }

  /**
   * Helper function to construct an {@link Object} path.
   *
   * @see Object#create(String)
   */
  public static Object Object(String field) {
    return Object.create(field);
  }

  /**
   * An immutable representation of a path into a dictionary.
   */
  public static class Object extends Path {
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
   * An immutable representation of a path into an array.
   */
  public static class Array extends Path {
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
  }
}
