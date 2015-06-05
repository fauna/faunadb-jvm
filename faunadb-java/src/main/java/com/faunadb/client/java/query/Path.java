package com.faunadb.client.java.query;

public abstract class Path {
  public static class ObjectPath extends Path {
    public static ObjectPath create(String field) {
      return new ObjectPath(field);
    }

    private final String field;

    ObjectPath(String field) {
      this.field = field;
    }
  }

  public static class ArrayPath extends Path {
    public static ArrayPath create(int index) {
      return new ArrayPath(index);
    }

    private final int field;

    ArrayPath(int field) {
      this.field = field;
    }
  }
}
