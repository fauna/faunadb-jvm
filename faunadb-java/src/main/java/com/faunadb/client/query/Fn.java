package com.faunadb.client.query;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

final class Fn {

  private static Expr apply(Map<String, Expr> args) {
    return new Expr.ConcreteExpr(Expr.wrapValues(args));
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
