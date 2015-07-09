package com.faunadb.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.faunadb.client.types.LazyValue;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ResponseNodeSpec {
  ObjectMapper json = new ObjectMapper().registerModule(new GuavaModule());

  @Test
  public void coerceIntoInvalidTypeReturnsNull() {
    JsonNode tree = json.createObjectNode().put("some", "string");
    LazyValue node = LazyValue.create(tree, json);
    assertThat(node.get("some").asString(), is("string"));

    assertNull(node.get("some").asBoolean());
    assertNull(node.get("some").asLong());
    assertNull(node.get("some").asDouble());
    assertNull(node.get("some").asRef());

    assertNull(node.get("some").asArray());
    assertNull(node.get("some").asObject());
    assertNull(node.get("some").asKey());
    assertNull(node.get("some").asClass());
    assertNull(node.get("some").asDatabase());
    assertNull(node.get("some").asEvent());
    assertNull(node.get("some").asIndex());
    assertNull(node.get("some").asPage());
    assertNull(node.get("some").asSet());

    JsonNode tree2 = json.createObjectNode().set("some", json.createArrayNode().add("array"));
    LazyValue node2 = LazyValue.create(tree2, json);
    assertNull(node2.get("some").asObject());
    assertNull(node2.get("some").asString());

    JsonNode tree3 = json.createObjectNode().set("some", json.createObjectNode().put("object", "key"));
    LazyValue node3 = LazyValue.create(tree3, json);
    assertNull(node3.get("some").asArray());
    assertNull(node3.get("some").asString());
  }
}
