package com.faunadb.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.faunadb.client.types.Value;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static com.faunadb.client.query.Language.*;

public class ObjectCodecSpec {
  class ValueObject {
    @JsonProperty
    String something;
    @JsonProperty
    String another;
  }

  class NestedValueObject {
    @JsonProperty
    ImmutableMap<String, String> aMap;

    @JsonProperty
    ValueObject nested;
  }

  ObjectMapper json = new ObjectMapper().registerModule(new GuavaModule());

  @Test
  public void testSimpleMap() {
    ImmutableMap<String, String> someObj = ImmutableMap.of("test", "value");
    Value.ObjectV result = json.convertValue(someObj, Value.ObjectV.class);
    assertThat(result.get("test").asString(), is("value"));

    JsonNode node = json.valueToTree(result);
    assertThat(node.get("test").asText(), is("value"));
  }

  @Test
  public void testObjectWithNull() {
    ValueObject obj = new ValueObject();
    obj.something = "huh";
    obj.another = null;
    Value.ObjectV result2 = json.convertValue(obj, Value.ObjectV.class);

    assertThat(result2.get("something").asString(), is("huh"));
    assertThat(result2.get("another"), is(NullV()));

    JsonNode node = json.valueToTree(result2);
    assertThat(node.get("something").asText(), is("huh"));
    assertThat(node.has("another"), is(true));
    assertThat(node.get("another").isNull(), is(true));
  }

  @Test
  public void testNestedObject() {
    NestedValueObject obj = new NestedValueObject();
    obj.aMap = ImmutableMap.of("some", "value");
    obj.nested = new ValueObject();
    obj.nested.something = "huh";
    obj.nested.another = null;
    Value.ObjectV result = json.convertValue(obj, Value.ObjectV.class);

    assertThat(result.get("aMap").asObject().get("some").asString(), is("value"));
    assertThat(result.get("nested").asObject().get("something").asString(), is("huh"));
    assertThat(result.get("nested").asObject().get("another"), is(NullV()));

    JsonNode node = json.valueToTree(result);
    assertThat(node.get("aMap").isObject(), is(true));
    assertThat(node.get("aMap").get("some").asText(), is("value"));
    assertThat(node.get("nested").get("something").asText(), is("huh"));
    assertThat(node.get("nested").has("another"), is(true));
    assertThat(node.get("nested").get("another").isNull(), is(true));
  }
}
