package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An immutable representation of a FaunaDB variable identifier. Create an instance using the {@link Var#create} method.
 *
 * <p><i>Example</i>: {@code { "var": "variable_name" }}.</p>
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a>
 */
public class Var extends Value {
  /**
   * Creates a new {@link Var} instance.
   * @param variable the name of the variable.
   */
  public static Var create(String variable) {
    return new Var(variable);
  }

  @JsonProperty("var")
  private final String variable;

  Var(String variable) {
    this.variable = variable;
  }

  /**
   * Returns the variable name.
   */
  public String variable() {
    return variable;
  }
}
