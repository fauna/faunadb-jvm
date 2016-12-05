package com.faunadb.client.util;

import java.util.Arrays;

/**
 * Methods from {@link java.util.Objects} that don't exist in old versions of Android
 */
final public class Objects {
  private Objects() {
  }

  public static <T> T requireNonNull(T obj) {
    if (obj == null)
      throw new NullPointerException();
    return obj;
  }

  public static int hash(Object... values) {
    return Arrays.hashCode(values);
  }

  public static int compare(long x, long y) {
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
  }

  public static int compare(int x, int y) {
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
  }
}
