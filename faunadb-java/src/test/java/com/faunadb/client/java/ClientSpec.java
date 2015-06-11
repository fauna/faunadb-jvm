package com.faunadb.client.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.java.errors.NotFoundQueryException;
import com.faunadb.client.java.query.*;
import com.faunadb.client.java.query.Value.*;
import com.faunadb.client.java.response.*;
import com.faunadb.client.java.types.Ref;
import com.faunadb.httpclient.Connection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class ClientSpec {
  static ImmutableMap<String, String> config = readConfig("config/test.yml");
  ObjectMapper json = new ObjectMapper();
  static FaunaClient rootClient;
  static FaunaClient client;
  static String testDbName = RandomStringUtils.randomAlphanumeric(8);

  static ImmutableMap<String, String> readConfig(String filename) {
    try {
      System.out.println(new File(".").getCanonicalPath());
      FileInputStream reader = new FileInputStream(filename);
      ImmutableMap<String, String> rv = ImmutableMap.copyOf(new Yaml().loadAs(reader, Map.class));
      reader.close();
      return rv;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @BeforeClass
  public static void beforeAll() throws IOException, ExecutionException, InterruptedException {
    rootClient = FaunaClient.create(Connection.builder().withFaunaRoot(config.get("root_url")).withAuthToken(config.get("root_token")).build());
    ListenableFuture<ResponseNode> dbCreateF = rootClient.query(Create.create(RefV.create("databases"), ObjectV.create("name", StringV.create(testDbName))));
    ResponseNode dbCreateR = dbCreateF.get();
    Ref dbRef = dbCreateR.asDatabase().ref();

    ListenableFuture<ResponseNode> keyCreateF = rootClient.query(Create.create(RefV.create("keys"), ObjectV.create("database", RefV.create(dbRef), "role", StringV.create("server"))));
    ResponseNode keyCreateR = keyCreateF.get();
    Key key = keyCreateR.asKey();

    client = FaunaClient.create(Connection.builder().withFaunaRoot(config.get("root_url")).withAuthToken(key.secret()).build());

    ListenableFuture<ResponseNode> classCreateF = client.query(Create.create(RefV.create("classes"), ObjectV.create("name", StringV.create("spells"))));
    classCreateF.get();

    ListenableFuture<ResponseNode> indexCreateF = client.query(Create.create(RefV.create("indexes"), ObjectV.create(
      "name", StringV.create("spells_by_test"),
      "source", RefV.create("classes/spells"),
      "path", StringV.create("data.queryTest1"),
      "unique", BooleanV.create(false))));

    indexCreateF.get();

    ListenableFuture<ResponseNode> setIndexF = client.query(Create.create(RefV.create("indexes"), ObjectV.create(
      "name", StringV.create("spells_instances"),
      "source", RefV.create("classes/spells"),
      "path", StringV.create("class"),
      "unique", BooleanV.create(false)
    )));

    setIndexF.get();

    ListenableFuture<ResponseNode> uniqueIndexF = client.query(Create.create(RefV.create("indexes"), ObjectV.create(
      "name", StringV.create("spells_by_unique_test"),
      "source", RefV.create("classes/spells"),
      "path", StringV.create("data.uniqueTest1"),
      "unique", BooleanV.create(true)
    )));

    uniqueIndexF.get();
  }

  @Test(expected=NotFoundQueryException.class)
  public void testLookupMissingInstance() throws Throwable {
    ListenableFuture<ResponseNode> resp = client.query(Get.create(RefV.create("classes/spells/1234")));
    try {
      resp.get();
    } catch (ExecutionException ex) {
      throw ex.getCause();
    }
  }

  @Test
  public void testCreateNewInstance() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<ResponseNode> respF = client.query(Create.create(RefV.create("classes/spells"), ObjectV.create("data", ObjectV.create("testField", StringV.create("testValue")))));
    Instance resp = respF.get().asInstance();

    assertThat(resp.ref().value(), startsWith("classes/spells/"));
    assertThat(resp.classRef().value(), is("classes/spells"));
    assertThat(resp.data().get("testField").asString(), is("testValue"));

    ListenableFuture<ResponseNode> resp2F = client.query(Create.create(RefV.create("classes/spells"), ObjectV.create("data", ObjectV.create("testField", ObjectV.create("array", ArrayV.create(NumberV.create(1), StringV.create("2"), DoubleV.create(3.4)), "bool", BooleanV.create(true), "num", NumberV.create(1234), "string", StringV.create("sup"), "float", DoubleV.create(1.234))))));
    Instance resp2 = resp2F.get().asInstance();

    assertTrue(resp.data().containsKey("testField"));
    ResponseMap testFieldObj = resp2.data().get("testField").asObject();
    ImmutableList<ResponseNode> array = testFieldObj.get("array").asArray();
    assertThat(array.get(0).asNumber(), is(1L));
    assertThat(array.get(1).asString(), is("2"));
    assertThat(array.get(2).asDouble(), is(3.4));
    assertThat(testFieldObj.get("string").asString(), is("sup"));
    assertThat(testFieldObj.get("num").asNumber(), is(1234L));
    assertThat(testFieldObj.get("bool").asBoolean(), is(true));
  }

  @Test
  public void testIssueQuery() throws IOException, ExecutionException, InterruptedException {
    String randomText1 = RandomStringUtils.randomAlphanumeric(8);
    String randomText2 = RandomStringUtils.randomAlphanumeric(8);
    Ref classRef = Ref.create("classes/spells");
    ListenableFuture<ResponseNode> createF1 = client.query(Create.create(RefV.create(classRef), ObjectV.create("data", ObjectV.create("queryTest1", StringV.create(randomText1)))));
    ListenableFuture<ResponseNode> createF2 = client.query(Create.create(RefV.create(classRef), ObjectV.create("data", ObjectV.create("queryTest1", StringV.create(randomText2)))));

    Instance create1 = createF1.get().asInstance();
    Instance create2 = createF2.get().asInstance();

    ListenableFuture<ResponseNode> queryF1 = client.query(Paginate.create(Match.create(StringV.create(randomText1), Ref.create("indexes/spells_by_test"))));
    Page page1 = queryF1.get().asPage();
    assertThat(page1.data().size(), is(1));
    assertThat(page1.data().get(0).asRef(), is(create1.ref()));

    ListenableFuture<ResponseNode> queryF2 = client.query(Paginate.create(Match.create(RefV.create(classRef), Ref.create("indexes/spells_instances"))));
    Page page = queryF2.get().asPage();

    ImmutableList.Builder<Ref> refsBuilder = ImmutableList.builder();
    for (ResponseNode node : page.data()) {
      refsBuilder.add(node.asRef());
    }

    ImmutableList<Ref> refs = refsBuilder.build();

    assertTrue(refs.contains(create1.ref()));
    assertTrue(refs.contains(create2.ref()));
  }

  @Test
  public void testIssueBatchedQuery() throws IOException, ExecutionException, InterruptedException {
    String randomText1 = RandomStringUtils.randomAlphanumeric(8);
    String randomText2 = RandomStringUtils.randomAlphanumeric(8);

    Ref classRef = Ref.create("classes/spells");

    Expression expr1 = Create.create(RefV.create(classRef), ObjectV.create("data", ObjectV.create("queryTest1", StringV.create(randomText1))));
    Expression expr2 = Create.create(RefV.create(classRef), ObjectV.create("data", ObjectV.create("queryTest1", StringV.create(randomText2))));

    ListenableFuture<ImmutableList<ResponseNode>> createFuture = client.query(ImmutableList.of(expr1, expr2));
    ImmutableList<ResponseNode> results = createFuture.get();

    assertThat(results.size(), is(2));
    assertThat(results.get(0).asInstance().data().get("queryTest1").asString(), is(randomText1));
    assertThat(results.get(1).asInstance().data().get("queryTest1").asString(), is(randomText2));
  }
}
