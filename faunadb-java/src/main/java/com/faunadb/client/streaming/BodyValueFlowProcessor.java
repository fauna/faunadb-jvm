package com.faunadb.client.streaming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.errors.PermissionDeniedException;
import com.faunadb.client.types.Result;
import com.faunadb.client.types.Value;
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
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper json = new ObjectMapper();
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

            Boolean errorEventType = value.at("event")
                .getOptional()
                .flatMap(v -> v.to(String.class).getOptional())
                .map(s -> s.equals("error"))
                .orElse(false);

            if (errorEventType) {
                Boolean unrecoverablePermissionError = value.at("data", "code")
                        .getOptional()
                        .flatMap(v -> v.to(String.class).getOptional())
                        .map(s -> s.equals("permission denied"))
                        .orElse(false);
                if (unrecoverablePermissionError) {
                    String description =  value.at("data", "description")
                            .getOptional()
                            .flatMap(v -> v.to(String.class).getOptional())
                            .orElse("no description available");
                    Exception ex = new PermissionDeniedException("code: permission denied, description:" + description);
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
