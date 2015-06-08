package com.faunadb.client.java;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.faunadb.client.java.response.Instance;
import com.faunadb.client.java.response.ResponseNode;
import com.faunadb.client.java.types.Ref;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;

public class DeserializationSpec {
  ObjectMapper json = new ObjectMapper().registerModule(new GuavaModule());

  @Test
  public void testDeserializeQueryResponseWithRefs() throws IOException {
    String toDeserialize = "{\n\t\t\"ref\": {\n\t\t\t\"@ref\": \"classes/spells/93044099947429888\"\n\t\t},\n\t\t\"class\": {\n\t\t\t\"@ref\": \"classes/spells\"\n\t\t},\n\t\t\"ts\": 1424992618413105,\n\t\t\"data\": {\n\t\t\t\"refField\": {\n\t\t\t\t\"@ref\": \"classes/spells/93044099909681152\"\n\t\t\t}\n\t\t}\n\t}";
    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Instance instance = parsed.asInstance();
    assertEquals(instance.ref(), Ref.create("classes/spells/93044099947429888"));
    assertEquals(instance.classRef(), Ref.create("classes/spells"));
    assertEquals(instance.ts().longValue(), 1424992618413105L);
    assertEquals(instance.data().get("refField").asRef(), Ref.create("classes/spells/93044099909681152"));
  }

  @Test
  public void deserializeQueryResponse() throws IOException {
    String toDeserialize = "{\n\"class\": {\n\"@ref\": \"classes/derp\"\n},\n\"data\": {\n\"test\": 1\n},\n\"ref\": {\n\"@ref\": \"classes/derp/101192216816386048\"\n},\n\"ts\": 1432763268186882\n}";
    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Instance instance = parsed.asInstance();
    assertEquals(instance.ref(), Ref.create("classes/derp/101192216816386048"));
    assertEquals(instance.classRef(), Ref.create("classes/derp"));
    assertEquals(instance.ts().longValue(), 1432763268186882L);
    assertEquals(instance.data().get("test").asNumber().intValue(), 1);
  }
}
