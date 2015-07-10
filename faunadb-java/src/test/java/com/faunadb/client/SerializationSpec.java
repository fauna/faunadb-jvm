package com.faunadb.client;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.query.*;
import com.faunadb.client.types.Value.*;
import com.faunadb.client.types.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static com.faunadb.client.query.Language.*;

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
    assertThat(json.writeValueAsString(LongV(1234)), is("1234"));
    assertThat(json.writeValueAsString(LongV(Long.MAX_VALUE)), is(Long.valueOf(Long.MAX_VALUE).toString()));

    assertThat(json.writeValueAsString(DoubleV(1.234)), is("1.234"));

    assertThat(json.writeValueAsString(NullV.Null), is("null"));
  }

  @Test
  public void serializeComplexValues() throws JsonProcessingException {
    ArrayV value1 = ArrayV(LongV(1), StringV("test"));
    assertThat(json.writeValueAsString(value1), is("[1,\"test\"]"));
    ArrayV value2 = ArrayV(ArrayV(ObjectV("test", StringV("value")), LongV(2323), BooleanV.True), StringV("hi"), ObjectV("test", StringV("yo"), "test2", NullV.Null));
    assertThat(json.writeValueAsString(value2), is("[[{\"test\":\"value\"},2323,true],\"hi\",{\"test\":\"yo\",\"test2\":null}]"));
    ObjectV obj1 = ObjectV("test", LongV(1), "test2", Ref("some/ref"));
    assertThat(json.writeValueAsString(obj1), is("{\"test\":1,\"test2\":{\"@ref\":\"some/ref\"}}"));
  }

  @Test
  public void serializeBasicForms() throws JsonProcessingException {
    Value letAndVar = Let(ImmutableMap.<String, Value>of("x", LongV(1), "y", StringV("2")), Var("x"));
    assertThat(json.writeValueAsString(letAndVar), is("{\"let\":{\"x\":1,\"y\":\"2\"},\"in\":{\"var\":\"x\"}}"));

    Value ifForm = If(BooleanV.True, StringV("was true"), StringV("was false"));
    assertThat(json.writeValueAsString(ifForm), is("{\"if\":true,\"then\":\"was true\",\"else\":\"was false\"}"));

    Value doForm = Do(
      Create(Ref("some/ref/1"), Quote(ObjectV("data", ObjectV("name", StringV("Hen Wen"))))),
      Get(Ref("some/ref/1")));
    assertThat(json.writeValueAsString(doForm), is("{\"do\":[{\"create\":{\"@ref\":\"some/ref/1\"},\"params\":{\"quote\":{\"data\":{\"name\":\"Hen Wen\"}}}},{\"get\":{\"@ref\":\"some/ref/1\"}}]}"));

    Value select = Select(ImmutableList.of(Path.Object("favorites"), Path.Object("foods"), Path.Array(1)),
      Quote(ObjectV("favorites", ObjectV("foods", ArrayV(StringV("crunchings"), StringV("munchings"), StringV("lunchings"))))));

    assertThat(json.writeValueAsString(select), is("{\"select\":[\"favorites\",\"foods\",1],\"from\":{\"quote\":{\"favorites\":{\"foods\":[\"crunchings\",\"munchings\",\"lunchings\"]}}}}"));

    Value quote = Quote(ObjectV("name", StringV("Hen Wen"), "Age", Add(LongV(100), LongV(10))));
    System.out.println(json.writeValueAsString(quote));
  }

  @Test
  public void serializeRefAndSet() throws JsonProcessingException {
    Ref ref = Ref("some/ref");
    assertEquals(json.writeValueAsString(ref), "{\"@ref\":\"some/ref\"}");
  }

  @Test
  public void serializeCollections() throws JsonProcessingException {
    Value map = Map(Lambda("munchings", Var("munchings")), ArrayV(LongV(1), LongV(2), LongV(3)));
    assertEquals(json.writeValueAsString(map), "{\"map\":{\"lambda\":\"munchings\",\"expr\":{\"var\":\"munchings\"}},\"collection\":[1,2,3]}");

    Value foreach = Foreach(Lambda("creature", Create(Ref("some/ref"), Object(ObjectV("data", Object(ObjectV("some", Var("creature"))))))), ArrayV(Ref("another/ref/1"), Ref("another/ref/2")));
    assertEquals(json.writeValueAsString(foreach), "{\"foreach\":{\"lambda\":\"creature\",\"expr\":{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"data\":{\"object\":{\"some\":{\"var\":\"creature\"}}}}}}},\"collection\":[{\"@ref\":\"another/ref/1\"},{\"@ref\":\"another/ref/2\"}]}");
  }

  @Test
  public void serializeResourceRetrieval() throws JsonProcessingException {
    Ref ref = Ref("some/ref/1");
    Value get = Get(ref);

    assertThat(json.writeValueAsString(get), is("{\"get\":{\"@ref\":\"some/ref/1\"}}"));

    Value paginate1 = Paginate(Union(
      Match(StringV("term"), Ref("indexes/some_index")),
      Match(StringV("term2"), Ref("indexes/some_index")))).build();

    assertThat(json.writeValueAsString(paginate1), is("{\"paginate\":{\"union\":[{\"match\":\"term\",\"index\":{\"@ref\":\"indexes/some_index\"}},{\"match\":\"term2\",\"index\":{\"@ref\":\"indexes/some_index\"}}]}}"));

    Value paginate2 = Paginate(Union(
      Match(StringV("term"), Ref("indexes/some_index")),
      Match(StringV("term2"), Ref("indexes/some_index")))).withSources(true).build();

    assertThat(json.writeValueAsString(paginate2), is("{\"paginate\":{\"union\":[{\"match\":\"term\",\"index\":{\"@ref\":\"indexes/some_index\"}},{\"match\":\"term2\",\"index\":{\"@ref\":\"indexes/some_index\"}}]},\"sources\":true}"));

    Value paginate3 = Paginate(Union(
      Match(StringV("term"), Ref("indexes/some_index")),
      Match(StringV("term2"), Ref("indexes/some_index")))).withEvents(true).build();

    assertThat(json.writeValueAsString(paginate3), is("{\"paginate\":{\"union\":[{\"match\":\"term\",\"index\":{\"@ref\":\"indexes/some_index\"}},{\"match\":\"term2\",\"index\":{\"@ref\":\"indexes/some_index\"}}]},\"events\":true}"));

    Value paginate4 = Paginate(Union(
      Match(StringV("term"), Ref("indexes/some_index")),
      Match(StringV("term2"), Ref("indexes/some_index"))))
      .withCursor(Before(Ref("some/ref/1")))
      .withSize(4).build();

    assertThat(json.writeValueAsString(paginate4), is("{\"paginate\":{\"union\":[{\"match\":\"term\",\"index\":{\"@ref\":\"indexes/some_index\"}},{\"match\":\"term2\",\"index\":{\"@ref\":\"indexes/some_index\"}}]},\"before\":{\"@ref\":\"some/ref/1\"},\"size\":4}"));

    Value count = Count(Match(StringV("fire"), Ref("indexes/spells_by_element")));
    assertThat(json.writeValueAsString(count), is("{\"count\":{\"match\":\"fire\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}}}"));
  }

  @Test
  public void serializeResourceModification() throws JsonProcessingException {
    Ref ref = Ref("classes/spells");
    ObjectV params = ObjectV("name", StringV("Mountainous Thunder"), "element", StringV("air"), "cost", LongV(15));
    Value create = Create(ref, Quote(ObjectV("data", params)));
    assertThat(json.writeValueAsString(create), is("{\"create\":{\"@ref\":\"classes/spells\"},\"params\":{\"quote\":{\"data\":{\"name\":\"Mountainous Thunder\",\"element\":\"air\",\"cost\":15}}}}"));

    Value update = Update(Ref("classes/spells/123456"), Quote(ObjectV("data", ObjectV("name", StringV("Mountain's Thunder"), "cost", NullV.Null))));
    assertThat(json.writeValueAsString(update), is("{\"update\":{\"@ref\":\"classes/spells/123456\"},\"params\":{\"quote\":{\"data\":{\"name\":\"Mountain's Thunder\",\"cost\":null}}}}"));

    Value replace = Replace(Ref("classes/spells/123456"), Quote(ObjectV("data", ObjectV("name", StringV("Mountain's Thunder"), "element", ArrayV(StringV("air"), StringV("earth")), "cost", LongV(10)))));
    assertThat(json.writeValueAsString(replace), is("{\"replace\":{\"@ref\":\"classes/spells/123456\"},\"params\":{\"quote\":{\"data\":{\"name\":\"Mountain's Thunder\",\"element\":[\"air\",\"earth\"],\"cost\":10}}}}"));

    Value delete = Delete(Ref("classes/spells/123456"));
    assertThat(json.writeValueAsString(delete), is("{\"delete\":{\"@ref\":\"classes/spells/123456\"}}"));
  }

  @Test
  public void serializeSets() throws JsonProcessingException {
    Value match = Match(StringV("fire"), Ref("indexes/spells_by_elements"));
    assertThat(json.writeValueAsString(match), is("{\"match\":\"fire\",\"index\":{\"@ref\":\"indexes/spells_by_elements\"}}"));

    Value union = Union(
      Match(StringV("fire"), Ref("indexes/spells_by_element")),
      Match(StringV("water"), Ref("indexes/spells_by_element"))
    );

    assertThat(json.writeValueAsString(union), is("{\"union\":[{\"match\":\"fire\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}},{\"match\":\"water\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}}]}"));

    Value intersection = Intersection(
      Match(StringV("fire"), Ref("indexes/spells_by_element")),
      Match(StringV("water"), Ref("indexes/spells_by_element"))
    );

    assertThat(json.writeValueAsString(intersection), is("{\"intersection\":[{\"match\":\"fire\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}},{\"match\":\"water\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}}]}"));

    Value difference = Difference(
      Match(StringV("fire"), Ref("indexes/spells_by_element")),
      Match(StringV("water"), Ref("indexes/spells_by_element"))
    );

    assertThat(json.writeValueAsString(difference), is("{\"difference\":[{\"match\":\"fire\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}},{\"match\":\"water\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}}]}"));

    Value join = Join(Match(StringV("fire"), Ref("indexes/spells_by_element")),
      Lambda("spell", Get(Var("spell"))));

    assertThat(json.writeValueAsString(join), is("{\"join\":{\"match\":\"fire\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}},\"with\":{\"lambda\":\"spell\",\"expr\":{\"get\":{\"var\":\"spell\"}}}}"));
  }
}
