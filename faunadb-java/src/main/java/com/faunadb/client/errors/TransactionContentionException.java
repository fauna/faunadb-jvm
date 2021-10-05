package com.faunadb.client.errors;

import com.faunadb.client.HttpResponses;

/**
 * An exception thrown if a HTTP 409 is returned from FaunaDB.
 */
public class TransactionContentionException extends FaunaException {
    public TransactionContentionException(HttpResponses.QueryErrorResponse response) {
        super(response);
    }

    public TransactionContentionException(String message) {
        super(message);
    }
}
