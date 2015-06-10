package com.faunadb.client.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.java.query.*;
import com.faunadb.client.java.query.Cursor.*;
import com.faunadb.client.java.query.Value.*;
import com.faunadb.client.java.types.Ref;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class SerializationSpec {
  ObjectMapper json = new ObjectMapper();

  @Test
  public void serializeRef() throws JsonProcessingException {
    Ref ref = Ref.create("some/ref");
    assertEquals(json.writeValueAsString(ref), "{\"@ref\":\"some/ref\"}");
  }

  @Test
  public void serializeGetAndPaginate() throws JsonProcessingException {
    Ref ref = Ref.create("some/ref");
    Get get = Get.create(RefV.create(ref));

    assertEquals("{\"get\":{\"@ref\":\"some/ref\"}}", json.writeValueAsString(get));

    Paginate get2 = Paginate.create(RefV.create(ref)).withCursor(Before.create(Ref.create("another/ref")));
    assertEquals("{\"paginate\":{\"@ref\":\"some/ref\"},\"before\":{\"@ref\":\"another/ref\"}}", json.writeValueAsString(get2));

    Paginate get3 = Paginate.create(RefV.create(ref)).withTs(1234L).withCursor(After.create(Ref.create("another/ref"))).withSize(1000L);
    assertEquals("{\"paginate\":{\"@ref\":\"some/ref\"},\"ts\":1234,\"after\":{\"@ref\":\"another/ref\"},\"size\":1000}", json.writeValueAsString(get3));
  }

  @Test
  public void serializeMatch() throws JsonProcessingException {
    Match m = Match.create(StringV.create("testTerm"), Ref.create("some/index"));
    assertEquals("{\"index\":{\"@ref\":\"some/index\"},\"match\":\"testTerm\"}", json.writeValueAsString(m));
  }

  @Test
  public void serializeComplexSet() throws JsonProcessingException {
    Match setTerm1 = Match.create(StringV.create("testTerm1"), Ref.create("some/index"));
    Match setTerm2 = Match.create(StringV.create("testTerm2"), Ref.create("another/index"));

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
    Events events = Events.create(RefV.create(ref));

    assertEquals("{\"events\":{\"@ref\":\"some/ref\"}}", json.writeValueAsString(events));

    Events events2 = events.withCursor(Before.create(Ref.create("another/ref")));
    assertEquals("{\"events\":{\"@ref\":\"some/ref\"},\"before\":{\"@ref\":\"another/ref\"}}", json.writeValueAsString(events2));

    Events events3 = events2.withSize(50L);
    assertEquals("{\"events\":{\"@ref\":\"some/ref\"},\"before\":{\"@ref\":\"another/ref\"},\"size\":50}", json.writeValueAsString(events3));
  }

  @Test
  public void serializeObject() throws JsonProcessingException {
    ObjectV obj = ObjectV.create(ImmutableMap.of("test1", StringV.create("value1"),
      "test2", NumberV.create(2),
      "test3", BooleanV.create(true)));

    assertEquals("{\"test1\":\"value1\",\"test2\":2,\"test3\":true}", json.writeValueAsString(obj));

    ObjectV nestedObj = ObjectV.create(ImmutableMap.<String, Value>of(
      "test1", ObjectV.create(ImmutableMap.<String, Value>of("nested1", StringV.create("nestedValue1")))
    ));

    assertEquals("{\"test1\":{\"nested1\":\"nestedValue1\"}}", json.writeValueAsString(nestedObj));
  }

  @Test
  public void serializeResourceOperation() throws JsonProcessingException {
    Ref ref = Ref.create("some/ref");
    ObjectV params =  ObjectV.create(ImmutableMap.<String, Value>of("test1", StringV.create("value2")));
    Create create = Create.create(RefV.create(ref), params);
    assertEquals("{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"test1\":\"value2\"}}", json.writeValueAsString(create));

    Replace replace = Replace.create(RefV.create(ref), params);
    assertEquals("{\"replace\":{\"@ref\":\"some/ref\"},\"params\":{\"test1\":\"value2\"}}", json.writeValueAsString(replace));

    Update update = Update.create(RefV.create(ref), params);
    assertEquals("{\"update\":{\"@ref\":\"some/ref\"},\"params\":{\"test1\":\"value2\"}}", json.writeValueAsString(update));

    Delete delete = Delete.create(RefV.create(ref));
    assertEquals("{\"delete\":{\"@ref\":\"some/ref\"}}", json.writeValueAsString(delete));
  }

  @Test
  public void serializeComplexExpression() throws JsonProcessingException {
    Ref ref = Ref.create("some/ref");
    Create expr1 = Create.create(RefV.create(ref), ObjectV.empty());
    Create expr2 = Create.create(RefV.create(ref), ObjectV.empty());

    Do complex = Do.create(ImmutableList.<Expression>of(expr1, expr2));
    System.out.println(json.writeValueAsString(complex));
  }
}
