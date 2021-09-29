package com.faunadb.client.errors;

/**
 * An exception thrown if a HTTP 409 is returned from FaunaDB.
 */
public class TransactionContentionException extends FaunaException {

    public TransactionContentionException(String message, int httpStatusCode) {
        super(message, httpStatusCode);
    }
}
