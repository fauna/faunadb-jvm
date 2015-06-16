package com.faunadb.client.java.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Ref extends Value implements Identifier {
  public static Ref create(String value) {
    return new Ref(value);
  }

  @JsonProperty("@ref")
  private final String value;

  @JsonCreator
  Ref(@JsonProperty("@ref") String value) {
    this.value = value;
  }

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
  public String toString() {
    return value;
  }

}
