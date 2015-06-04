package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Ref;

public abstract class Cursor {
  public static class Before extends Cursor {
    public static Before create(Ref ref) {
      return new Before(ref);
    }

    private final Ref ref;
    Before(Ref ref) {
      this.ref = ref;
    }

    public Ref ref() {
      return ref;
    }
  }

  public static class After extends Cursor {
    public static After create(Ref ref) {
      return new After(ref);
    }

    private final Ref ref;
    After(Ref ref) {
      this.ref = ref;
    }

    public Ref ref() {
      return ref;
    }
  }
}

