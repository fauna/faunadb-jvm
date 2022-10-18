package com.faunadb.common.models.request;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines parameters which be included with the outgoing request.
 */
public class RequestParameters {

    private static final int MAX_TAGS_PER_REQUEST = 25;

    private static final int KEY_CHAR_LIMIT = 40;
    private static final int VALUE_CHAR_LIMIT = 80;

    private static final Pattern ALLOWABLE_CHARACTER_REGEX = Pattern.compile("^\\w+$");

    private final Optional<Duration> timeout;
    private final Optional<String> traceId;
    private final Map<String, String> tags;

    /**
     * Constructs a {@link RequestParameters} instance from a provided timeout
     * @param timeout
     * @return {@link RequestParameters}
     */
    public static RequestParameters fromOptionalTimeout(Optional<Duration> timeout) {
        return new RequestParameters(timeout, Optional.empty(), new HashMap<>());
    }

    /**
     * Constructs an empty {@link RequestParameters} instance
     */
    public RequestParameters() {
        this.timeout = Optional.empty();
        this.traceId = Optional.empty();
        this.tags = new HashMap<>();
    }

    /**
     * Constructs a {@link RequestParameters} instance, using the provided inputs.
     * @param timeout Timeout for the request
     * @param traceId A unique identifier for this query. Adheres to the
     *                [W3C Trace Context](https://w3c.github.io/trace-context) spec.
     * @param tags    Key-value pair metadata to associate with this query.
     * @throws IllegalArgumentException If the number of tags provided exceeds the maximum.
     */
    public RequestParameters(Optional<Duration> timeout, Optional<String> traceId, Map<String, String> tags) {

        if (tags == null) {
            throw new IllegalArgumentException("Tags cannot be null. Consider passing an empty set instead");
        } else if (tags.size() > MAX_TAGS_PER_REQUEST) {
            throw new IllegalArgumentException(
                    String.format("Maximum number of tags provided, current max is %s", MAX_TAGS_PER_REQUEST));
        }

        this.timeout = timeout;
        this.traceId = traceId;
        this.tags = new HashMap<>(getValidatedTags(tags));
    }

    /**
     * Get the timeout associated with this request
     * @return timeout
     */
    public Optional<Duration> getTimeout() {
        return this.timeout;
    }

    /**
     * Get the traceId associated with this request
     * @return traceId
     */
    public Optional<String> getTraceId() {
        return this.traceId;
    }

    /**
     * Get the tags associated with this request
     * @return tags
     */
    public Map<String, String> getTags() {
        return this.tags;
    }

    private Map<String, String> getValidatedTags(Map<String, String> tags) {
        tags.entrySet().stream().forEach(entry -> {
            validateKey(entry.getKey());
            validateValue(entry.getValue());
        });
        return tags;
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Empty keys not allowed");
        } else if (key.length() > KEY_CHAR_LIMIT) {
            throw new IllegalArgumentException(
                    String.format("Key, %s, is longer than the allowable limit of %d characters", key, KEY_CHAR_LIMIT));
        }
        Matcher m = ALLOWABLE_CHARACTER_REGEX.matcher(key);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("Provided key, %s, contains invalid characters", key));
        }
    }

    private void validateValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Empty values not allowed");
        } else if (value.length() > VALUE_CHAR_LIMIT) {
            throw new IllegalArgumentException(
                    String.format("Value, %s, is longer than the allowable limit of %d characters", value,
                                  VALUE_CHAR_LIMIT
                    ));
        }
        Matcher m = ALLOWABLE_CHARACTER_REGEX.matcher(value);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("Provided value, %s, contains invalid characters", value));
        }
    }
}
