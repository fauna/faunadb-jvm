package com.faunadb;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class FaunaInstanceBuilder {
    public FaunaInstanceBuilder(String ref, String classRef, long ts, ObjectNode data, ObjectNode constraints) {
        this.ref = ref;
        this.classRef = classRef;
        this.ts = ts;
        this.data = data;
        this.constraints = constraints;
    }

    public static FaunaInstanceBuilder create() {
        return new FaunaInstanceBuilder("", "", 0, JsonNodeFactory.instance.objectNode(), JsonNodeFactory.instance.objectNode());
    }

    public FaunaInstanceBuilder withRef(String newRef) {
        return new FaunaInstanceBuilder(newRef, classRef, ts, data, constraints);
    }

    public FaunaInstanceBuilder withClass(String newClassRef) {
        return new FaunaInstanceBuilder(ref, newClassRef, ts, data, constraints);
    }

    public FaunaInstanceBuilder withTs(long newTs) {
        return new FaunaInstanceBuilder(ref, classRef, newTs, data, constraints);
    }

    public FaunaInstanceBuilder withData(ObjectNode newData) {
        return new FaunaInstanceBuilder(ref, classRef, ts, newData, constraints);
    }

    public FaunaInstanceBuilder withConstraints(ObjectNode newConstraints) {
        return new FaunaInstanceBuilder(ref, classRef, ts, data, newConstraints);
    }

    public FaunaInstance build() {
        return new FaunaInstance(ref, classRef, ts, data, constraints, JsonNodeFactory.instance.objectNode());
    }

    private final String ref;
    private final String classRef;
    private final long ts;
    private final ObjectNode data;
    private final ObjectNode constraints;
}
