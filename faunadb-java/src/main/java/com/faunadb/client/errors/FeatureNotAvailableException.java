package com.faunadb.client.errors;

import java.util.List;

public class FeatureNotAvailableException extends FaunaException {

    public FeatureNotAvailableException(String message, int httpStatusCode, List<String> position) {
        super(message, httpStatusCode, position);
    }

    @Override
    public CoreExceptionCodes code() {
        return CoreExceptionCodes.FEATURE_NOT_AVAILABLE;
    }
}
