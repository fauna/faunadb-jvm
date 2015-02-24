package com.faunadb.query;

public class ObjectBuilder {
    public static ObjectPrimitive fromMap(java.util.Map<String, Primitive> map) {
        return new ObjectPrimitive(map);
    }
}
