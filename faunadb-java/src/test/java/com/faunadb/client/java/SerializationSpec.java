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
import static com.faunadb.client.java.query.Language.*;

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
    ObjectV obj1 = ObjectV("test", NumberV(1), "test2", RefV("some/ref"));
    assertThat(json.writeValueAsString(obj1), is("{\"object\":{\"test\":1,\"test2\":{\"@ref\":\"some/ref\"}}}"));
  }

  @Test
  public void serializeBasicForms() throws JsonProcessingException {
    Let letAndVar = Let(ImmutableMap.<String, Expression>of("x", NumberV(1), "y", StringV("2")), Var("x"));
    assertThat(json.writeValueAsString(letAndVar), is("{\"let\":{\"x\":1,\"y\":\"2\"},\"in\":{\"var\":\"x\"}}"));

    If ifForm = If(BooleanV.True, StringV("was true"), StringV("was false"));
    assertThat(json.writeValueAsString(ifForm), is("{\"if\":true,\"then\":\"was true\",\"else\":\"was false\"}"));

    Do doForm = Do(ImmutableList.of(
      Create(RefV("some/ref/1"), ObjectV("data", ObjectV("name", StringV("Hen Wen")))),
      Get(RefV("some/ref/1"))));
    assertThat(json.writeValueAsString(doForm), is("{\"do\":[{\"create\":{\"@ref\":\"some/ref/1\"},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Hen Wen\"}}}}},{\"get\":{\"@ref\":\"some/ref/1\"}}]}"));

    Select select = Select(ImmutableList.of(Path.Object("favorites"), Path.Object("foods"), Path.Array(1)),
      ObjectV("favorites", ObjectV("foods", ArrayV(StringV("crunchings"), StringV("munchings"), StringV("lunchings")))));

    assertThat(json.writeValueAsString(select), is("{\"select\":[\"favorites\",\"foods\",1],\"from\":{\"object\":{\"favorites\":{\"object\":{\"foods\":[\"crunchings\",\"munchings\",\"lunchings\"]}}}}}"));
  }

  @Test
  public void serializeRefAndSet() throws JsonProcessingException {
    Ref ref = Ref("some/ref");
    assertEquals(json.writeValueAsString(ref), "{\"@ref\":\"some/ref\"}");
  }

  @Test
  public void serializeCollections() throws JsonProcessingException {
    Map map = Map(Lambda("munchings", Var("munchings")), ArrayV(NumberV(1), NumberV(2), NumberV(3)));
    assertEquals(json.writeValueAsString(map), "{\"map\":{\"lambda\":\"munchings\",\"expr\":{\"var\":\"munchings\"}},\"collection\":[1,2,3]}");

    Foreach foreach = Foreach(Lambda("creature", Create(RefV("some/ref"), ObjectV("data", ObjectV("some", VarV("creature"))))), ArrayV(RefV("another/ref/1"), RefV("another/ref/2")));
    assertEquals(json.writeValueAsString(foreach), "{\"foreach\":{\"lambda\":\"creature\",\"expr\":{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"data\":{\"object\":{\"some\":{\"var\":\"creature\"}}}}}}},\"collection\":[{\"@ref\":\"another/ref/1\"},{\"@ref\":\"another/ref/2\"}]}");
  }

  @Test
  public void serializeResourceRetrieval() throws JsonProcessingException {
    Ref ref = Ref("some/ref/1");
    Get get = Get(RefV(ref));

    assertThat(json.writeValueAsString(get), is("{\"get\":{\"@ref\":\"some/ref/1\"}}"));

    Paginate paginate1 = Paginate(Union(ImmutableList.<Set>of(
      Match(StringV("term"), Ref("indexes/some_index")),
      Match(StringV("term2"), Ref("indexes/some_index")))));

    assertThat(json.writeValueAsString(paginate1), is("{\"paginate\":{\"union\":[{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term\"},{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term2\"}]}}"));

    Paginate paginate2 = Paginate(Union(ImmutableList.<Set>of(
      Match(StringV("term"), Ref("indexes/some_index")),
      Match(StringV("term2"), Ref("indexes/some_index"))))).withSources(true);

    assertThat(json.writeValueAsString(paginate2), is("{\"paginate\":{\"union\":[{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term\"},{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term2\"}]},\"sources\":true}"));

    Paginate paginate3 = Paginate(Union(ImmutableList.<Set>of(
      Match(StringV("term"), Ref("indexes/some_index")),
      Match(StringV("term2"), Ref("indexes/some_index"))))).withEvents(true);

    assertThat(json.writeValueAsString(paginate3), is("{\"paginate\":{\"union\":[{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term\"},{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term2\"}]},\"events\":true}"));

    Paginate paginate4 = Paginate(Union(ImmutableList.<Set>of(
      Match(StringV("term"), Ref("indexes/some_index")),
      Match(StringV("term2"), Ref("indexes/some_index")))))
      .withCursor(Before(Ref("some/ref/1")))
      .withSize(4);

    assertThat(json.writeValueAsString(paginate4), is("{\"paginate\":{\"union\":[{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term\"},{\"index\":{\"@ref\":\"indexes/some_index\"},\"match\":\"term2\"}]},\"before\":{\"@ref\":\"some/ref/1\"},\"size\":4}"));

    Count count = Count(Match(StringV("fire"), Ref("indexes/spells_by_element")));
    assertThat(json.writeValueAsString(count), is("{\"count\":{\"index\":{\"@ref\":\"indexes/spells_by_element\"},\"match\":\"fire\"}}"));
  }

  @Test
  public void serializeResourceModification() throws JsonProcessingException {
    Ref ref = Ref("classes/spells");
    ObjectV params = ObjectV("name", StringV("Mountainous Thunder"), "element", StringV("air"), "cost", NumberV(15));
    Create create = Create(RefV(ref), ObjectV("data", params));
    assertThat(json.writeValueAsString(create), is("{\"create\":{\"@ref\":\"classes/spells\"},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Mountainous Thunder\",\"element\":\"air\",\"cost\":15}}}}}"));

    Update update = Update(RefV("classes/spells/123456"), ObjectV("data", ObjectV("name", StringV("Mountain's Thunder"), "cost", NullV.Null)));
    assertThat(json.writeValueAsString(update), is("{\"update\":{\"@ref\":\"classes/spells/123456\"},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Mountain's Thunder\",\"cost\":null}}}}}"));

    Replace replace = Replace(RefV("classes/spells/123456"), ObjectV("data", ObjectV("name", StringV("Mountain's Thunder"), "element", ArrayV(StringV("air"), StringV("earth")), "cost", NumberV(10))));
    assertThat(json.writeValueAsString(replace), is("{\"replace\":{\"@ref\":\"classes/spells/123456\"},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Mountain's Thunder\",\"element\":[\"air\",\"earth\"],\"cost\":10}}}}}"));

    Delete delete = Delete(RefV("classes/spells/123456"));
    assertThat(json.writeValueAsString(delete), is("{\"delete\":{\"@ref\":\"classes/spells/123456\"}}"));
  }

  @Test
  public void serializeSets() throws JsonProcessingException {
    Match match = Match(StringV("fire"), Ref("indexes/spells_by_elements"));
    assertThat(json.writeValueAsString(match), is("{\"index\":{\"@ref\":\"indexes/spells_by_elements\"},\"match\":\"fire\"}"));

    Union union = Union(ImmutableList.<Set>of(
      Match(StringV("fire"), Ref("indexes/spells_by_element")),
      Match(StringV("water"), Ref("indexes/spells_by_element"))
    ));

    assertThat(json.writeValueAsString(union), is("{\"union\":[{\"index\":{\"@ref\":\"indexes/spells_by_element\"},\"match\":\"fire\"},{\"index\":{\"@ref\":\"indexes/spells_by_element\"},\"match\":\"water\"}]}"));

    Intersection intersection = Intersection(ImmutableList.<Set>of(
      Match(StringV("fire"), Ref("indexes/spells_by_element")),
      Match(StringV("water"), Ref("indexes/spells_by_element"))
    ));

    assertThat(json.writeValueAsString(intersection), is("{\"intersection\":[{\"index\":{\"@ref\":\"indexes/spells_by_element\"},\"match\":\"fire\"},{\"index\":{\"@ref\":\"indexes/spells_by_element\"},\"match\":\"water\"}]}"));

    Difference difference = Difference(ImmutableList.<Set>of(
      Match(StringV("fire"), Ref("indexes/spells_by_element")),
      Match(StringV("water"), Ref("indexes/spells_by_element"))
    ));

    assertThat(json.writeValueAsString(difference), is("{\"difference\":[{\"index\":{\"@ref\":\"indexes/spells_by_element\"},\"match\":\"fire\"},{\"index\":{\"@ref\":\"indexes/spells_by_element\"},\"match\":\"water\"}]}"));

    Join join = Join(Match(StringV("fire"), Ref("indexes/spells_by_element")),
      Lambda("spell", Get(Var("spell"))));

    assertThat(json.writeValueAsString(join), is("{\"join\":{\"index\":{\"@ref\":\"indexes/spells_by_element\"},\"match\":\"fire\"},\"with\":{\"lambda\":\"spell\",\"expr\":{\"get\":{\"var\":\"spell\"}}}}"));
  }
}
