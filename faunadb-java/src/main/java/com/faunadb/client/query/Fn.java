package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonValue;
import com.faunadb.client.types.Value;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static java.lang.String.format;

class Fn extends Expr.Literal {

  static class Call extends Value.ConcreteValue {

    private final ObjectV body;

    Call(Map<String, ? extends Value> body) {
      this.body = new ObjectV(body);
    }

    @JsonValue
    ObjectV body() {
      return body;
    }

    @Override
    public boolean equals(Object other) {
      return other != null &&
        other instanceof Fn.Call &&
        this.body.equals(((Call) other).body);
    }

    @Override
    public int hashCode() {
      return body.hashCode();
    }

    @Override
    public String toString() {
      return format("Fn.Call(%s)", body);
    }

  }

  private static Expr apply(Map<String, ? extends Value> args) {
    return new Fn(new Call(args));
  }

  static Expr apply(String k1, Value p1) {
    return apply(ImmutableMap.of(k1, p1));
  }

  static Expr apply(String k1, Value p1, String k2, Value p2) {
    return apply(ImmutableMap.of(k1, p1, k2, p2));
  }

  static Expr apply(String k1, Value p1, String k2, Value p2, String k3, Value p3) {
    return apply(ImmutableMap.of(k1, p1, k2, p2, k3, p3));
  }

  static Expr apply(String k1, Value p1, String k2, Value p2, String k3, Value p3, String k4, Value p4) {
    return apply(ImmutableMap.of(k1, p1, k2, p2, k3, p3, k4, p4));
  }

  Fn(Call call) {
    super(call);
  }

}
