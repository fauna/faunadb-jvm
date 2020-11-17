package com.faunadb.common.http;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

public class ResponseBodyStringProcessor {

    public static CompletableFuture<String> consumeBody(HttpResponse<Flow.Publisher<List<ByteBuffer>>> response) {
        final CompletableFuture<String> stringPromise = new CompletableFuture<>();
        Flow.Subscriber<List<ByteBuffer>> stringBodyHandlerSubscriber = new Flow.Subscriber<>() {
            Flow.Subscription subscription = null;
            List<ByteBuffer> captured = new ArrayList<>();
            @Override
            public void onSubscribe(Flow.Subscription s) {
                subscription = s;
                subscription.request(1);
            }

            @Override
            public void onNext(List<ByteBuffer> item) {
                captured.addAll(item);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                stringPromise.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                String text = captured.stream()
                        .map(b -> StandardCharsets.UTF_8.decode(b).toString())
                        .collect(Collectors.joining());
                stringPromise.complete(text);
            }
        };
        response.body().subscribe(stringBodyHandlerSubscriber);
        return stringPromise;
    }
}
