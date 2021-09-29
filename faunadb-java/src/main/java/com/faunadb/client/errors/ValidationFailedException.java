package com.faunadb.client.errors;

import java.util.List;

public class ValidationFailedException extends FaunaException {

    private List<String> failures;

    public ValidationFailedException(String message, int httpStatusCode, List<String> position, List<String> failures) {
        super(message, httpStatusCode, position);
        this.failures = failures;
    }

    public List<String> getFailures() {
        return failures;
    }

    @Override
    public CoreExceptionCodes code() {
        return CoreExceptionCodes.VALIDATION_FAILED;
    }
}
