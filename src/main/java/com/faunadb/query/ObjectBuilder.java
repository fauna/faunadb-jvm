package com.faunadb.query;

public class ObjectBuilder {
    public static ObjectV fromMap(java.util.Map<String, Value> map) {
        return new ObjectV(map);
    }
}
