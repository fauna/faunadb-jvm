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

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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

    Value select = Select(Path(Path.Object("favorites"), Path.Object("foods"), Path.Array(1)),
      Quote(ObjectV("favorites", ObjectV("foods", ArrayV(StringV("crunchings"), StringV("munchings"), StringV("lunchings"))))));

    assertThat(json.writeValueAsString(select), is("{\"select\":[\"favorites\",\"foods\",1],\"from\":{\"quote\":{\"favorites\":{\"foods\":[\"crunchings\",\"munchings\",\"lunchings\"]}}}}"));

    Value quote = Quote(ObjectV("name", StringV("Hen Wen"), "Age", Add(LongV(100), LongV(10))));
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

    Value filter = Filter(Lambda("i", Equals(LongV(1L), Var("i"))), ArrayV(LongV(1L), LongV(2L), LongV(3L)));
    assertThat(json.writeValueAsString(filter), is("{\"filter\":{\"lambda\":\"i\",\"expr\":{\"equals\":[1,{\"var\":\"i\"}]}},\"collection\":[1,2,3]}"));

    Value take = Take(LongV(2L), ArrayV(LongV(1L),LongV(2L),LongV(3L)));
    assertThat(json.writeValueAsString(take), is("{\"take\":2,\"collection\":[1,2,3]}"));

    Value drop = Drop(LongV(2L), ArrayV(LongV(1L), LongV(2L), LongV(3L)));
    assertThat(json.writeValueAsString(drop), is("{\"drop\":2,\"collection\":[1,2,3]}"));

    Value prepend = Prepend(ArrayV(LongV(1L), LongV(2L), LongV(3L)), ArrayV(LongV(4L), LongV(5L), LongV(6L)));
    assertThat(json.writeValueAsString(prepend), is("{\"prepend\":[1,2,3],\"collection\":[4,5,6]}"));

    Value append = Append(ArrayV(LongV(4L), LongV(5L), LongV(6L)), ArrayV(LongV(1L), LongV(2L), LongV(3L)));
    assertThat(json.writeValueAsString(append), is("{\"append\":[4,5,6],\"collection\":[1,2,3]}"));
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

    Value insert = Insert(Ref("classes/spells/123456"), 1L, Action.CREATE, Quote(ObjectV("data", ObjectV("name", StringV("Mountain's Thunder"), "cost", LongV(10), "element", ArrayV(StringV("air"), StringV("earth"))))));
    assertThat(json.writeValueAsString(insert), is("{\"insert\":{\"@ref\":\"classes/spells/123456\"},\"ts\":1,\"action\":\"create\",\"params\":{\"quote\":{\"data\":{\"name\":\"Mountain's Thunder\",\"cost\":10,\"element\":[\"air\",\"earth\"]}}}}"));

    Value remove = Remove(Ref("classes/spells/123456"), 1L, Action.DELETE);
    assertThat(json.writeValueAsString(remove), is("{\"remove\":{\"@ref\":\"classes/spells/123456\"},\"ts\":1,\"action\":\"delete\"}"));
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

  @Test
  public void serializeAuthentication() throws JsonProcessingException {
    Value login = Login(Ref("classes/characters/104979509695139637"), Quote(ObjectV("password", StringV("abracadabra"))));
    assertThat(json.writeValueAsString(login), is("{\"login\":{\"@ref\":\"classes/characters/104979509695139637\"},\"params\":{\"quote\":{\"password\":\"abracadabra\"}}}"));

    Value logout = Logout(true);
    assertThat(json.writeValueAsString(logout), is("{\"logout\":true}"));

    Value identify = Identify(Ref("classes/characters/104979509695139637"), StringV("abracadabra"));
    assertThat(json.writeValueAsString(identify), is("{\"identify\":{\"@ref\":\"classes/characters/104979509695139637\"},\"password\":\"abracadabra\"}"));
  }

  @Test
  public void serializeTsAndDateValues() throws JsonProcessingException {
    Value ts = TsV(Instant.EPOCH.plus(5, ChronoUnit.MINUTES));
    assertThat(json.writeValueAsString(ts), is("{\"@ts\":\"1970-01-01T00:05:00Z\"}"));

    Value date = DateV(LocalDate.ofEpochDay(2));
    assertThat(json.writeValueAsString(date), is("{\"@date\":\"1970-01-03\"}"));
  }

  @Test
  public void serializeDateAndTime() throws JsonProcessingException {
    Value time = Time(StringV("1970-01-01T00:00:00+00:00"));
    assertThat(json.writeValueAsString(time), is("{\"time\":\"1970-01-01T00:00:00+00:00\"}"));

    Value epoch = Epoch(LongV(10L), TimeUnit.SECOND);
    assertThat(json.writeValueAsString(epoch), is("{\"epoch\":10,\"unit\":\"second\"}"));

    Value epoch2 = Epoch(LongV(10L), "millisecond");
    assertThat(json.writeValueAsString(epoch2), is("{\"epoch\":10,\"unit\":\"millisecond\"}"));

    Value date = Date(StringV("1970-01-02"));
    assertThat(json.writeValueAsString(date), is("{\"date\":\"1970-01-02\"}"));
  }

  @Test
  public void serializeMiscAndMath() throws JsonProcessingException {
    Value equals = Equals(StringV("fire"), StringV("fire"));
    assertThat(json.writeValueAsString(equals), is("{\"equals\":[\"fire\",\"fire\"]}"));

    Value concat = Concat(StringV("Hen"), StringV("Wen"));
    assertThat(json.writeValueAsString(concat), is("{\"concat\":[\"Hen\",\"Wen\"]}"));

    Value concat2 = Concat(ImmutableList.<Value>of(StringV("Hen"), StringV("Wen")), " ");
    assertThat(json.writeValueAsString(concat2), is("{\"concat\":[\"Hen\",\"Wen\"],\"separator\":\" \"}"));

    Value add = Add(LongV(1L), LongV(2L));
    assertThat(json.writeValueAsString(add), is("{\"add\":[1,2]}"));

    Value multiply = Multiply(LongV(1L), LongV(2L));
    assertThat(json.writeValueAsString(multiply), is("{\"multiply\":[1,2]}"));

    Value subtract = Subtract(LongV(1L), LongV(2L));
    assertThat(json.writeValueAsString(subtract), is("{\"subtract\":[1,2]}"));

    Value divide = Divide(LongV(1L), LongV(2L));
    assertThat(json.writeValueAsString(divide), is("{\"divide\":[1,2]}"));

    Value modulo = Modulo(LongV(1L), LongV(2L));
    assertThat(json.writeValueAsString(modulo), is("{\"modulo\":[1,2]}"));

    Value and = And(BooleanV(true), BooleanV(false));
    assertThat(json.writeValueAsString(and), is("{\"and\":[true,false]}"));

    Value or = Or(BooleanV(true), BooleanV(false));
    assertThat(json.writeValueAsString(or), is("{\"or\":[true,false]}"));

    Value not = Not(BooleanV(false));
    assertThat(json.writeValueAsString(not), is("{\"not\":false}"));
  }

}
