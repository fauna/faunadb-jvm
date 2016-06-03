package com.faunadb.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.faunadb.client.types.Ref;
import com.faunadb.client.types.Value;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DeserializationSpec {

  private ObjectMapper json;

  @Before
  public void setUp() throws Exception {
    json = new ObjectMapper().registerModule(new GuavaModule());
  }

  @Test
  public void shouldDeserializeString() throws Exception {
    assertThat(parsed("\"a string\"").asString(),
      equalTo("a string"));
  }

  @Test
  public void shouldDeserializeBoolean() throws Exception {
    assertThat(parsed("true").asBoolean(), is(true));
    assertThat(parsed("false").asBoolean(), is(false));
  }

  @Test
  public void shouldDeserializeLong() throws Exception {
    assertThat(parsed(String.valueOf(Long.MAX_VALUE)).asLong(),
      equalTo(Long.MAX_VALUE));
  }

  @Test
  public void shouldDeserializeDouble() throws Exception {
    assertThat(parsed(String.valueOf(Double.MAX_VALUE)).asDouble(),
      equalTo(Double.MAX_VALUE));
  }

  @Test
  public void shouldDeserializeRef() throws Exception {
    assertThat(parsed("{ \"@ref\": \"classes/people/1\" }").asRef(),
      equalTo(new Ref("classes/people/1")));
  }

  @Test
  public void shouldDeserializeArray() throws Exception {
    Value parsed = parsed("[1, \"string\", [true, false], {\"@ref\": \"databases\"}]");
    assertThat(parsed.get(0).asLong(), equalTo(1L));
    assertThat(parsed.get(1).asString(), equalTo("string"));
    assertThat(parsed.get(2).get(0).asBoolean(), is(true));
    assertThat(parsed.get(2).get(1).asBoolean(), is(false));
    assertThat(parsed.get(3).asRef(), equalTo(new Ref("databases")));
  }

  @Test
  public void shouldDeserializeDate() throws IOException {
    assertThat(parsed("{ \"@date\": \"1970-01-03\" }").asDate(),
      equalTo(LocalDate.ofEpochDay(2)));
  }

  @Test
  public void shouldDeserializeTS() throws IOException {
    assertThat(parsed("{ \"@ts\": \"1970-01-01T00:05:00Z\" }").asTs(),
      equalTo(Instant.EPOCH.plus(5, ChronoUnit.MINUTES)));
  }

  @Test
  public void shouldDeserializeObject() throws Exception {
    Value parsed = parsed("{" +
      "  \"ref\": {" +
      "   \"@ref\": \"classes/spells/93044099947429888\"" +
      "  }," +
      "  \"class\": {" +
      "   \"@ref\": \"classes/spells\"" +
      "  }," +
      "  \"ts\": 1424992618413105," +
      "  \"data\": {" +
      "   \"name\": \"fireball\"," +
      "   \"refField\": {" +
      "    \"@ref\": \"classes/spells/93044099909681152\"" +
      "   }," +
      "   \"elements\": [\"fire\", \"air\"]" +
      "  }" +
      " }"
    );

    assertThat(parsed.get("ref").asRef(), equalTo(new Ref("classes/spells/93044099947429888")));
    assertThat(parsed.get("class").asRef(), equalTo(new Ref("classes/spells")));
    assertThat(parsed.get("ts").asLong(), equalTo(1424992618413105L));
    assertThat(parsed.get("data", "name").asString(), equalTo("fireball"));
    assertThat(parsed.get("data", "refField").asRef(), equalTo(new Ref("classes/spells/93044099909681152")));
    assertThat(parsed.get("data", "elements").get(0).asString(), equalTo("fire"));
    assertThat(parsed.get("data", "elements").get(1).asString(), equalTo("air"));
  }

  @Test
  public void shouldDeserializeNull() throws Exception {
    assertThat(parsed("{ \"resources\": null }").get("resources").asStringOption(),
      is(Optional.<String>absent()));

    assertThat(parsed("[1, null]").get(1).asStringOption(),
      is(Optional.<String>absent()));
  }

  @Test
  public void shouldDeserializeObjectLiteral() throws Exception {
    Value parsed = parsed("{ \"@obj\": {\"@name\": \"Test\"}}");
    assertThat(parsed.get("@name").asString(), equalTo("Test"));
  }

  @Test
  public void shouldDeserializeSetRef() throws Exception {
    Value parsed = parsed(
      "{" +
        "  \"@set\": {" +
        "    \"match\": { \"@ref\": \"indexes/spells_by_element\" }," +
        "    \"terms\": \"fire\"" +
        "  }" +
        "}"
    );

    ImmutableMap<String, Value> set = parsed.asSetRef().parameters();
    assertThat(set.get("terms").asString(), equalTo("fire"));
    assertThat(set.get("match").asRef(),
      equalTo(new Ref("indexes/spells_by_element")));
  }

  private Value parsed(String str) throws java.io.IOException {
    return json.readValue(str, Value.class);
  }

}
