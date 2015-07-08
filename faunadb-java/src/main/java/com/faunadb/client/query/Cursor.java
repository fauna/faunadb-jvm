package com.faunadb.client.query;

import com.faunadb.client.types.Value;

/**
 * The base type for FaunaDB cursors. Instances of cursor classes can be obtained through the {@code create} method on
 * the concrete types, or through the helper functions in {@link Language}
 */
public abstract class Cursor {
  /**
   * An immutable representation of a Before cursor.
   * @see Language#Before(Value)
   */
  public static final class Before extends Cursor {
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
   * @see Language#After(Value)
   */
  public static final class After extends Cursor {
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

