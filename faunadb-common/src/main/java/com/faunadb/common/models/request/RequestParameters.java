package com.faunadb.common.models.request;

import com.faunadb.common.models.tags.Tag;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Defines parameters which be included with the outgoing request.
 */
public class RequestParameters {

    private static final int MAX_TAGS_PER_REQUEST = 25;
    private final Optional<Duration> timeout;
    private final Optional<String> traceId;
    private final Set<Tag> tags;

    public static RequestParameters fromOptionalTimeout(Optional<Duration> timeout) {
        return new RequestParameters(timeout, Optional.empty(), new HashSet<>());
    }

    public RequestParameters() {
        this.timeout = Optional.empty();
        this.traceId = Optional.empty();
        this.tags = new HashSet<>();
    }

    public RequestParameters(Optional<Duration> timeout, Optional<String> traceId, Set<Tag> tags) {

        if (tags == null) {
            throw new RuntimeException("Tags cannot be null. Consider passing an empty set instead");
        } else if (tags.size() > MAX_TAGS_PER_REQUEST) {
            throw new RuntimeException(
                    String.format("Maximum number of tags provided, current max is %s", MAX_TAGS_PER_REQUEST));
        }

        this.timeout = timeout;
        this.traceId = traceId;
        this.tags = new HashSet<>(tags);
    }

    public Optional<Duration> getTimeout() {
        return this.timeout;
    }

    public Optional<String> getTraceId() {
        return this.traceId;
    }

    public Set<Tag> getTags() {
        return this.tags;
    }
}
