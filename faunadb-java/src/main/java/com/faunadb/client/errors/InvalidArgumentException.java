package com.faunadb.client.errors;

import java.util.List;

public class InvalidArgumentException extends FaunaException {

    public InvalidArgumentException(String message, int httpStatusCode, List<String> position) {
        super(message, httpStatusCode, position);
    }

    @Override
    public CoreExceptionCodes code() {
        return CoreExceptionCodes.INVALID_ARGUMENT;
    }
}
