package com.faunadb.client.test;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

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
