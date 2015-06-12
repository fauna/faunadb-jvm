package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonValue;

public abstract class Path {
  public static Object Object(String field) {
    return Object.create(field);
  }

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

  public static Array Array(int field) {
    return Array.create(field);
  }

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
