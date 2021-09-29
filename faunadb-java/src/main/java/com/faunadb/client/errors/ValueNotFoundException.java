package com.faunadb.client.errors;

import java.util.List;

public class ValueNotFoundException extends FaunaException {

    public ValueNotFoundException(String message, int httpStatusCode, List<String> position) {
        super(message, httpStatusCode, position);
    }

    @Override
    public CoreExceptionCodes code() {
        return CoreExceptionCodes.VALUE_NOT_FOUND;
    }
}
