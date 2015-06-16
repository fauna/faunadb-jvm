package com.faunadb.client.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.java.errors.BadQueryException;
import com.faunadb.client.java.errors.NotFoundQueryException;
import com.faunadb.client.java.query.*;
import com.faunadb.client.java.response.*;
import com.faunadb.client.java.types.Ref;
import com.faunadb.httpclient.Connection;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static com.faunadb.client.java.query.Language.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ClientSpec {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

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
    ListenableFuture<ResponseNode> dbCreateF = rootClient.query(Create(Ref("databases"), ObjectV("name", StringV(testDbName))));
    ResponseNode dbCreateR = dbCreateF.get();
    Ref dbRef = dbCreateR.asDatabase().ref();

    ListenableFuture<ResponseNode> keyCreateF = rootClient.query(Create(Ref("keys"), ObjectV("database", RefV(dbRef), "role", StringV("server"))));
    ResponseNode keyCreateR = keyCreateF.get();
    Key key = keyCreateR.asKey();

    client = FaunaClient.create(Connection.builder().withFaunaRoot(config.get("root_url")).withAuthToken(key.secret()).build());

    ListenableFuture<ResponseNode> classCreateF = client.query(Create(Ref("classes"), ObjectV("name", StringV("spells"))));
    classCreateF.get();

    ListenableFuture<ResponseNode> indexCreateF = client.query(Create(Ref("indexes"), ObjectV(
        "name", StringV("spells_by_test"),
        "source", RefV("classes/spells"),
        "path", StringV("data.queryTest1"),
        "unique", BooleanV(false))));

    indexCreateF.get();

    ListenableFuture<ResponseNode> setIndexF = client.query(Create(Ref("indexes"), ObjectV(
        "name", StringV("spells_instances"),
        "source", RefV("classes/spells"),
        "path", StringV("class"),
        "unique", BooleanV(false)
    )));

    setIndexF.get();

    ListenableFuture<ResponseNode> uniqueIndexF = client.query(Create(Ref("indexes"), ObjectV(
        "name", StringV("spells_by_unique_test"),
        "source", RefV("classes/spells"),
        "path", StringV("data.uniqueTest1"),
        "unique", BooleanV(true)
    )));

    uniqueIndexF.get();

    ListenableFuture<ResponseNode> indexByElementF = client.query(Create(Ref("indexes"), ObjectV(
        "name", StringV("spells_by_element"),
        "source", RefV("classes/spells"),
        "path", StringV("data.element")
    )));

    indexByElementF.get();
  }

  @Test(expected = NotFoundQueryException.class)
  public void testLookupMissingInstance() throws Throwable {
    ListenableFuture<ResponseNode> resp = client.query(Get(RefV("classes/spells/1234")));
    try {
      resp.get();
    } catch (ExecutionException ex) {
      throw ex.getCause();
    }
  }

  @Test
  public void testCreateNewInstance() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<ResponseNode> respF = client.query(Create(Ref("classes/spells"), ObjectV("data", ObjectV("testField", StringV("testValue")))));
    Instance resp = respF.get().asInstance();

    assertThat(resp.ref().value(), startsWith("classes/spells/"));
    assertThat(resp.classRef().value(), is("classes/spells"));
    assertThat(resp.data().get("testField").asString(), is("testValue"));

    ListenableFuture<ResponseNode> resp2F = client.query(Create(Ref("classes/spells"), ObjectV("data", ObjectV("testField", ObjectV("array", ArrayV(NumberV(1), StringV("2"), DoubleV(3.4)), "bool", BooleanV(true), "num", NumberV(1234), "string", StringV("sup"), "float", DoubleV(1.234))))));
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
    Ref classRef = Ref("classes/spells");
    ListenableFuture<ResponseNode> createF1 = client.query(Create(classRef, ObjectV("data", ObjectV("queryTest1", StringV(randomText1)))));
    ListenableFuture<ResponseNode> createF2 = client.query(Create(classRef, ObjectV("data", ObjectV("queryTest1", StringV(randomText2)))));

    Instance create1 = createF1.get().asInstance();
    Instance create2 = createF2.get().asInstance();

    ListenableFuture<ResponseNode> queryF1 = client.query(Paginate(Match(StringV(randomText1), Ref("indexes/spells_by_test"))));
    Page page1 = queryF1.get().asPage();
    assertThat(page1.data().size(), is(1));
    assertThat(page1.data().get(0).asRef(), is(create1.ref()));

    ListenableFuture<ResponseNode> queryF2 = client.query(Paginate(Match.create(classRef, Ref("indexes/spells_instances"))));
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

    Ref classRef = Ref("classes/spells");

    Create expr1 = Create(classRef, ObjectV("data", ObjectV("queryTest1", StringV(randomText1))));
    Create expr2 = Create(classRef, ObjectV("data", ObjectV("queryTest1", StringV(randomText2))));

    ListenableFuture<ImmutableList<ResponseNode>> createFuture = client.query(ImmutableList.of(expr1, expr2));
    ImmutableList<ResponseNode> results = createFuture.get();

    assertThat(results.size(), is(2));
    assertThat(results.get(0).asInstance().data().get("queryTest1").asString(), is(randomText1));
    assertThat(results.get(1).asInstance().data().get("queryTest1").asString(), is(randomText2));
  }

  @Test
  public void testIssuePagedQuery() throws IOException, ExecutionException, InterruptedException {
    String randomText1 = RandomStringUtils.randomAlphanumeric(8);
    String randomText2 = RandomStringUtils.randomAlphanumeric(8);
    String randomText3 = RandomStringUtils.randomAlphanumeric(8);

    Ref classRef = Ref("classes/spells");

    ListenableFuture<ResponseNode> createFuture1 = client.query(Create(classRef, ObjectV("data", ObjectV("queryTest1", StringV(randomText1)))));
    ListenableFuture<ResponseNode> createFuture2 = client.query(Create(classRef, ObjectV("data", ObjectV("queryTest1", StringV(randomText2)))));
    ListenableFuture<ResponseNode> createFuture3 = client.query(Create(classRef, ObjectV("data", ObjectV("queryTest1", StringV(randomText3)))));

    createFuture1.get();
    createFuture2.get();
    createFuture3.get();

    ListenableFuture<ResponseNode> queryF = client.query(Paginate(Match(classRef, Ref("indexes/spells_instances"))).withSize(1));
    Page resp1 = queryF.get().asPage();

    assertThat(resp1.data().size(), is(1));
    assertThat(resp1.before(), not(Optional.<Ref>absent()));
    assertThat(resp1.after(), is(Optional.<Ref>absent()));

    ListenableFuture<ResponseNode> queryF2 = client.query(Paginate(Match(classRef, Ref("indexes/spells_instances"))).withSize(1).withCursor(Before(resp1.before().get())));
    Page resp2 = queryF2.get().asPage();

    assertThat(resp2.data().size(), is(1));
    assertThat(resp2.data(), not(resp1.data()));
    assertThat(resp2.before(), not(Optional.<Ref>absent()));
    assertThat(resp2.after(), not(Optional.<Ref>absent()));
  }

  @Test
  public void testIssueLambdaQuery() throws IOException, ExecutionException, InterruptedException {
    String randomText1 = RandomStringUtils.randomAlphanumeric(8);
    String randomText2 = RandomStringUtils.randomAlphanumeric(8);

    Ref classRef = Ref("classes/spells");

    ListenableFuture<ResponseNode> createFuture1 = client.query(Create(classRef, ObjectV("data", ObjectV("queryTest1", StringV(randomText1)))));
    ListenableFuture<ResponseNode> createFuture2 = client.query(Create(classRef, ObjectV("data", ObjectV("queryTest1", StringV(randomText2)))));

    createFuture1.get();
    createFuture2.get();

    ListenableFuture<ResponseNode> queryF = client.query(Map(Lambda("x", Get(Var("x"))), Paginate(Match(Ref("classes/spells"), Ref("indexes/spells_instances"))).withSize(2)));
    Page resp = queryF.get().asPage();

    assertThat(resp.data().size(), is(2));
    assertThat(resp.data().get(0).asInstance(), notNullValue());
    assertThat(resp.data().get(1).asInstance(), notNullValue());
  }

  @Test
  public void testHandleConstraintViolation() throws Throwable {
    String randomText = RandomStringUtils.randomAlphanumeric(8);
    Ref classRef = Ref("classes/spells");
    ListenableFuture<ResponseNode> createF = client.query(Create(classRef, ObjectV("data", ObjectV("uniqueTest1", StringV(randomText)))));
    createF.get();

    ListenableFuture<ResponseNode> createF2 = client.query(Create(classRef, ObjectV("data", ObjectV("uniqueTest1", StringV(randomText)))));
    try {
      createF2.get();
    } catch (ExecutionException ex) {
      Throwable t = ex.getCause();
      assertThat(t, is(instanceOf(BadQueryException.class)));
      BadQueryException bqe = (BadQueryException) t;

      assertThat(bqe.errors().size(), is(1));
      assertThat(bqe.errors().get(0).code(), is("validation failed"));
      assertThat(bqe.errors().get(0).parameters().get("data.uniqueTest1").error(), is("duplicate value"));
    }
  }

  @Test
  public void testBasicForms() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<ResponseNode> letF = client.query(Let(ImmutableMap.<String, Expression>of("x", NumberV(1), "y", NumberV(2)), Var("x")));
    ResponseNode let = letF.get();
    assertThat(let.asNumber(), is(1L));

    ListenableFuture<ResponseNode> ifF = client.query(If(BooleanV(true), StringV("was true"), StringV("was false")));
    ResponseNode ifNode = ifF.get();
    assertThat(ifNode.asString(), is("was true"));

    Long randomRefNum = RandomUtils.nextLong(0, 250000);
    Ref randomRef = Ref("classes/spells/" + randomRefNum);

    ListenableFuture<ResponseNode> doF = client.query(Do(ImmutableList.of(
        Create(RefV(randomRef), ObjectV("data", ObjectV("name", StringV("Magic Missile")))),
        Get(RefV(randomRef))
    )));
    ResponseNode doNode = doF.get();
    Instance doInstance = doNode.asInstance();
    assertThat(doInstance.ref(), is(randomRef));

    ListenableFuture<ResponseNode> objectF = client.query(ObjectV(ImmutableMap.of("name", StringV("Hen Wen"), "age", NumberV(123))));
    ResponseNode objectNode = objectF.get();
    ResponseMap objectMap = objectNode.asObject();
    assertThat(objectMap.get("name").asString(), is("Hen Wen"));
    assertThat(objectMap.get("age").asNumber(), is(123L));

    ListenableFuture<ResponseNode> selectF = client.query(Select(ImmutableList.of(Path.Object("favorites"), Path.Object("foods"), Path.Array(1)),
        ObjectV("favorites", ObjectV("foods", ArrayV(StringV("crunchings"), StringV("munchings"), StringV("lunchings"))))));
    ResponseNode selectNode = selectF.get();
    assertThat(selectNode.asString(), is("munchings"));
  }

  @Test
  public void testCollections() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<ResponseNode> mapF = client.query(Map(Lambda("munchings", Add(ImmutableList.<Expression>of(Var("munchings"), NumberV(1)))), ArrayV(NumberV(1), NumberV(2), NumberV(3))));
    ResponseNode mapNode = mapF.get();
    ImmutableList<ResponseNode> mapArray = mapNode.asArray();
    assertThat(mapArray.size(), is(3));
    assertThat(mapArray.get(0).asNumber(), is(2L));
    assertThat(mapArray.get(1).asNumber(), is(3L));
    assertThat(mapArray.get(2).asNumber(), is(4L));

    ListenableFuture<ResponseNode> foreachF = client.query(Foreach(Lambda("spell", Create(Ref("classes/spells"), ObjectV("data", ObjectV("name", Var("spell"))))), ArrayV(StringV("Fireball Level 1"), StringV("Fireball Level 2"))));
    ResponseNode foreachNode = foreachF.get();
    ImmutableList<ResponseNode> foreachArray = foreachNode.asArray();
    assertThat(foreachArray.size(), is(2));
    assertThat(foreachArray.get(0).asString(), is("Fireball Level 1"));
    assertThat(foreachArray.get(1).asString(), is("Fireball Level 2"));
  }

  @Test
  public void testResourceModification() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<ResponseNode> createF = client.query(Create(Ref("classes/spells"), ObjectV("data", ObjectV("name", StringV("Magic Missile"), "element", StringV("arcane"), "cost", NumberV(10)))));
    ResponseNode createNode = createF.get();
    Instance createInstance = createNode.asInstance();
    assertThat(createInstance.ref().value(), startsWith("classes/spells"));
    assertThat(createInstance.data().get("name").asString(), is("Magic Missile"));

    ListenableFuture<ResponseNode> updateF = client.query(Update(RefV(createInstance.ref()), ObjectV("data", ObjectV("name", StringV("Faerie Fire"), "cost", NullV()))));
    ResponseNode updateNode = updateF.get();
    Instance updateInstance = updateNode.asInstance();
    assertThat(updateInstance.ref(), is(createInstance.ref()));
    assertThat(updateInstance.data().get("name").asString(), is("Faerie Fire"));
    assertThat(updateInstance.data().get("element").asString(), is("arcane"));
    assertThat(updateInstance.data().get("cost"), nullValue());

    ListenableFuture<ResponseNode> replaceF = client.query(Replace(RefV(createInstance.ref()), ObjectV("data", ObjectV("name", StringV("Volcano"), "element", ArrayV(StringV("fire"), StringV("earth")), "cost", NumberV(10)))));
    ResponseNode replaceNode = replaceF.get();
    Instance replaceInstance = replaceNode.asInstance();
    assertThat(replaceInstance.ref(), is(createInstance.ref()));
    assertThat(replaceInstance.data().get("name").asString(), is("Volcano"));
    assertThat(replaceInstance.data().get("element").asArray().get(0).asString(), is("fire"));
    assertThat(replaceInstance.data().get("element").asArray().get(1).asString(), is("earth"));
    assertThat(replaceInstance.data().get("cost").asNumber(), is(10L));

    ListenableFuture<ResponseNode> deleteF = client.query(Delete(RefV(createInstance.ref())));
    deleteF.get();

    thrown.expectCause(isA(NotFoundQueryException.class));
    ListenableFuture<ResponseNode> getF = client.query(Get(RefV(createInstance.ref())));
    getF.get();
  }
}

