package com.faunadb.client.errors;

import java.util.List;

public class StackOverflowException extends FaunaException {

    public StackOverflowException(String message, int httpStatusCode, List<String> position) {
        super(message, httpStatusCode, position);
    }

    @Override
    public CoreExceptionCodes code() {
        return CoreExceptionCodes.STACK_OVERFLOW;
    }
}
