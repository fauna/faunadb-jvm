package com.faunadb.client.streaming;

import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Field;
import com.faunadb.client.types.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class SnapshotEventFlowProcessor extends SubmissionPublisher<Value> implements Flow.Processor<Value, Value> {

    public SnapshotEventFlowProcessor(Expr target, Function<Expr, CompletableFuture<Value>> loadDocument) {
        this.loadDocument = loadDocument;
        this.target = target;
    }

    private static Field<Long> TxnField = Field.at("txn").to(Long.class);
    private static Field<Long> TsField = Field.at("ts").to(Long.class);
    private static Field<String> TypeField = Field.at("type").to(String.class);

    private final Logger log = LoggerFactory.getLogger(getClass());
    private Function<Expr, CompletableFuture<Value>> loadDocument;
    private Expr target;
    private Flow.Subscription subscription = null;
    private Flow.Subscriber<? super Value> subscriber = null;
    private Long snapshotTS = null;
    private AtomicBoolean initialized = new AtomicBoolean(false);

    private void requestOne() {
        subscription.request(1);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Value> subscriber) {
        if (this.subscriber == null) {
            this.subscriber = subscriber;
            super.subscribe(subscriber);
            requestOne();
        } else {
            throw new IllegalStateException("SnapshotEventFlowProcessor can have only one subscriber");
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void onNext(Value event) {
        if (initialized.get()) {
            Long eventTS = event.get(TxnField);
            if (eventTS > snapshotTS) submit(event); // ignore event older than doc. snapshot
            requestOne();
        } else {
            // not initialized receiving first element
            boolean isStartEvent = event.getOptional(TypeField).map(s -> s.equals("start")).orElse(false);
            if (isStartEvent) {
                loadDocument.apply(target).whenComplete((documentSnapshot, ex) -> {
                    if (ex != null) {
                        onError(ex);
                        subscription.cancel();
                    } else {
                        snapshotTS = documentSnapshot.get(TsField);
                        // send start event first
                        submit(event);
                        // follow up with the snapshot event
                        Map<String, Value> fields = new HashMap<>();
                        fields.put("type", new Value.StringV("snapshot"));
                        fields.put("txn", new Value.LongV(snapshotTS));
                        fields.put("event", documentSnapshot);
                        Value documentEvent = new Value.ObjectV(fields);
                        submit(documentEvent);
                        initialized.set(true);
                        // only request more when we are ready in order to avoid race condition
                        requestOne();
                    }
                });
            } else {
                onError(new IllegalArgumentException("Stream did not begin with a `start` event but with" + event));
                subscription.cancel();
            }
        }
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
