package com.faunadb.client.errors;

import com.faunadb.client.HttpResponses;

import java.util.List;

/**
 * An exception thrown if an error event is receiving on a stream.
 */
public class StreamingException extends FaunaException {
    public StreamingException(HttpResponses.QueryError queryError) {
        super(FaunaException.constructErrorMessage(List.of(queryError)));
    }
}
