package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * A FaunaDB set literal.
 *
 * @see <a href="https://faunadb.com/documentation/queries#values-special_types">FaunaDB Special Types</a>
 */
public class SetRef extends Value.ScalarValue<ImmutableMap<String, Value>> {

  public SetRef(@JsonProperty("@set") ImmutableMap<String, Value> parameters) {
    super(parameters);
  }

  @JsonValue
  public ImmutableMap<String, Value> parameters() {
    return value;
  }

  @Override
  public SetRef asSetRef() {
    return this;
  }

  @Override
  public Optional<SetRef> asSetRefOption() {
    return Optional.of(this);
  }

}
