package com.faunadb.client.errors;

import java.util.List;

public class FunctionCallException extends FaunaException {

    private List<FaunaException> exceptions;

    public FunctionCallException(String message, int httpStatusCode, List<String> position, List<FaunaException> exceptions) {
        super(message, httpStatusCode, position);
        this.exceptions = exceptions;
    }

    public List<FaunaException> getExceptions() {
        return exceptions;
    }

    @Override
    public CoreExceptionCodes code() {
        return CoreExceptionCodes.CALL_ERROR;
    }
}
