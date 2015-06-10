package com.faunadb.client.java.errors;

public class NotFoundException extends FaunaException {
  public NotFoundException(String message) {
    super(message);
  }
}
