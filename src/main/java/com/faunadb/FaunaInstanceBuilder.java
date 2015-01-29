package com.faunadb;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class FaunaInstanceBuilder {
    public FaunaInstanceBuilder(String ref, String classRef, long ts, ObjectNode data) {
        this.ref = ref;
        this.classRef = classRef;
        this.ts = ts;
        this.data = data;
    }

    public static FaunaInstanceBuilder create() {
        return new FaunaInstanceBuilder("", "", 0, JsonNodeFactory.instance.objectNode());
    }

    public FaunaInstanceBuilder withRef(String newRef) {
        return new FaunaInstanceBuilder(newRef, classRef, ts, data);
    }

    public FaunaInstanceBuilder withClass(String newClassRef) {
        return new FaunaInstanceBuilder(ref, newClassRef, ts, data);
    }

    public FaunaInstanceBuilder withTs(long newTs) {
        return new FaunaInstanceBuilder(ref, classRef, newTs, data);
    }

    public FaunaInstanceBuilder withData(ObjectNode newData) {
        return new FaunaInstanceBuilder(ref, classRef, ts, newData);
    }

    public FaunaInstance build() {
        return new FaunaInstance(ref, classRef, ts, data);
    }

    private final String ref;
    private final String classRef;
    private final long ts;
    private final ObjectNode data;
}
