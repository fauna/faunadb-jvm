package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

/**
 * A FaunaDB ref type.
 *
 * @see <a href="https://faunadb.com/documentation/queries#values-special_types">FaunaDB Special Types</a>
 */
public class Ref extends Value.ScalarValue<Value> {

  @JsonCreator
  public Ref(@JsonProperty("@ref") String value) {
    this(new StringV(value));
  }

  public Ref(Value ref) {
    super(ref);
  }

  /**
   * Extracts its string value.
   *
   * @return a string with the ref value
   */
  @JsonProperty("@ref")
  public String value() {
    return value.asString();
  }

  @Override
  public Ref asRef() {
    return this;
  }

  @Override
  public Optional<Ref> asRefOption() {
    return Optional.of(this);
  }

}
