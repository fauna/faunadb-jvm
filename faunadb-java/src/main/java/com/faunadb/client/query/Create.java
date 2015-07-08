package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.Identifier;
import com.faunadb.client.types.Value.*;

/**
 * An immutable representation of a Create function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-modifying-resources">FaunaDB Resource Modification Functions</a>
 *
 * @see Language#Create(Identifier)
 */
public final class Create implements Identifier, Expression {
  @JsonProperty("create")
  private final Identifier ref;
  @JsonProperty("params")
  private final Expression params;

  /**
   * Obtains a new instance of this class with no parameters.
   * @param ref the ref of the class in which to create a new instance.
   * @see Language#Create(Identifier)
   */
  public static Create create(Identifier ref) {
    return new Create(ref, ObjectV.empty());
  }

  /**
   * Obtains a new instance of this class.
   * @param ref the ref of the class in which to create a new instance.
   * @param params The parameters for the create operation.
   * @see Language#Create(Identifier, Expression)
   */
  public static Create create(Identifier ref, Expression params) {
    return new Create(ref, params);
  }

  Create(Identifier ref, Expression params) {
    this.ref = ref;
    this.params = params;
  }

  public Identifier ref() {
    return ref;
  }

  public Expression params() {
    return params;
  }
}
