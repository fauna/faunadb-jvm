package com.faunadb.client.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.faunadb.client.java.response.*;
import com.faunadb.client.java.response.Class;
import com.faunadb.client.java.types.Ref;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class DeserializationSpec {
  ObjectMapper json = new ObjectMapper().registerModule(new GuavaModule());

  @Test
  public void deserializeInstanceResponseWithRefs() throws IOException {
    String toDeserialize = "{\n\t\t\"ref\": {\n\t\t\t\"@ref\": \"classes/spells/93044099947429888\"\n\t\t},\n\t\t\"class\": {\n\t\t\t\"@ref\": \"classes/spells\"\n\t\t},\n\t\t\"ts\": 1424992618413105,\n\t\t\"data\": {\n\t\t\t\"refField\": {\n\t\t\t\t\"@ref\": \"classes/spells/93044099909681152\"\n\t\t\t}\n\t\t}\n\t}";
    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Instance instance = parsed.asInstance();
    assertEquals(instance.ref(), Ref.create("classes/spells/93044099947429888"));
    assertEquals(instance.classRef(), Ref.create("classes/spells"));
    assertEquals(instance.ts().longValue(), 1424992618413105L);
    assertEquals(instance.data().get("refField").asRef(), Ref.create("classes/spells/93044099909681152"));
  }

  @Test
  public void deserializeInstanceResponse() throws IOException {
    String toDeserialize = "{\n\"class\": {\n\"@ref\": \"classes/derp\"\n},\n\"data\": {\n\"test\": 1\n},\n\"ref\": {\n\"@ref\": \"classes/derp/101192216816386048\"\n},\n\"ts\": 1432763268186882\n}";
    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Instance instance = parsed.asInstance();
    assertEquals(instance.ref(), Ref.create("classes/derp/101192216816386048"));
    assertEquals(instance.classRef(), Ref.create("classes/derp"));
    assertEquals(instance.ts().longValue(), 1432763268186882L);
    assertEquals(instance.data().get("test").asNumber().intValue(), 1);
  }

  @Test
  public void deserializeInstanceResponseWithObjectLiteral() throws IOException {
    String toDeserialize = "{\n\"class\": {\n\"@ref\": \"classes/derp\"\n},\n\"data\": {\n\"test\": {\n\"field1\": {\n\"@obj\": {\n\"@name\": \"Test\"\n}\n}\n}\n},\n\"ref\": {\n\"@ref\": \"classes/derp/101727203651223552\"\n},\n\"ts\": 1433273471399755\n}";
    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Instance instance = parsed.asInstance();
    System.out.println(instance);
    ResponseMap unwrappedField = instance.data().get("test").asObject().get("field1").asObject();
    assertEquals("Test", unwrappedField.get("@name").asString());
  }

  @Test
  public void deserializeDatabaseResponse() throws IOException {
    String toDeserialize = "{\n" +
        "        \"class\": {\n" +
        "            \"@ref\": \"databases\"\n" +
        "        },\n" +
        "        \"name\": \"spells\",\n" +
        "        \"ref\": {\n" +
        "            \"@ref\": \"databases/spells\"\n" +
        "        },\n" +
        "        \"ts\": 1434343547025544\n" +
        "    }\n";

    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Database database = parsed.asDatabase();
    assertThat(database.name(), is("spells"));
    assertThat(database.classRef(), is(Ref.create("databases")));
    assertThat(database.ts(), is(1434343547025544L));
    assertThat(database.ref(), is(Ref.create("databases/spells")));
  }

  @Test
  public void deserializeKeyResponse() throws IOException {
    String toDeserialize = " {\n" +
        "        \"class\": {\n" +
        "            \"@ref\": \"keys\"\n" +
        "        },\n" +
        "        \"data\": {\n" +
        "            \"data\": \"yeah\",\n" +
        "            \"some\": 123\n" +
        "        },\n" +
        "        \"database\": {\n" +
        "            \"@ref\": \"databases/spells\"\n" +
        "        },\n" +
        "        \"hashed_secret\": \"$2a$05$LKJiF.hpkt40W9oMC/5atu2g03m2.cPGU9Srys5vmAdOgBaGYjfl2\",\n" +
        "        \"ref\": {\n" +
        "            \"@ref\": \"keys/102850208874889216\"\n" +
        "        },\n" +
        "        \"role\": \"server\",\n" +
        "        \"secret\": \"kqoBbWW4VRAAAAACtCcfczgIhDni0TUjuk5RxoNwpgzx\",\n" +
        "        \"ts\": 1434344452631179\n" +
        "    }\n";

    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Key key = parsed.asKey();
    assertThat(key.classRef(), is(Ref.create("keys")));
    assertThat(key.database(), is(Ref.create("databases/spells")));
    assertThat(key.data().get("some").asNumber(), is(123L));
    assertThat(key.data().get("data").asString(), is("yeah"));
    assertThat(key.hashedSecret(), is("$2a$05$LKJiF.hpkt40W9oMC/5atu2g03m2.cPGU9Srys5vmAdOgBaGYjfl2"));
    assertThat(key.ref(), is(Ref.create("keys/102850208874889216")));
    assertThat(key.role(), is("server"));
    assertThat(key.secret(), is("kqoBbWW4VRAAAAACtCcfczgIhDni0TUjuk5RxoNwpgzx"));
    assertThat(key.ts(), is(1434344452631179L));
  }

  @Test
  public void deserializeClassResponse() throws IOException {
    String toDeserialize = " {\n" +
        "        \"class\": {\n" +
        "            \"@ref\": \"classes\"\n" +
        "        },\n" +
        "        \"history_days\": 30,\n" +
        "        \"name\": \"spells\",\n" +
        "        \"ref\": {\n" +
        "            \"@ref\": \"classes/spells\"\n" +
        "        },\n" +
        "        \"ts\": 1434344944425065\n" +
        "    }";
    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Class cls = parsed.asClass();
    assertThat(cls.classRef(), is(Ref.create("classes")));
    assertThat(cls.historyDays(), is(30L));
    assertThat(cls.name(), is("spells"));
    assertThat(cls.ref(), is(Ref.create("classes/spells")));
    assertThat(cls.ts(), is(1434344944425065L));
  }

  @Test
  public void deserializeIndexResponse() throws IOException {
    String toDeserialize = "{\n" +
        "        \"active\": false,\n" +
        "        \"class\": {\n" +
        "            \"@ref\": \"indexes\"\n" +
        "        },\n" +
        "        \"name\": \"spells_by_name\",\n" +
        "        \"ref\": {\n" +
        "            \"@ref\": \"indexes/spells_by_name\"\n" +
        "        },\n" +
        "        \"source\": {\n" +
        "            \"@ref\": \"classes/spells\"\n" +
        "        },\n" +
        "        \"terms\": [\n" +
        "            {\n" +
        "                \"path\": \"data.name\"\n" +
        "            }\n" +
        "        ],\n" +
        "        \"ts\": 1434345216167501,\n" +
        "        \"unique\": true\n" +
        "    }";

    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Index idx = parsed.asIndex();
    assertThat(idx.active(), is(false));
    assertThat(idx.classRef(), is(Ref.create("indexes")));
    assertThat(idx.name(), is("spells_by_name"));
    assertThat(idx.ref(), is(Ref.create("indexes/spells_by_name")));
    assertThat(idx.source(), is(Ref.create("classes/spells")));
    assertThat(idx.terms(), is(ImmutableList.of(ImmutableMap.of("path", "data.name"))));
    assertThat(idx.ts(), is(1434345216167501L));
    assertThat(idx.unique(), is(true));
  }

  @Test
  public void deserializeEventResponse() throws IOException {
    String toDeserialize = "{\n" +
        "\t\t\t\"ts\": 1434477366352519,\n" +
        "\t\t\t\"action\": \"create\",\n" +
        "\t\t\t\"resource\": {\n" +
        "\t\t\t\t\"@ref\": \"classes/spells/102989579003363328\"\n" +
        "\t\t\t}\n" +
        "\t\t}";

    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Event event = parsed.asEvent();

    assertThat(event.resource(), is(Ref.create("classes/spells/102989579003363328")));
    assertThat(event.action(), is("create"));
    assertThat(event.ts(), is(1434477366352519L));
  }

  @Test
  public void deserializePageResponseWithNoBeforeOrAfter() throws IOException {
    String toDeserialize = "{\n" +
        "        \"data\": [\n" +
        "            {\n" +
        "                \"@ref\": \"classes/spells/102851646450565120\"\n" +
        "            }\n" +
        "        ]\n" +
        "    }\n";

    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Page page = parsed.asPage();
    assertThat(page.data().size(), is(1));
    assertThat(page.data().get(0).asRef(), is(Ref.create("classes/spells/102851646450565120")));
    assertThat(page.after(), is(Optional.<Ref>absent()));
    assertThat(page.before(), is(Optional.<Ref>absent()));
  }

  @Test
  public void deserializePageResponseWithBefore() throws IOException {
    String toDeserialize = "{\n" +
        "        \"before\": {\n" +
        "            \"@ref\": \"classes/spells/102851646450565120\"\n" +
        "        },\n" +
        "        \"data\": [\n" +
        "            {\n" +
        "                \"@ref\": \"classes/spells/102851646450565120\"\n" +
        "            }\n" +
        "        ]\n" +
        "    }";

    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Page page = parsed.asPage();
    assertThat(page.data().size(), is(1));
    assertThat(page.data().get(0).asRef(), is(Ref.create("classes/spells/102851646450565120")));
    assertThat(page.before(), is(Optional.of(Ref.create("classes/spells/102851646450565120"))));
    assertThat(page.after(), is(Optional.<Ref>absent()));
  }

  @Test
  public void deserializePageResponseWithBeforeAndAfter() throws IOException {
    String toDeserialize = "{\n" +
        "        \"after\": {\n" +
        "            \"@ref\": \"classes/spells/102852248441192448\"\n" +
        "        },\n" +
        "        \"before\": {\n" +
        "            \"@ref\": \"classes/spells/102851646450565120\"\n" +
        "        },\n" +
        "        \"data\": [\n" +
        "            {\n" +
        "                \"@ref\": \"classes/spells/102851646450565120\"\n" +
        "            }\n" +
        "        ]\n" +
        "    }";

    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Page page = parsed.asPage();
    assertThat(page.data().size(), is(1));
    assertThat(page.data().get(0).asRef(), is(Ref.create("classes/spells/102851646450565120")));
    assertThat(page.after(), is(Optional.of(Ref.create("classes/spells/102852248441192448"))));
    assertThat(page.before(), is(Optional.of(Ref.create("classes/spells/102851646450565120"))));
  }

  @Test
  public void deserializePageResponseWithAfter() throws IOException {
    String toDeserialize = "{\n" +
        "        \"after\": {\n" +
        "            \"@ref\": \"classes/spells/102851646450565120\"\n" +
        "        },\n" +
        "        \"data\": [\n" +
        "            {\n" +
        "                \"@ref\": \"classes/spells/102851640310104064\"\n" +
        "            }\n" +
        "        ]\n" +
        "    }";

    ResponseNode parsed = json.readValue(toDeserialize, ResponseNode.class);
    Page page = parsed.asPage();
    assertThat(page.data().size(), is(1));
    assertThat(page.data().get(0).asRef(), is(Ref.create("classes/spells/102851640310104064")));
    assertThat(page.before(), is(Optional.<Ref>absent()));
    assertThat(page.after(), is(Optional.of(Ref.create("classes/spells/102851646450565120"))));
  }
}
