package com.faunadb.client.java.errors;

public class UnavailableException extends FaunaException {
  public UnavailableException(String message) {
    super("FaunaDB host unavailable: "+message);
  }
}
