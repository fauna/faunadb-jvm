package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * A FaunaDB set literal.
 *
 * @see <a href="https://faunadb.com/documentation/queries#values-special_types">FaunaDB Special Types</a>
 */
@JsonDeserialize(using = Codec.SetRefDeserializer.class)
public class SetRef extends Value.ScalarValue<ImmutableMap<String, Value>> {

  public SetRef(ImmutableMap<String, Value> parameters) {
    super(parameters);
  }

  @JsonValue
  @Override
  public ImmutableMap<String, Value> asObject() {
    return value;
  }

  @Override
  public Value get(String key) {
    return value.get(key);
  }

  @Override
  public Optional<Value> getOption(String key) {
    return Optional.fromNullable(get(key));
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
