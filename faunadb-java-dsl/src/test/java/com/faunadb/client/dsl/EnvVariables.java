package com.faunadb.client.dsl;

import java.util.Objects;

import static java.lang.String.format;

final public class EnvVariables {

  private EnvVariables() {
  }

  public static String require(String name) {
    String value = System.getenv(name);
    if (value == null)
      throw new RuntimeException(format("%s must be defined to run tests", name));

    return value;
  }

  public static String getOrElse(String name, String defaultValue) {
    String value = System.getenv(name);
    return value != null ? value : Objects.requireNonNull(defaultValue);
  }

}
