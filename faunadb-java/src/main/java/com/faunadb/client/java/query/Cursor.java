package com.faunadb.client.java.query;

import com.faunadb.client.java.types.Value;

/**
 * The base type for FaunaDB cursors. Instances of cursor classes can be obtained through the {@code create} method on
 * the concrete types.
 */
public abstract class Cursor {
  /**
   * An immutable representation of a Before cursor.
   */
  public static class Before extends Cursor {
    /**
     * Obtains a new instance of this class.
     */
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

  /**
   * An immutable representation of an After cursor.
   */
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

