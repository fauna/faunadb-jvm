package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Identifier;
import com.faunadb.client.java.types.Value.*;

/**
 * An immutable value-type representation of a Create function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-modifying-resources">FaunaDB Resource Modification Functions</a>
 */
public class Create implements Identifier, Expression {
  @JsonProperty("create")
  private final Identifier ref;
  @JsonProperty("params")
  private final Expression params;

  /**
   * Obtains a new instance of this class with no parameters.
   * @param ref the ref of the class in which to create a new instance.
   */
  public static Create create(Identifier ref) {
    return new Create(ref, ObjectV.empty());
  }

  /**
   * Obtains a new instance of this class.
   * @param ref the ref of the class in which to create a new instance.
   * @param params The parameters for the create operation.
   */
  public static Create create(Identifier ref, Expression params) {
    return new Create(ref, params);
  }

  Create(Identifier ref, Expression params) {
    this.ref = ref;
    this.params = params;
  }

  /**
   * Returns the ref of the class in which the new instance will be created.
   */
  public Identifier ref() {
    return ref;
  }

  /**
   * Returns the parameters for the create operation.
   */
  public Expression params() {
    return params;
  }
}
