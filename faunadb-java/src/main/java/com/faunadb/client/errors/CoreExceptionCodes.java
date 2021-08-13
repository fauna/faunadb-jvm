package com.faunadb.client.errors;

public enum CoreExceptionCodes {
    INVALID_ARGUMENT("invalid argument"),
    CALL_ERROR("call error"),
    PERMISSION_DENIED("permission denied"),
    INVALID_EXPRESSION("invalid expression"),
    INVALID_URL_PARAMETER("invalid url parameter"),
    TRANSACTION_ABORTED("transaction aborted"),
    INVALID_WRITE_TIME("invalid write time"),
    INVALID_REF("invalid ref"),
    MISSING_IDENTITY("missing identity"),
    INVALID_TOKEN("invalid token"),
    STACK_OVERFLOW("stack overflow"),
    AUTHENTICATION_FAILED("authentication failed"),
    VALUE_NOT_FOUND("value not found"),
    INSTANCE_NOT_FOUND("instance not found"),
    INSTANCE_ALREADY_EXISTS("instance already exists"),
    VALIDATION_FAILED("validation failed"),
    INSTANCE_NOT_UNIQUE("instance not unique"),
    FEATURE_NOT_AVAILABLE("feature not available"),
    UNKNOWN_ERROR("unknown error");

    private final String code;

    private CoreExceptionCodes(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    public static CoreExceptionCodes elemByCode(String code) {
        for (CoreExceptionCodes item : CoreExceptionCodes.values()) {
            if (item.getCode().equals(code)) return item;
        }
        return UNKNOWN_ERROR;
    }
}
