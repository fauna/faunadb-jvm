package com.faunadb.client.errors;

/**
 * An exception thrown if an error event is receiving on a stream.
 */
public class StreamingException extends FaunaException {
    public StreamingException(String message) {
        super(message);
    }
}
