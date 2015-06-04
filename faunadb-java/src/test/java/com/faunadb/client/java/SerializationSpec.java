package com.faunadb.client.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.java.query.Cursor.*;
import com.faunadb.client.java.query.Get;
import com.faunadb.client.java.query.Match;
import com.faunadb.client.java.query.Paginate;
import com.faunadb.client.java.query.Value.*;
import com.faunadb.client.java.types.Ref;
import static org.junit.Assert.*;
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
}
