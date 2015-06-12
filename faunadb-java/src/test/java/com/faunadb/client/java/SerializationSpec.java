package com.faunadb.client.java;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.java.query.*;
import com.faunadb.client.java.query.Cursor.*;
import com.faunadb.client.java.query.Set;
import com.faunadb.client.java.query.Value.*;
import com.faunadb.client.java.types.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static com.faunadb.client.java.query.Value.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class SerializationSpec {
  ObjectMapper json = new ObjectMapper();

  @Test
  public void serializeLiteralValues() throws JsonProcessingException {
    assertThat(json.writeValueAsString(BooleanV.True), is("true"));
    assertThat(json.writeValueAsString(BooleanV.False), is("false"));

    assertThat(json.writeValueAsString(StringV("test")), is("\"test\""));
    assertThat(json.writeValueAsString(NumberV(1234)), is("1234"));
    assertThat(json.writeValueAsString(NumberV(Long.MAX_VALUE)), is(Long.valueOf(Long.MAX_VALUE).toString()));

    assertThat(json.writeValueAsString(DoubleV(1.234)), is("1.234"));

    assertThat(json.writeValueAsString(NullV.Null), is("null"));
  }

  @Test
  public void serializeComplexValues() throws JsonProcessingException {
    ArrayV value1 = ArrayV(NumberV(1), StringV("test"));
    assertThat(json.writeValueAsString(value1), is("[1,\"test\"]"));
    ArrayV value2 = ArrayV(ArrayV(ObjectV("test", StringV("value")), NumberV(2323), BooleanV.True), StringV("hi"), ObjectV("test", StringV("yo"), "test2", NullV.Null));
    assertThat(json.writeValueAsString(value2), is("[[{\"object\":{\"test\":\"value\"}},2323,true],\"hi\",{\"object\":{\"test\":\"yo\",\"test2\":null}}]"));
    ObjectV obj1 = ObjectV("test", NumberV(1), "test2", RefV.create("some/ref"));
    assertThat(json.writeValueAsString(obj1), is("{\"object\":{\"test\":1,\"test2\":{\"@ref\":\"some/ref\"}}}"));
  }

  @Test
  public void serializeBasicForms() throws JsonProcessingException {
    Let letAndVar = Let.create(ImmutableMap.<String, Expression>of("x", NumberV(1), "y", StringV("2")), Var.create("x"));
    assertThat(json.writeValueAsString(letAndVar), is("{\"let\":{\"x\":1,\"y\":\"2\"},\"in\":{\"var\":\"x\"}}"));

    If ifForm = If.create(BooleanV.True, StringV("was true"), StringV("was false"));
    assertThat(json.writeValueAsString(ifForm), is("{\"if\":true,\"then\":\"was true\",\"else\":\"was false\"}"));

    Do doForm = Do.create(ImmutableList.<Expression>of(
        Create.create(RefV("some/ref/1"), ObjectV("data", ObjectV("name", StringV("Hen Wen")))),
        Get.create(RefV("some/ref/1"))));
    assertThat(json.writeValueAsString(doForm), is("{\"do\":[{\"create\":{\"@ref\":\"some/ref/1\"},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Hen Wen\"}}}}},{\"get\":{\"@ref\":\"some/ref/1\"}}]}"));

    Select select = Select.create(ImmutableList.of(Path.Object("favorites"), Path.Object("foods"), Path.Array(1)),
      ObjectV("favorites", ObjectV("foods", ArrayV(StringV("crunchings"), StringV("munchings"), StringV("lunchings")))));

    assertThat(json.writeValueAsString(select), is("{\"select\":[\"favorites\",\"foods\",1],\"from\":{\"object\":{\"favorites\":{\"object\":{\"foods\":[\"crunchings\",\"munchings\",\"lunchings\"]}}}}}"));
  }

  @Test
  public void serializeRefAndSet() throws JsonProcessingException {
    Ref ref = Ref.create("some/ref");
    assertEquals(json.writeValueAsString(ref), "{\"@ref\":\"some/ref\"}");
  }

  @Test
  public void serializeCollections() throws JsonProcessingException {
    Map map = Map.create(Lambda.create("munchings", Var.create("munchings")), ArrayV(NumberV(1), NumberV(2), NumberV(3)));
    assertEquals(json.writeValueAsString(map), "{\"map\":{\"lambda\":\"munchings\",\"expr\":{\"var\":\"munchings\"}},\"collection\":[1,2,3]}");

    Foreach foreach = Foreach.create(Lambda.create("creature", Create.create(RefV.create("some/ref"), ObjectV("data", ObjectV("some", VarV("creature"))))), ArrayV(RefV("another/ref/1"), RefV("another/ref/2")));
    assertEquals(json.writeValueAsString(foreach), "{\"foreach\":{\"lambda\":\"creature\",\"expr\":{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"data\":{\"object\":{\"some\":{\"var\":\"creature\"}}}}}}},\"collection\":[{\"@ref\":\"another/ref/1\"},{\"@ref\":\"another/ref/2\"}]}");
  }

  @Test
  public void serializeResourceRetrieval() throws JsonProcessingException {
    Ref ref = Ref.create("some/ref/1");
    Get get = Get.create(RefV(ref));

    assertThat(json.writeValueAsString(get), is("{\"get\":{\"@ref\":\"some/ref/1\"}}"));

    Paginate paginate1 = Paginate.create(Union.create(ImmutableList.<Set>of(
      Match.create(StringV("term"), Ref.create("indexes/some_index")),
      Match.create(StringV("term2"), Ref.create("indexes/some_index")))));

    assertThat(json.writeValueAsString(paginate1), is("{\"paginate\":{\"union\":[{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term\"},{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term2\"}]}}"));

    Paginate paginate2 = Paginate.create(Union.create(ImmutableList.<Set>of(
      Match.create(StringV("term"), Ref.create("indexes/some_index")),
      Match.create(StringV("term2"), Ref.create("indexes/some_index"))))).withSources(true);

    assertThat(json.writeValueAsString(paginate2), is("{\"paginate\":{\"union\":[{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term\"},{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term2\"}]},\"sources\":true}"));

    Paginate paginate3 = Paginate.create(Union.create(ImmutableList.<Set>of(
      Match.create(StringV("term"), Ref.create("indexes/some_index")),
      Match.create(StringV("term2"), Ref.create("indexes/some_index"))))).withEvents(true);

    assertThat(json.writeValueAsString(paginate3), is("{\"paginate\":{\"union\":[{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term\"},{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term2\"}]},\"events\":true}"));

    Paginate paginate4 = Paginate.create(Union.create(ImmutableList.<Set>of(
      Match.create(StringV("term"), Ref.create("indexes/some_index")),
      Match.create(StringV("term2"), Ref.create("indexes/some_index")))))
      .withCursor(Before.create(Ref.create("some/ref/1")))
      .withSize(4);

    assertThat(json.writeValueAsString(paginate4), is("{\"paginate\":{\"union\":[{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term\"},{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term2\"}]},\"before\":{\"@ref\":\"some/ref/1\"},\"size\":4}"));
  }

  @Test
  public void serializeMatch() throws JsonProcessingException {
    Match m = Match.create(StringV("testTerm"), Ref.create("some/index"));
    assertEquals("{\"index\":{\"@ref\":\"some/index\"},\"match\":\"testTerm\"}", json.writeValueAsString(m));
  }

  @Test
  public void serializeComplexSet() throws JsonProcessingException {
    Match setTerm1 = Match.create(StringV("testTerm1"), Ref.create("some/index"));
    Match setTerm2 = Match.create(StringV("testTerm2"), Ref.create("another/index"));

    Union union = Union.create(ImmutableList.<Set>of(setTerm1, setTerm2));
    assertEquals("{\"union\":[{\"index\":{\"@ref\":\"some/index\"},\"match\":\"testTerm1\"},{\"index\":{\"@ref\":\"another/index\"},\"match\":\"testTerm2\"}]}", json.writeValueAsString(union));

    Intersection intersection = Intersection.create(ImmutableList.<Set>of(setTerm1, setTerm2));
    assertEquals("{\"intersection\":[{\"index\":{\"@ref\":\"some/index\"},\"match\":\"testTerm1\"},{\"index\":{\"@ref\":\"another/index\"},\"match\":\"testTerm2\"}]}", json.writeValueAsString(intersection));

    Difference difference = Difference.create(ImmutableList.<Set>of(setTerm1, setTerm2));
    assertEquals("{\"difference\":[{\"index\":{\"@ref\":\"some/index\"},\"match\":\"testTerm1\"},{\"index\":{\"@ref\":\"another/index\"},\"match\":\"testTerm2\"}]}", json.writeValueAsString(difference));

    Join join = Join.create(setTerm1, "some/target/_");
    assertEquals("{\"join\":{\"index\":{\"@ref\":\"some/index\"},\"match\":\"testTerm1\"},\"with\":\"some/target/_\"}", json.writeValueAsString(join));
  }

  @Test
  public void serializeEvents() throws JsonProcessingException {
    Ref ref = Ref.create("some/ref");
    Events events = Events.create(RefV(ref));

    assertEquals("{\"events\":{\"@ref\":\"some/ref\"}}", json.writeValueAsString(events));

    Events events2 = events.withCursor(Before.create(Ref.create("another/ref")));
    assertEquals("{\"events\":{\"@ref\":\"some/ref\"},\"before\":{\"@ref\":\"another/ref\"}}", json.writeValueAsString(events2));

    Events events3 = events2.withSize(50L);
    assertEquals("{\"events\":{\"@ref\":\"some/ref\"},\"before\":{\"@ref\":\"another/ref\"},\"size\":50}", json.writeValueAsString(events3));
  }

  @Test
  public void serializeResourceOperation() throws JsonProcessingException {
    Ref ref = Ref.create("some/ref");
    ObjectV params = ObjectV("test1", StringV("value2"));
    Create create = Create.create(RefV(ref), params);
    assertEquals("{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"test1\":\"value2\"}}}", json.writeValueAsString(create));

    Replace replace = Replace.create(RefV(ref), params);
    assertEquals("{\"replace\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"test1\":\"value2\"}}}", json.writeValueAsString(replace));

    Update update = Update.create(RefV(ref), params);
    assertEquals("{\"update\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"test1\":\"value2\"}}}", json.writeValueAsString(update));

    Delete delete = Delete.create(RefV(ref));
    assertEquals("{\"delete\":{\"@ref\":\"some/ref\"}}", json.writeValueAsString(delete));
  }
}
