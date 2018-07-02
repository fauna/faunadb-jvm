package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.LinkedHashMap;
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
      super(Collections.unmodifiableMap(body));
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
    Map<String, Expr> kvs = new LinkedHashMap<>();
    kvs.put(k1, p1);
    return apply(kvs);
  }

  static Expr apply(String k1, Expr p1, String k2, Expr p2) {
    Map<String, Expr> kvs = new LinkedHashMap<>();
    kvs.put(k1, p1);
    kvs.put(k2, p2);
    return apply(kvs);
  }

  static Expr apply(String k1, Expr p1, String k2, Expr p2, String k3, Expr p3) {
    Map<String, Expr> kvs = new LinkedHashMap<>();
    kvs.put(k1, p1);
    kvs.put(k2, p2);
    kvs.put(k3, p3);
    return apply(kvs);
  }

  static Expr apply(String k1, Expr p1, String k2, Expr p2, String k3, Expr p3, String k4, Expr p4) {
    Map<String, Expr> kvs = new LinkedHashMap<>();
    kvs.put(k1, p1);
    kvs.put(k2, p2);
    kvs.put(k3, p3);
    kvs.put(k4, p4);
    return apply(kvs);
  }

}
