package com.fauna.learn;

import com.faunadb.client.types.FaunaConstructor;
import com.faunadb.client.types.FaunaField;
import com.faunadb.client.types.FaunaIgnore;

public class Spell {

    @FaunaField private String name;
    @FaunaField private String element;
    @FaunaField private int cost;

    @FaunaIgnore  private String notUsed;

    @FaunaConstructor
    public Spell(@FaunaField("name") String name, @FaunaField("element") String element, @FaunaField("cost") int cost) {
        this.name = name;
        this.element = element;
        this.cost = cost;
    }

    public String getName() {
        return name;
    }

    public String getElement() {
        return element;
    }

    public int getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "Spell{" +
            "name='" + name + '\'' +
            ", element='" + element + '\'' +
            ", cost=" + cost +
            '}';
    }
}
