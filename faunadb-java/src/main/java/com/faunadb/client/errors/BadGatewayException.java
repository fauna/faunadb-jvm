package com.faunadb.client.errors;

public class BadGatewayException extends FaunaException {

    public BadGatewayException(String message, int httpStatusCode) {
        super(message, httpStatusCode);
    }
}
