package com.faunadb.client.streaming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.HttpResponses;
import com.faunadb.client.errors.PermissionDeniedException;
import com.faunadb.client.types.Field;
import com.faunadb.client.types.Result;
import com.faunadb.client.types.Value;
import com.faunadb.common.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.Collectors;

public class BodyValueFlowProcessor extends SubmissionPublisher<Value> implements Flow.Processor<java.util.List<ByteBuffer>, Value> {

    public BodyValueFlowProcessor(ObjectMapper json, Connection connection) {
        this.json = json;
        this.connection = connection;
    }

    private static Value ErrorValue = new Value.StringV("error");
    private static Field<Long> TxnField = Field.at("txn").to(Long.class);

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ObjectMapper json;
    private Connection connection;
    private Flow.Subscription subscription = null;
    private Flow.Subscriber<? super Value> subscriber = null;

    private void requestOne() {
        subscription.request(1);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Value> subscriber) {
        if (this.subscriber == null) {
            this.subscriber = subscriber;
            super.subscribe(subscriber);
            requestOne();
        } else
            throw new IllegalStateException("BodyValueFlowProcessor can have only one subscriber");
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void onNext(List<ByteBuffer> items) {
        String text = items.stream()
            .map(b -> StandardCharsets.US_ASCII.decode(b).toString())
            .collect(Collectors.joining());

        try {
            JsonNode jsonNode = json.readTree(text);
            Value value = json.treeToValue(jsonNode, Value.class);
            // update connection last txn time
            value.getOptional(TxnField).ifPresent(ts -> connection.syncLastTxnTime(ts));

            Boolean errorEventType = value.at("type")
                .getOptional()
                .map(v -> v.equals(ErrorValue))
                .orElse(false);

            if (errorEventType) {
                HttpResponses.QueryError queryError = json.treeToValue(jsonNode.get("event"), HttpResponses.QueryError.class);
                boolean unrecoverablePermissionError = queryError.code().equals("permission denied");
                if (unrecoverablePermissionError) {
                    HttpResponses.QueryErrorResponse qer = new HttpResponses.QueryErrorResponse(401, List.of(queryError));
                    Exception ex = new PermissionDeniedException(qer);
                    subscriber.onError(ex); // notify subscriber stream
                    subscription.cancel(); // cancel subscription on the request body
                } else {
                    submit(value); // submit domain error event
                }
            } else {
                submit(value);
            }
        } catch (Exception ex){
            log.error("could not parse event " + text, ex);
            subscriber.onError(ex); // notify subscriber stream
            subscription.cancel(); // cancel subscription on the request body
        }

        requestOne();
    }

    @Override
    public void onError(Throwable throwable) {
        log.error("unrecoverable error encountered by subscription", throwable);
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        log.debug("subscription completed");
        subscriber.onComplete();
    }
}
