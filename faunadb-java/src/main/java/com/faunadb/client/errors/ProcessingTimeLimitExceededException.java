package com.faunadb.client.errors;

public class ProcessingTimeLimitExceededException extends FaunaException {

    public ProcessingTimeLimitExceededException(String message, int httpStatusCode) {
        super(message, httpStatusCode);
    }
}
