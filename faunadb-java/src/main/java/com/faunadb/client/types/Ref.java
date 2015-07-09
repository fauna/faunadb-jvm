package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An immutable representation of a FaunaDB ref type. Create an instance using the {@link Ref#create} method.
 *
 * <p><i>Example</i>: <code>{ "@ref": "classes/spells/102462014988746752" }</code>
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-values-special_types">FaunaDB Special Types</a>
 *
 */
public class Ref extends Value.ConcreteValue {
  /**
   * Creates a new {@link Ref} instance.
   * @param value the value of the Ref.
   */
  public static Ref create(String value) {
    return new Ref(value);
  }

  @JsonProperty("@ref")
  private final String value;

  @Override
  public Ref asRef() {
    return this;
  }

  @JsonCreator
  Ref(@JsonProperty("@ref") String value) {
    this.value = value;
  }

  /**
   * Returns the String representation of the ref.
   */
  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof Ref) && value.contentEquals(((Ref) obj).value());
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  /**
   * Returns the String representation of the ref. Equivalent to {@link value()}.
   */
  public String toString() {
    return value;
  }

}
