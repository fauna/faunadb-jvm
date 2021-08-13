package com.faunadb.client.errors;

import java.util.List;

public class InvalidTokenException extends FaunaException {

    public InvalidTokenException(String message, int httpStatusCode, List<String> position) {
        super(message, httpStatusCode, position);
    }

    @Override
    public CoreExceptionCodes code() {
        return CoreExceptionCodes.INVALID_TOKEN;
    }
}
