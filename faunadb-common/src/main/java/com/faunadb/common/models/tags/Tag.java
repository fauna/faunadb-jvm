package com.faunadb.common.models.tags;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines a key-value pair to be attached to queries.
 * <p>
 * Tags are unique for a given key. Tags added to the same {@link java.util.Set} will collide with the latest
 * overwriting any earlier instances having the same key, e.g. [key=Key1, value=Value1] will be overridden by
 * [key=Key1, value=Value2] when added to the same {@link java.util.Set}.
 * <p>
 * This classes enforces validation on both the key and value for allowed characters and length.
 */
public class Tag {

    private static final int KEY_CHAR_LIMIT = 40;
    private static final int VALUE_CHAR_LIMIT = 80;

    private static final Pattern ALLOWABLE_CHARACTER_REGEX = Pattern.compile("^\\w+$");
    private String key;
    private String value;


    public Tag(String key, String value) {
        this.key = getValidatedKey(key);
        this.value = getValidatedValue(value);
    }

    @Override
    public int hashCode() {
        // For the purposes of hashing, two Tag objects with the same key should collide
        return Objects.hash(this.key);
    }

    @Override
    public boolean equals(Object obj) {
        // self check
        if (this == obj) return true;
        // null check
        if (obj == null) return false;
        // type check and cast
        if (getClass() != obj.getClass()) return false;
        Tag tag = (Tag) obj;
        // field comparison
        return Objects.equals(this.key, tag.key);
    }

    @Override
    public String toString() {
        return String.format("%s=%s", this.key, this.value);
    }

    private String getValidatedKey(String key) {
        if (key == null || key.isBlank()) {
            throw new RuntimeException("Empty keys not allowed");
        } else if (key.length() > KEY_CHAR_LIMIT) {
            throw new RuntimeException(
                    String.format("Key, %s, is longer than the allowable limit of %d characters", key, KEY_CHAR_LIMIT));
        }
        Matcher m = ALLOWABLE_CHARACTER_REGEX.matcher(key);
        if (m.matches()) {
            return key;
        } else {
            throw new RuntimeException(String.format("Provided key, %s, contains invalid characters", key));
        }
    }

    private String getValidatedValue(String value) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Empty values not allowed");
        } else if (value.length() > VALUE_CHAR_LIMIT) {
            throw new RuntimeException(
                    String.format("Value, %s, is longer than the allowable limit of %d characters", value,
                                  VALUE_CHAR_LIMIT
                    ));
        }
        Matcher m = ALLOWABLE_CHARACTER_REGEX.matcher(value);
        if (m.matches()) {
            return value;
        } else {
            throw new RuntimeException(String.format("Provided value, %s, contains invalid characters", value));
        }
    }
}
