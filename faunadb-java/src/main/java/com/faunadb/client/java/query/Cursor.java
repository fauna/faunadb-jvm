package com.faunadb.client.java.query;

import com.faunadb.client.java.types.Value;

public abstract class Cursor {
  public static class Before extends Cursor {
    public static Before create(Value value) {
      return new Before(value);
    }

    private final Value value;
    Before(Value value) {
      this.value = value;
    }

    public Value value() {
      return value;
    }
  }

  public static class After extends Cursor {
    public static After create(Value value) {
      return new After(value);
    }

    private final Value value;
    After(Value value) {
      this.value = value;
    }

    public Value value() {
      return value;
    }
  }
}

