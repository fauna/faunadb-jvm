package com.faunadb.client.java.query;

public class If implements Expression {
  public static If create(Expression condition, Expression then, Expression elseExpression) {
    return new If(condition, then, elseExpression);
  }

  private final Expression condition;
  private final Expression then;
  private final Expression elseExpression;

  If(Expression condition, Expression then, Expression elseExpression) {
    this.condition = condition;
    this.then = then;
    this.elseExpression = elseExpression;
  }
}
