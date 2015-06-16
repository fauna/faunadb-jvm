package com.faunadb.client.java.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Var extends Value implements Identifier {
  public static Var create(String variable) {
    return new Var(variable);
  }

  @JsonProperty("var")
  private final String variable;

  Var(String variable) {
    this.variable = variable;
  }

  public String variable() {
    return variable;
  }
}
