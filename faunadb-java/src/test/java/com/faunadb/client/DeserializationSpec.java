package com.faunadb.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.Native;
import com.faunadb.client.types.Value.ObjectV;
import com.faunadb.client.types.Value.RefV;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.faunadb.client.types.Codec.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DeserializationSpec {

  private ObjectMapper json;

  @Before
  public void setUp() {
    json = new ObjectMapper().registerModule(new Jdk8Module());
  }

  @Test
  public void shouldDeserializeString() throws Exception {
    assertThat(parsed("\"a string\"").to(STRING).get(),
      equalTo("a string"));
  }

  @Test
  public void shouldDeserializeBoolean() throws Exception {
    assertThat(parsed("true").to(BOOLEAN).get(), is(true));
    assertThat(parsed("false").to(BOOLEAN).get(), is(false));
  }

  @Test
  public void shouldDeserializeLong() throws Exception {
    assertThat(parsed(String.valueOf(Long.MAX_VALUE)).to(LONG).get(),
      equalTo(Long.MAX_VALUE));
  }

  @Test
  public void shouldDeserializeDouble() throws Exception {
    assertThat(parsed(String.valueOf(Double.MAX_VALUE)).to(DOUBLE).get(),
      equalTo(Double.MAX_VALUE));
  }

  @Test
  public void shouldDeserializeRef() throws Exception {
    assertThat(parsed("{ \"@ref\": {\"id\": \"1\", \"collection\": {\"@ref\": {\"id\": \"people\", \"collection\": { \"@ref\": {\"id\": \"collections\"} } } } } }").to(REF).get(),
      equalTo(new RefV("1", new RefV("people", Native.COLLECTIONS))));
  }

  @Test
  public void shouldDeserializeArray() throws Exception {
    Value parsed = parsed("[1, \"string\", [true, false], {\"@ref\": {\"id\": \"databases\"}}]");
    assertThat(parsed.at(0).to(LONG).get(), equalTo(1L));
    assertThat(parsed.at(1).to(STRING).get(), equalTo("string"));
    assertThat(parsed.at(2).at(0).to(BOOLEAN).get(), is(true));
    assertThat(parsed.at(2).at(1).to(BOOLEAN).get(), is(false));
    assertThat(parsed.at(3).to(REF).get(), equalTo(Native.DATABASES));
  }

  @Test
  public void shouldDeserializeDate() throws IOException {
    assertThat(parsed("{ \"@date\": \"1970-01-03\" }").to(DATE).get(),
               equalTo(LocalDate.ofEpochDay(2)));
  }

  @Test
  public void shouldDeserializeTime() throws IOException {
    assertThat(parsed("{ \"@ts\": \"1970-01-01T00:05:00Z\" }").to(TIME).get(),
               equalTo(Instant.ofEpochMilli(0).plus(5, ChronoUnit.MINUTES)));

    assertThat(parsed("{ \"@ts\": \"1970-01-01T00:00:05Z\" }").to(TIME).get(),
               equalTo(Instant.ofEpochMilli(0).plus(5, ChronoUnit.SECONDS)));

    assertThat(parsed("{ \"@ts\": \"1970-01-01T00:00:00.005Z\" }").to(TIME).get(),
      equalTo(Instant.ofEpochMilli(5)));
  }

  @Test
  public void shouldDeserializeObject() throws Exception {
    Value parsed = parsed("{" +
      "  \"ref\": {" +
      "    \"@ref\": { \"id\": \"93044099947429888\", \"collection\": { \"@ref\": { \"id\": \"spells\", \"collection\": { \"@ref\": { \"id\": \"collections\" } } } } }" +
      "  }," +
      "  \"collection\": {" +
      "   \"@ref\": { \"id\": \"spells\", \"collection\": { \"@ref\": {\"id\": \"collections\"} } }" +
      "  }," +
      "  \"ts\": 1424992618413105," +
      "  \"data\": {" +
      "   \"name\": \"fireball\"," +
      "   \"refField\": {" +
      "    \"@ref\": { \"id\": \"93044099909681152\", \"collection\": { \"@ref\": { \"id\": \"spells\", \"collection\": { \"@ref\": { \"id\": \"collections\" } } } } }" +
      "   }," +
      "   \"elements\": [\"fire\", \"air\"]" +
      "  }" +
      " }"
    );

    assertThat(parsed.at("ref").to(REF).get(), equalTo(new RefV("93044099947429888", new RefV("spells", Native.COLLECTIONS))));
    assertThat(parsed.at("collection").to(REF).get(), equalTo(new RefV("spells", Native.COLLECTIONS)));
    assertThat(parsed.at("ts").to(LONG).get(), equalTo(1424992618413105L));
    assertThat(parsed.at("data", "name").to(STRING).get(), equalTo("fireball"));
    assertThat(parsed.at("data", "refField").to(REF).get(), equalTo(new RefV("93044099909681152", new RefV("spells", Native.COLLECTIONS))));
    assertThat(parsed.at("data", "elements").at(0).to(STRING).get(), equalTo("fire"));
    assertThat(parsed.at("data", "elements").at(1).to(STRING).get(), equalTo("air"));
  }

  @Test
  public void shouldDeserializeEmptyObject() throws Exception {
    Value parsed = parsed("{}");

    assertThat(parsed, equalTo((Value)new ObjectV(new HashMap(0))));
  }

  @Test
  public void shouldDeserializeNull() throws Exception {
    assertThat(parsed("{ \"resources\": null }").at("resources").to(STRING).getOptional(),
      is(Optional.<String>empty()));

    assertThat(parsed("[1, null]").at(1).to(STRING).getOptional(),
      is(Optional.<String>empty()));
  }

  @Test
  public void shouldDeserializeObjectLiteral() throws Exception {
    Value parsed = parsed("{ \"@obj\": {\"@name\": \"Test\"}}");
    assertThat(parsed.at("@name").to(STRING).get(), equalTo("Test"));
  }

  @Test
  public void shouldDeserializeSetRef() throws Exception {
    Value parsed = parsed(
      "{" +
        "  \"@set\": {" +
        "    \"match\": { \"@ref\": {\"id\": \"spells_by_element\", \"collection\": { \"@ref\": {\"id\": \"indexes\"} } } }," +
        "    \"terms\": \"fire\"" +
        "  }" +
        "}"
    );

    Map<String, Value> set = parsed.to(SET_REF).get().parameters();
    assertThat(set.get("terms").to(STRING).get(), equalTo("fire"));
    assertThat(set.get("match").to(REF).get(),
      equalTo(new RefV("spells_by_element", Native.INDEXES)));
  }

  @Test
  public void shouldDeserializeBytes() throws Exception {
    assertThat(parsed("{\"@bytes\":\"AQIDBA==\"}").to(BYTES).get(), equalTo(new byte[]{0x1, 0x2, 0x3, 0x4}));
  }

  @Test
  public void shouldDeserializeBytesUrlSafe() throws Exception {
    assertThat(parsed("{\"@bytes\":\"-A==\"}").to(BYTES).get(), equalTo(new byte[] {(byte)0xf8}));
    assertThat(parsed("{\"@bytes\":\"-Q==\"}").to(BYTES).get(), equalTo(new byte[] {(byte)0xf9}));
    assertThat(parsed("{\"@bytes\":\"-g==\"}").to(BYTES).get(), equalTo(new byte[] {(byte)0xfa}));
    assertThat(parsed("{\"@bytes\":\"-w==\"}").to(BYTES).get(), equalTo(new byte[] {(byte)0xfb}));
    assertThat(parsed("{\"@bytes\":\"_A==\"}").to(BYTES).get(), equalTo(new byte[] {(byte)0xfc}));
    assertThat(parsed("{\"@bytes\":\"_Q==\"}").to(BYTES).get(), equalTo(new byte[] {(byte)0xfd}));
    assertThat(parsed("{\"@bytes\":\"_g==\"}").to(BYTES).get(), equalTo(new byte[] {(byte)0xfe}));
    assertThat(parsed("{\"@bytes\":\"_w==\"}").to(BYTES).get(), equalTo(new byte[] {(byte)0xff}));
  }

  private Value parsed(String str) throws java.io.IOException {
    return json.readValue(str, Value.class);
  }

}
