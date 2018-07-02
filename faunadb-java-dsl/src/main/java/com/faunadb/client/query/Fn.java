package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class Fn {

  private static abstract class Unescaped<T> extends Expr {
    final T body;

    private Unescaped(T body) {
      this.body = body;
    }

    @Override
    @JsonValue
    protected T toJson() {
      return body;
    }
  }

  private static final class UnescapedObject extends Unescaped<Map<String, Expr>> {
    private UnescapedObject(Map<String, ? extends Expr> body) {
      super(ImmutableMap.copyOf(body));
    }
  }

  private static final class UnescapedArray extends Unescaped<List<Expr>> {
    private UnescapedArray(List<? extends Expr> body) {
      super(Collections.unmodifiableList(body));
    }
  }

  static Expr apply(List<? extends Expr> args) {
    return new UnescapedArray(args);
  }

  static Expr apply(Map<String, ? extends Expr> args) {
    return new UnescapedObject(args);
  }

  static Expr apply(String k1, Expr p1) {
    return apply(ImmutableMap.of(k1, p1));
  }

  static Expr apply(String k1, Expr p1, String k2, Expr p2) {
    return apply(ImmutableMap.of(k1, p1, k2, p2));
  }

  static Expr apply(String k1, Expr p1, String k2, Expr p2, String k3, Expr p3) {
    return apply(ImmutableMap.of(k1, p1, k2, p2, k3, p3));
  }

  static Expr apply(String k1, Expr p1, String k2, Expr p2, String k3, Expr p3, String k4, Expr p4) {
    return apply(ImmutableMap.of(k1, p1, k2, p2, k3, p3, k4, p4));
  }

}
