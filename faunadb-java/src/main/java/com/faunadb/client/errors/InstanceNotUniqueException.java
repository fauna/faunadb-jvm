package com.faunadb.client.errors;

import java.util.List;

public class InstanceNotUniqueException extends FaunaException {

    public InstanceNotUniqueException(String message, int httpStatusCode, List<String> position) {
        super(message, httpStatusCode, position);
    }

    @Override
    public CoreExceptionCodes code() {
        return CoreExceptionCodes.INSTANCE_NOT_UNIQUE;
    }
}
