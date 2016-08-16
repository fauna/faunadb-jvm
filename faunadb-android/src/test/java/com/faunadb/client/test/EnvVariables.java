package com.faunadb.client.test;

import static com.faunadb.client.util.Objects.requireNonNull;
import static java.lang.String.format;

final class EnvVariables {

  private EnvVariables() {
  }

  static String require(String name) {
    String value = System.getenv(name);
    if (value == null)
      throw new RuntimeException(format("%s must be defined to run tests", name));

    return value;
  }

  static String getOrElse(String name, String defaultValue) {
    String value = System.getenv(name);
    return value != null ? value : requireNonNull(defaultValue);
  }

}
