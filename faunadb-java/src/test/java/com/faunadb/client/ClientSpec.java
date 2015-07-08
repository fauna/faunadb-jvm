package com.faunadb.client;

import com.faunadb.client.errors.BadRequestException;
import com.faunadb.client.errors.NotFoundException;
import com.faunadb.client.query.*;
import com.faunadb.client.query.Set;
import com.faunadb.client.response.*;
import com.faunadb.client.types.*;
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
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.faunadb.client.query.Language.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ClientSpec {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  static ImmutableMap<String, String> config = readConfig("config/test.yml");
  static FaunaClient rootClient;
  static FaunaClient client;
  static String testDbName = RandomStringUtils.randomAlphanumeric(8);

  static ImmutableMap<String, String> readConfig(String filename) {
    try {
      FileInputStream reader = new FileInputStream(filename);
      ImmutableMap<String, String> rv = ImmutableMap.<String, String>copyOf(new Yaml().loadAs(reader, Map.class));
      reader.close();
      return rv;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @BeforeClass
  public static void beforeAll() throws IOException, ExecutionException, InterruptedException {
    rootClient = FaunaClient.create(Connection.builder().withFaunaRoot(config.get("root_url")).withAuthToken(config.get("root_token")).build());
    ListenableFuture<ResponseNode> dbCreateF = rootClient.query(Create(Ref("databases"), Quote(ObjectV("name", StringV(testDbName)))));
    ResponseNode dbCreateR = dbCreateF.get();
    Ref dbRef = dbCreateR.asDatabase().ref();

    ListenableFuture<ResponseNode> keyCreateF = rootClient.query(Create(Ref("keys"), Quote(ObjectV("database", dbRef, "role", StringV("server")))));
    ResponseNode keyCreateR = keyCreateF.get();
    Key key = keyCreateR.asKey();

    client = FaunaClient.create(Connection.builder().withFaunaRoot(config.get("root_url")).withAuthToken(key.secret()).build());

    ListenableFuture<ResponseNode> classCreateF = client.query(Create(Ref("classes"), Quote(ObjectV("name", StringV("spells")))));
    classCreateF.get();

    ListenableFuture<ResponseNode> indexByElementF = client.query(Create(Ref("indexes"), Quote(ObjectV(
      "name", StringV("spells_by_element"),
      "source", Ref("classes/spells"),
      "path", StringV("data.element")
    ))));

    indexByElementF.get();
  }

  @Test
  public void testLookupMissingInstance() throws Throwable {
    thrown.expectCause(isA(NotFoundException.class));
    ListenableFuture<ResponseNode> resp = client.query(Get(Ref("classes/spells/1234")));
    resp.get();
  }

  @Test
  public void testCreateNewInstance() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<ResponseNode> respF = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("testField", StringV("testValue"))))));
    Instance resp = respF.get().asInstance();

    assertThat(resp.ref().value(), startsWith("classes/spells/"));
    assertThat(resp.classRef().value(), is("classes/spells"));
    assertThat(resp.data().get("testField").asString(), is("testValue"));

    ListenableFuture<ResponseNode> resp2F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("testField", ObjectV("array", ArrayV(LongV(1), StringV("2"), DoubleV(3.4)), "bool", BooleanV(true), "num", LongV(1234), "string", StringV("sup"), "float", DoubleV(1.234)))))));
    Instance resp2 = resp2F.get().asInstance();

    assertTrue(resp.data().containsKey("testField"));
    ResponseMap testFieldObj = resp2.data().get("testField").asObject();
    ImmutableList<ResponseNode> array = testFieldObj.get("array").asArray();
    assertThat(array.get(0).asLong(), is(1L));
    assertThat(array.get(1).asString(), is("2"));
    assertThat(array.get(2).asDouble(), is(3.4));
    assertThat(testFieldObj.get("string").asString(), is("sup"));
    assertThat(testFieldObj.get("num").asLong(), is(1234L));
    assertThat(testFieldObj.get("bool").asBoolean(), is(true));
  }

  @Test
  public void testIssueBatchedQuery() throws IOException, ExecutionException, InterruptedException {
    String randomText1 = RandomStringUtils.randomAlphanumeric(8);
    String randomText2 = RandomStringUtils.randomAlphanumeric(8);

    Ref classRef = Ref("classes/spells");

    Create expr1 = Create(classRef, Quote(ObjectV("data", ObjectV("queryTest1", StringV(randomText1)))));
    Create expr2 = Create(classRef, Quote(ObjectV("data", ObjectV("queryTest1", StringV(randomText2)))));

    ListenableFuture<ImmutableList<ResponseNode>> createFuture = client.query(ImmutableList.of(expr1, expr2));
    ImmutableList<ResponseNode> results = createFuture.get();

    assertThat(results.size(), is(2));
    assertThat(results.get(0).asInstance().data().get("queryTest1").asString(), is(randomText1));
    assertThat(results.get(1).asInstance().data().get("queryTest1").asString(), is(randomText2));
  }

  @Test
  public void testGet() throws IOException, ExecutionException, InterruptedException {
    String randomText1 = RandomStringUtils.randomAlphanumeric(8);
    ListenableFuture<ResponseNode> createFuture1 = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("queryTest1", StringV(randomText1))))));
    Instance create1 = createFuture1.get().asInstance();

    ListenableFuture<ResponseNode> queryF = client.query(Get(create1.ref()));
    Instance result = queryF.get().asInstance();
    assertThat(result.data().get("queryTest1").asString(), is(randomText1));
  }

  @Test
  public void testPaginate() throws IOException, ExecutionException, InterruptedException {
    String randomClassName = RandomStringUtils.randomAlphanumeric(8);
    ListenableFuture<ResponseNode> randomClassF = client.query(Create(Ref("classes"), Quote(ObjectV("name", StringV(randomClassName)))));
    Ref classRef = randomClassF.get().asClass().ref();

    ListenableFuture<ResponseNode> randomClassIndexF = client.query(Create(Ref("indexes"), Quote(ObjectV(
      "name", StringV(randomClassName + "_class_index"),
      "source", classRef,
      "path", StringV("class"),
      "unique", BooleanV(false)
    ))));

    ListenableFuture<ResponseNode> indexCreateF = client.query(Create(Ref("indexes"), Quote(ObjectV(
      "name", StringV(randomClassName + "_test_index"),
      "source", classRef,
      "path", StringV("data.queryTest1"),
      "unique", BooleanV(false)))));

    Ref randomClassIndex = randomClassIndexF.get().asIndex().ref();
    Ref testIndex = indexCreateF.get().asIndex().ref();

    String randomText1 = RandomStringUtils.randomAlphanumeric(8);
    String randomText2 = RandomStringUtils.randomAlphanumeric(8);
    String randomText3 = RandomStringUtils.randomAlphanumeric(8);

    ListenableFuture<ResponseNode> createFuture1 = client.query(Create(classRef, Quote(ObjectV("data", ObjectV("queryTest1", StringV(randomText1))))));
    ListenableFuture<ResponseNode> createFuture2 = client.query(Create(classRef, Quote(ObjectV("data", ObjectV("queryTest1", StringV(randomText2))))));
    ListenableFuture<ResponseNode> createFuture3 = client.query(Create(classRef, Quote(ObjectV("data", ObjectV("queryTest1", StringV(randomText3))))));

    Instance create1 = createFuture1.get().asInstance();
    Instance create2 = createFuture2.get().asInstance();
    Instance create3 = createFuture3.get().asInstance();

    ListenableFuture<ResponseNode> queryF1 = client.query(Paginate(Match(StringV(randomText1), testIndex)));
    Page page1 = queryF1.get().asPage();
    assertThat(page1.data().size(), is(1));
    assertThat(page1.data().get(0).asRef(), is(create1.ref()));

    ListenableFuture<ResponseNode> queryF2 = client.query(Paginate(Match.create(classRef, randomClassIndex)));
    Page page = queryF2.get().asPage();

    ImmutableList.Builder<Ref> refsBuilder = ImmutableList.builder();
    for (ResponseNode node : page.data()) {
      refsBuilder.add(node.asRef());
    }

    ImmutableList<Ref> refs = refsBuilder.build();
    assertThat(refs, hasItems(create1.ref(), create2.ref()));

    ListenableFuture<ResponseNode> countF1 = client.query(Count(Match(classRef, randomClassIndex)));
    ResponseNode countNode = countF1.get();
    assertThat(countNode.asLong(), is(3L));

    ListenableFuture<ResponseNode> queryF3 = client.query(Paginate(Match(classRef, randomClassIndex)).withSize(1));
    Page resp1 = queryF3.get().asPage();

    assertThat(resp1.data().size(), is(1));
    assertThat(resp1.after(), not(Optional.<ResponseNode>absent()));
    assertThat(resp1.before(), is(Optional.<ResponseNode>absent()));

    ListenableFuture<ResponseNode> queryF4 = client.query(Paginate(Match(classRef, randomClassIndex)).withSize(1).withCursor(After(resp1.after().get().asRef())));
    Page resp2 = queryF4.get().asPage();

    assertThat(resp2.data().size(), is(1));
    assertThat(resp2.data(), not(resp1.data()));
    assertThat(resp2.before(), not(Optional.<ResponseNode>absent()));
    assertThat(resp2.after(), not(Optional.<ResponseNode>absent()));
  }

  @Test
  public void testHandleConstraintViolation() throws Throwable {
    String randomClassName = RandomStringUtils.randomAlphanumeric(8);
    ListenableFuture<ResponseNode> randomClassF = client.query(Create(Ref("classes"), Quote(ObjectV("name", StringV(randomClassName)))));
    Ref classRef = randomClassF.get().asClass().ref();

    ListenableFuture<ResponseNode> randomClassIndexF = client.query(Create(Ref("indexes"), Quote(ObjectV(
        "name", StringV(randomClassName + "_class_index"),
        "source", classRef,
        "path", StringV("data.uniqueTest1"),
        "unique", BooleanV(true)
    ))));
    randomClassIndexF.get();

    String randomText = RandomStringUtils.randomAlphanumeric(8);
    ListenableFuture<ResponseNode> createF = client.query(Create(classRef, Quote(ObjectV("data", ObjectV("uniqueTest1", StringV(randomText))))));
    createF.get();

    ListenableFuture<ResponseNode> createF2 = client.query(Create(classRef, Quote(ObjectV("data", ObjectV("uniqueTest1", StringV(randomText))))));
    thrown.expectCause(isA(BadRequestException.class));
    createF2.get();
  }

  @Test
  public void testTypes() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<ResponseNode> setF = client.query(Match(StringV("arcane"), Ref("indexes/spells_by_element")));
    ResponseNode setNode = setF.get();
    com.faunadb.client.response.Set set = setNode.asSet();
    assertThat(set.parameters().get("match").asString(), is("arcane"));
    assertThat(set.parameters().get("index").asRef(), is(Ref("indexes/spells_by_element")));
  }

  @Test
  public void testBasicForms() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<ResponseNode> letF = client.query(Let(ImmutableMap.<String, Expression>of("x", LongV(1), "y", LongV(2)), Var("x")));
    ResponseNode let = letF.get();
    assertThat(let.asLong(), is(1L));

    ListenableFuture<ResponseNode> ifF = client.query(If(BooleanV(true), StringV("was true"), StringV("was false")));
    ResponseNode ifNode = ifF.get();
    assertThat(ifNode.asString(), is("was true"));

    Long randomRefNum = RandomUtils.nextLong(0, 250000);
    Ref randomRef = Ref("classes/spells/" + randomRefNum);

    ListenableFuture<ResponseNode> doF = client.query(Do(ImmutableList.of(
        Create(randomRef, Quote(ObjectV("data", ObjectV("name", StringV("Magic Missile"))))),
        Get(randomRef)
    )));
    ResponseNode doNode = doF.get();
    Instance doInstance = doNode.asInstance();
    assertThat(doInstance.ref(), is(randomRef));

    ListenableFuture<ResponseNode> objectF = client.query(Quote(ObjectV(ImmutableMap.of("name", StringV("Hen Wen"), "age", LongV(123)))));
    ResponseNode objectNode = objectF.get();
    ResponseMap objectMap = objectNode.asObject();
    assertThat(objectMap.get("name").asString(), is("Hen Wen"));
    assertThat(objectMap.get("age").asLong(), is(123L));

    ListenableFuture<ResponseNode> selectF = client.query(Select(ImmutableList.of(Path.Object("favorites"), Path.Object("foods"), Path.Array(1)),
        Quote(ObjectV("favorites", ObjectV("foods", ArrayV(StringV("crunchings"), StringV("munchings"), StringV("lunchings")))))));
    ResponseNode selectNode = selectF.get();
    assertThat(selectNode.asString(), is("munchings"));
  }

  @Test
  public void testCollections() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<ResponseNode> mapF = client.query(Map(Lambda("munchings", Add(ImmutableList.<Expression>of(Var("munchings"), LongV(1)))), ArrayV(LongV(1), LongV(2), LongV(3))));
    ResponseNode mapNode = mapF.get();
    ImmutableList<ResponseNode> mapArray = mapNode.asArray();
    assertThat(mapArray.size(), is(3));
    assertThat(mapArray.get(0).asLong(), is(2L));
    assertThat(mapArray.get(1).asLong(), is(3L));
    assertThat(mapArray.get(2).asLong(), is(4L));

    ListenableFuture<ResponseNode> foreachF = client.query(Foreach(Lambda("spell", Create(Ref("classes/spells"), Object(ObjectV("data", Object(ObjectV("name", Var("spell"))))))), ArrayV(StringV("Fireball Level 1"), StringV("Fireball Level 2"))));
    ResponseNode foreachNode = foreachF.get();
    ImmutableList<ResponseNode> foreachArray = foreachNode.asArray();
    assertThat(foreachArray.size(), is(2));
    assertThat(foreachArray.get(0).asString(), is("Fireball Level 1"));
    assertThat(foreachArray.get(1).asString(), is("Fireball Level 2"));
  }

  @Test
  public void testResourceModification() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<ResponseNode> createF = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("name", StringV("Magic Missile"), "element", StringV("arcane"), "cost", LongV(10))))));
    ResponseNode createNode = createF.get();
    Instance createInstance = createNode.asInstance();
    assertThat(createInstance.ref().value(), startsWith("classes/spells"));
    assertThat(createInstance.data().get("name").asString(), is("Magic Missile"));

    ListenableFuture<ResponseNode> updateF = client.query(Update(createInstance.ref(), Quote(ObjectV("data", ObjectV("name", StringV("Faerie Fire"), "cost", NullV())))));
    ResponseNode updateNode = updateF.get();
    Instance updateInstance = updateNode.asInstance();
    assertThat(updateInstance.ref(), is(createInstance.ref()));
    assertThat(updateInstance.data().get("name").asString(), is("Faerie Fire"));
    assertThat(updateInstance.data().get("element").asString(), is("arcane"));
    assertThat(updateInstance.data().get("cost"), nullValue());

    ListenableFuture<ResponseNode> replaceF = client.query(Replace(createInstance.ref(), Quote(ObjectV("data", ObjectV("name", StringV("Volcano"), "element", ArrayV(StringV("fire"), StringV("earth")), "cost", LongV(10))))));
    ResponseNode replaceNode = replaceF.get();
    Instance replaceInstance = replaceNode.asInstance();
    assertThat(replaceInstance.ref(), is(createInstance.ref()));
    assertThat(replaceInstance.data().get("name").asString(), is("Volcano"));
    assertThat(replaceInstance.data().get("element").get(0).asString(), is("fire"));
    assertThat(replaceInstance.data().get("element").get(1).asString(), is("earth"));
    assertThat(replaceInstance.data().get("cost").asLong(), is(10L));

    ListenableFuture<ResponseNode> deleteF = client.query(Delete(createInstance.ref()));
    deleteF.get();

    thrown.expectCause(isA(NotFoundException.class));
    ListenableFuture<ResponseNode> getF = client.query(Get(createInstance.ref()));
    getF.get();
  }

  @Test
  public void testSets() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<ResponseNode> create1F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("name", StringV("Magic Missile"), "element", StringV("arcane"), "cost", LongV(10))))));
    ListenableFuture<ResponseNode> create2F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("name", StringV("Fireball"), "element", StringV("fire"), "cost", LongV(10))))));
    ListenableFuture<ResponseNode> create3F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("name", StringV("Faerie Fire"), "element", ArrayV(StringV("arcane"), StringV("nature")), "cost", LongV(10))))));
    ListenableFuture<ResponseNode> create4F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("name", StringV("Summon Animal Companion"), "element", StringV("nature"), "cost", LongV(10))))));
    ResponseNode createNode1 = create1F.get();
    ResponseNode createNode2 = create2F.get();
    ResponseNode createNode3 = create3F.get();
    ResponseNode createNode4 = create4F.get();
    Instance createInstance1 = createNode1.asInstance();
    Instance createInstance2 = createNode2.asInstance();
    Instance createInstance3 = createNode3.asInstance();
    Instance createInstance4 = createNode4.asInstance();

    ListenableFuture<ResponseNode> matchF = client.query(Paginate(Match(StringV("arcane"), Ref("indexes/spells_by_element"))));
    ResponseNode matchResponse = matchF.get();
    Page matchList = matchResponse.asPage();
    assertThat(matchList.data().size(), greaterThanOrEqualTo(1));
    ImmutableList.Builder<Ref> matchRefsBuilder = ImmutableList.builder();
    for (ResponseNode matchNode : matchList.data()) {
      matchRefsBuilder.add(matchNode.asRef());
    }
    assertThat(matchRefsBuilder.build(), hasItem(createInstance1.ref()));

    ListenableFuture<ResponseNode> matchEventsF = client.query(Paginate(Match(StringV("arcane"), Ref("indexes/spells_by_element"))).withEvents(true));
    ResponseNode matchEventsResponse = matchEventsF.get();
    Page matchEventsPage = matchEventsResponse.asPage();
    assertThat(matchEventsPage.data().size(), greaterThanOrEqualTo(1));
    ImmutableList.Builder<Ref> matchRefEventsBuilder = ImmutableList.builder();
    for (ResponseNode matchEventNode : matchEventsPage.data()) {
      Event event = matchEventNode.asEvent();
      if (event.action().contentEquals("create")) {
        matchRefEventsBuilder.add(event.resource());
      }
    }
    assertThat(matchRefEventsBuilder.build(), hasItem(createInstance1.ref()));

    ListenableFuture<ResponseNode> unionF = client.query(Paginate(Union(ImmutableList.<Set>of(Match(StringV("arcane"), Ref("indexes/spells_by_element")), Match(StringV("fire"), Ref("indexes/spells_by_element"))))));
    ResponseNode unionResponse = unionF.get();
    Page unionPage = unionResponse.asPage();
    assertThat(unionPage.data().size(), greaterThanOrEqualTo(2));
    ImmutableList.Builder<Ref> unionRefsBuilder = ImmutableList.builder();
    for (ResponseNode unionNode : unionPage.data()) {
      unionRefsBuilder.add(unionNode.asRef());
    }
    assertThat(unionRefsBuilder.build(), hasItems(createInstance1.ref(), createInstance2.ref()));

    ListenableFuture<ResponseNode> unionEventsF = client.query(Paginate(Union(ImmutableList.<Set>of(Match(StringV("arcane"), Ref("indexes/spells_by_element")), Match(StringV("fire"), Ref("indexes/spells_by_element"))))).withEvents(true));
    ResponseNode unionEventsResponse = unionEventsF.get();
    Page unionEventsPage = unionEventsResponse.asPage();
    assertThat(unionEventsPage.data().size(), greaterThanOrEqualTo(2));
    ImmutableList.Builder<Ref> unionEventsRefsBuilder = ImmutableList.builder();
    for (ResponseNode unionEventsNode : unionEventsPage.data()) {
      Event event = unionEventsNode.asEvent();
      if (event.action().contentEquals("create")) {
        unionEventsRefsBuilder.add(event.resource());
      }
    }
    assertThat(unionEventsRefsBuilder.build(), hasItems(createInstance1.ref(), createInstance2.ref()));

    ListenableFuture<ResponseNode> intersectionF = client.query(Paginate(Intersection(ImmutableList.<Set>of(Match(StringV("arcane"), Ref("indexes/spells_by_element")), Match(StringV("nature"), Ref("indexes/spells_by_element"))))));
    ResponseNode intersectionResponse = intersectionF.get();
    Page intersectionPage = intersectionResponse.asPage();
    assertThat(intersectionPage.data().size(), greaterThanOrEqualTo(1));
    ImmutableList.Builder<Ref> intersectionRefsBuilder = ImmutableList.builder();
    for (ResponseNode intersectionNode : intersectionPage.data()) {
      intersectionRefsBuilder.add(intersectionNode.asRef());
    }
    assertThat(intersectionRefsBuilder.build(), hasItem(createInstance3.ref()));

    ListenableFuture<ResponseNode> differenceF = client.query(Paginate(Difference(ImmutableList.<Set>of(Match(StringV("nature"), Ref("indexes/spells_by_element")), Match(StringV("arcane"), Ref("indexes/spells_by_element"))))));
    ResponseNode differenceResponse = differenceF.get();
    Page differencePage = differenceResponse.asPage();
    assertThat(differencePage.data().size(), greaterThanOrEqualTo(1));
    ImmutableList.Builder<Ref> differenceRefsBuilder = ImmutableList.builder();
    for (ResponseNode differenceNode : differencePage.data()) {
      differenceRefsBuilder.add(differenceNode.asRef());
    }
    assertThat(differenceRefsBuilder.build(), hasItem(createInstance4.ref()));
    assertThat(differenceRefsBuilder.build(), not(hasItem(createInstance3.ref())));
  }
}

