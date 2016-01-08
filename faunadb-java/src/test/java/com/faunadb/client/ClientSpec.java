package com.faunadb.client;

import com.faunadb.client.errors.BadRequestException;
import com.faunadb.client.errors.NotFoundException;
import com.faunadb.client.errors.UnauthorizedException;
import com.faunadb.client.query.*;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.faunadb.client.query.Language.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ClientSpec {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  static ImmutableMap<String, String> config = getConfig();
  static FaunaClient rootClient;
  static FaunaClient client;
  static String testDbName = "faunadb-java-test-" + RandomStringUtils.randomAlphanumeric(8);

  static ImmutableMap<String, String> getConfig() {
    String rootKey = System.getenv("FAUNA_ROOT_KEY");
    if (rootKey == null) throw new RuntimeException("FAUNA_ROOT_KEY must be defined to run tests");

    String domain = System.getenv("FAUNA_DOMAIN");
    if (domain == null) domain = "rest.faunadb.com";

    String scheme = System.getenv("FAUNA_SCHEME");
    if (scheme == null) scheme = "https";

    String port = System.getenv("FAUNA_PORT");
    if (port == null) port = "443";

    return ImmutableMap.<String, String>builder()
      .put("root_token", rootKey)
      .put("root_url", scheme + "://" + domain + ":" + port)
      .build();
  }

  @BeforeClass
  public static void beforeAll() throws IOException, ExecutionException, InterruptedException {
    rootClient = FaunaClient.create(Connection.builder().withFaunaRoot(config.get("root_url")).withAuthToken(config.get("root_token")).build());
    ListenableFuture<Value> dbCreateF = rootClient.query(Create(Ref("databases"), Quote(ObjectV("name", StringV(testDbName)))));
    Value dbCreateR = dbCreateF.get();
    Ref dbRef = dbCreateR.asDatabase().ref();

    ListenableFuture<Value> keyCreateF = rootClient.query(Create(Ref("keys"), Quote(ObjectV("database", dbRef, "role", StringV("server")))));
    Value keyCreateR = keyCreateF.get();
    Key key = keyCreateR.asKey();

    client = FaunaClient.create(Connection.builder().withFaunaRoot(config.get("root_url")).withAuthToken(key.secret()).build());

    ListenableFuture<Value> classCreateF = client.query(Create(Ref("classes"), Quote(ObjectV("name", StringV("spells")))));
    classCreateF.get();

    ListenableFuture<Value> createClass2F = client.query(Create(Ref("classes"), Quote(ObjectV("name", StringV("characters")))));
    createClass2F.get();

    ListenableFuture<Value> createClass3F = client.query(Create(Ref("classes"), Quote(ObjectV("name", StringV("spellbooks")))));
    createClass3F.get();

    ListenableFuture<Value> indexByElementF = client.query(Create(Ref("indexes"), Quote(ObjectV(
      "name", StringV("spells_by_element"),
      "source", Ref("classes/spells"),
      "path", StringV("data.element"),
      "active", BooleanV(true)
    ))));

    indexByElementF.get();

    ListenableFuture<Value> indexSpellbookByOwnerF = client.query(Create(Ref("indexes"), Quote(ObjectV(
      "name", StringV("spellbooks_by_owner"),
      "source", Ref("classes/spellbooks"),
      "path", StringV("data.owner"),
      "active", BooleanV(true)
    ))));

    indexSpellbookByOwnerF.get();

    ListenableFuture<Value> indexBySpellbookF = client.query(Create(Ref("indexes"), Quote(ObjectV(
      "name", StringV("spells_by_spellbook"),
      "source", Ref("classes/spells"),
      "path", StringV("data.spellbook"),
      "active", BooleanV(true)
    ))));

    indexBySpellbookF.get();
  }

  @Test
  public void testLookupMissingInstance() throws Throwable {
    thrown.expectCause(isA(NotFoundException.class));
    ListenableFuture<Value> resp = client.query(Get(Ref("classes/spells/1234")));
    resp.get();
  }

  @Test
  public void testUnauthorizedExcpetion() throws Throwable {
    thrown.expectCause(isA(UnauthorizedException.class));
    FaunaClient badClient = FaunaClient.create(Connection.builder().withFaunaRoot(config.get("root_url")).withAuthToken("notavalidsecret").build());
    ListenableFuture<Value> resp = badClient.query(Get(Ref("classes/spells/1234")));
    resp.get();
  }

  @Test
  public void testCreateNewInstance() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<Value> respF = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("testField", StringV("testValue"))))));
    Instance resp = respF.get().asInstance();

    assertThat(resp.ref().value(), startsWith("classes/spells/"));
    assertThat(resp.classRef().value(), is("classes/spells"));
    assertThat(resp.data().get("testField").asString(), is("testValue"));

    ListenableFuture<Value> existsF = client.query(Exists(resp.ref()));
    assertThat(existsF.get().asBoolean(), is(true));

    ListenableFuture<Value> resp2F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("testField", ObjectV("array", ArrayV(LongV(1), StringV("2"), DoubleV(3.4)), "bool", BooleanV(true), "num", LongV(1234), "string", StringV("sup"), "float", DoubleV(1.234)))))));
    Instance resp2 = resp2F.get().asInstance();

    assertTrue(resp.data().containsKey("testField"));
    ImmutableMap<String, Value> testFieldObj = resp2.data().get("testField").asObject();
    ImmutableList<Value> array = testFieldObj.get("array").asArray();
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

    Value expr1 = Create(classRef, Quote(ObjectV("data", ObjectV("queryTest1", StringV(randomText1)))));
    Value expr2 = Create(classRef, Quote(ObjectV("data", ObjectV("queryTest1", StringV(randomText2)))));

    ListenableFuture<ImmutableList<Value>> createFuture = client.query(ImmutableList.of(expr1, expr2));
    ImmutableList<Value> results = createFuture.get();

    assertThat(results.size(), is(2));
    assertThat(results.get(0).asInstance().data().get("queryTest1").asString(), is(randomText1));
    assertThat(results.get(1).asInstance().data().get("queryTest1").asString(), is(randomText2));
  }

  @Test
  public void testGet() throws IOException, ExecutionException, InterruptedException {
    String randomText1 = RandomStringUtils.randomAlphanumeric(8);
    ListenableFuture<Value> createFuture1 = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("queryTest1", StringV(randomText1))))));
    Instance create1 = createFuture1.get().asInstance();

    ListenableFuture<Value> queryF = client.query(Get(create1.ref()));
    Instance result = queryF.get().asInstance();
    assertThat(result.data().get("queryTest1").asString(), is(randomText1));
  }

  @Test
  public void testPaginate() throws IOException, ExecutionException, InterruptedException {
    String randomClassName = RandomStringUtils.randomAlphanumeric(8);
    ListenableFuture<Value> randomClassF = client.query(Create(Ref("classes"), Quote(ObjectV("name", StringV(randomClassName)))));
    Ref classRef = randomClassF.get().asClass().ref();

    ListenableFuture<Value> randomClassIndexF = client.query(Create(Ref("indexes"), Quote(ObjectV(
      "name", StringV(randomClassName + "_class_index"),
      "source", classRef,
      "path", StringV("class"),
      "active", BooleanV(true),
      "unique", BooleanV(false)
    ))));

    ListenableFuture<Value> indexCreateF = client.query(Create(Ref("indexes"), Quote(ObjectV(
      "name", StringV(randomClassName + "_test_index"),
      "source", classRef,
      "path", StringV("data.queryTest1"),
      "active", BooleanV(true),
      "unique", BooleanV(false)))));

    Ref randomClassIndex = randomClassIndexF.get().asIndex().ref();
    Ref testIndex = indexCreateF.get().asIndex().ref();

    String randomText1 = RandomStringUtils.randomAlphanumeric(8);
    String randomText2 = RandomStringUtils.randomAlphanumeric(8);
    String randomText3 = RandomStringUtils.randomAlphanumeric(8);

    ListenableFuture<Value> createFuture1 = client.query(Create(classRef, Quote(ObjectV("data", ObjectV("queryTest1", StringV(randomText1))))));
    ListenableFuture<Value> createFuture2 = client.query(Create(classRef, Quote(ObjectV("data", ObjectV("queryTest1", StringV(randomText2))))));
    ListenableFuture<Value> createFuture3 = client.query(Create(classRef, Quote(ObjectV("data", ObjectV("queryTest1", StringV(randomText3))))));

    Instance create1 = createFuture1.get().asInstance();
    Instance create2 = createFuture2.get().asInstance();
    Instance create3 = createFuture3.get().asInstance();

    ListenableFuture<Value> queryF1 = client.query(Paginate(Match(StringV(randomText1), testIndex)).build());
    Page page1 = queryF1.get().asPage();
    assertThat(page1.data().size(), is(1));
    assertThat(page1.data().get(0).asRef(), is(create1.ref()));

    ListenableFuture<Value> queryF2 = client.query(Paginate(Match(classRef, randomClassIndex)).build());
    Page page = queryF2.get().asPage();

    ImmutableList.Builder<Ref> refsBuilder = ImmutableList.builder();
    for (Value node : page.data()) {
      refsBuilder.add(node.asRef());
    }

    ImmutableList<Ref> refs = refsBuilder.build();
    assertThat(refs, hasItems(create1.ref(), create2.ref()));

    ListenableFuture<Value> countF1 = client.query(Count(Match(classRef, randomClassIndex)));
    Value countNode = countF1.get();
    assertThat(countNode.asLong(), is(3L));

    ListenableFuture<Value> queryF3 = client.query(Paginate(Match(classRef, randomClassIndex)).withSize(1).build());
    Page resp1 = queryF3.get().asPage();

    assertThat(resp1.data().size(), is(1));
    assertThat(resp1.after(), not(Optional.<Value>absent()));
    assertThat(resp1.before(), is(Optional.<Value>absent()));

    ListenableFuture<Value> queryF4 = client.query(Paginate(Match(classRef, randomClassIndex)).withSize(1).withCursor(After(resp1.after().get())).build());
    Page resp2 = queryF4.get().asPage();

    assertThat(resp2.data().size(), is(1));
    assertThat(resp2.data(), not(resp1.data()));
    assertThat(resp2.before(), not(Optional.<Value>absent()));
    assertThat(resp2.after(), not(Optional.<Value>absent()));
  }

  @Test
  public void testHandleConstraintViolation() throws Throwable {
    String randomClassName = RandomStringUtils.randomAlphanumeric(8);
    ListenableFuture<Value> randomClassF = client.query(Create(Ref("classes"), Quote(ObjectV("name", StringV(randomClassName)))));
    Ref classRef = randomClassF.get().asClass().ref();

    ListenableFuture<Value> randomClassIndexF = client.query(Create(Ref("indexes"), Quote(ObjectV(
        "name", StringV(randomClassName + "_class_index"),
        "source", classRef,
        "path", StringV("data.uniqueTest1"),
        "active", BooleanV(true),
        "unique", BooleanV(true)
    ))));
    randomClassIndexF.get();

    String randomText = RandomStringUtils.randomAlphanumeric(8);
    ListenableFuture<Value> createF = client.query(Create(classRef, Quote(ObjectV("data", ObjectV("uniqueTest1", StringV(randomText))))));
    createF.get();

    ListenableFuture<Value> createF2 = client.query(Create(classRef, Quote(ObjectV("data", ObjectV("uniqueTest1", StringV(randomText))))));
    thrown.expectCause(isA(BadRequestException.class));
    createF2.get();
  }

  @Test
  public void testTypes() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<Value> setF = client.query(Match(StringV("arcane"), Ref("indexes/spells_by_element")));
    Value setNode = setF.get();
    com.faunadb.client.types.Set set = setNode.asSet();
    assertThat(set.parameters().get("terms").asString(), is("arcane"));
    assertThat(set.parameters().get("match").asRef(), is(Ref("indexes/spells_by_element")));
  }

  @Test
  public void testBasicForms() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<Value> letF = client.query(Let(ImmutableMap.<String, Value>of("x", LongV(1), "y", LongV(2)), Var("x")));
    Value let = letF.get();
    assertThat(let.asLong(), is(1L));

    ListenableFuture<Value> ifF = client.query(If(BooleanV(true), StringV("was true"), StringV("was false")));
    Value ifNode = ifF.get();
    assertThat(ifNode.asString(), is("was true"));

    Long randomRefNum = RandomUtils.nextLong(0, 250000);
    Ref randomRef = Ref("classes/spells/" + randomRefNum);

    ListenableFuture<Value> doF = client.query(Do(
      Create(randomRef, Quote(ObjectV("data", ObjectV("name", StringV("Magic Missile"))))),
      Get(randomRef)
    ));
    Value doNode = doF.get();
    Instance doInstance = doNode.asInstance();
    assertThat(doInstance.ref(), is(randomRef));

    ListenableFuture<Value> objectF = client.query(Quote(ObjectV("name", StringV("Hen Wen"), "age", LongV(123))));
    Value objectNode = objectF.get();
    ImmutableMap<String, Value> objectMap = objectNode.asObject();
    assertThat(objectMap.get("name").asString(), is("Hen Wen"));
    assertThat(objectMap.get("age").asLong(), is(123L));
  }

  @Test
  public void testCollections() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<Value> mapF = client.query(Map(Lambda("munchings", Add(Var("munchings"), LongV(1))), ArrayV(LongV(1), LongV(2), LongV(3))));
    Value mapNode = mapF.get();
    ImmutableList<Value> mapArray = mapNode.asArray();
    assertThat(mapArray.size(), is(3));
    assertThat(mapArray.get(0).asLong(), is(2L));
    assertThat(mapArray.get(1).asLong(), is(3L));
    assertThat(mapArray.get(2).asLong(), is(4L));

    ListenableFuture<Value> foreachF = client.query(Foreach(Lambda("spell", Create(Ref("classes/spells"), Object(ObjectV("data", Object(ObjectV("name", Var("spell"))))))), ArrayV(StringV("Fireball Level 1"), StringV("Fireball Level 2"))));
    Value foreachNode = foreachF.get();
    ImmutableList<Value> foreachArray = foreachNode.asArray();
    assertThat(foreachArray.size(), is(2));
    assertThat(foreachArray.get(0).asString(), is("Fireball Level 1"));
    assertThat(foreachArray.get(1).asString(), is("Fireball Level 2"));
  }

  @Test
  public void testResourceModification() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<Value> createF = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("name", StringV("Magic Missile"), "element", StringV("arcane"), "cost", LongV(10))))));
    Value createNode = createF.get();
    Instance createInstance = createNode.asInstance();
    assertThat(createInstance.ref().value(), startsWith("classes/spells"));
    assertThat(createInstance.data().get("name").asString(), is("Magic Missile"));

    ListenableFuture<Value> updateF = client.query(Update(createInstance.ref(), Quote(ObjectV("data", ObjectV("name", StringV("Faerie Fire"), "cost", NullV())))));
    Value updateNode = updateF.get();
    Instance updateInstance = updateNode.asInstance();
    assertThat(updateInstance.ref(), is(createInstance.ref()));
    assertThat(updateInstance.data().get("name").asString(), is("Faerie Fire"));
    assertThat(updateInstance.data().get("element").asString(), is("arcane"));
    assertThat(updateInstance.data().get("cost"), nullValue());

    ListenableFuture<Value> replaceF = client.query(Replace(createInstance.ref(), Quote(ObjectV("data", ObjectV("name", StringV("Volcano"), "element", ArrayV(StringV("fire"), StringV("earth")), "cost", LongV(10))))));
    Value replaceNode = replaceF.get();
    Instance replaceInstance = replaceNode.asInstance();
    assertThat(replaceInstance.ref(), is(createInstance.ref()));
    assertThat(replaceInstance.data().get("name").asString(), is("Volcano"));
    assertThat(replaceInstance.data().get("element").get(0).asString(), is("fire"));
    assertThat(replaceInstance.data().get("element").get(1).asString(), is("earth"));
    assertThat(replaceInstance.data().get("cost").asLong(), is(10L));

    ListenableFuture<Value> insertF = client.query(Insert(createInstance.ref(), 1L, Action.CREATE, Quote(ObjectV("data", ObjectV("cooldown", LongV(5L))))));
    Instance insertR = insertF.get().asInstance();
    assertThat(insertR.ref(), is(createInstance.ref()));
    assertThat(insertR.data().size(), is(1));
    assertThat(insertR.data().get("cooldown").asLong(), is(5L));

    ListenableFuture<Value> removeF = client.query(Remove(createInstance.ref(), 2L, Action.DELETE));
    Value removeR = removeF.get();
    assertNull(removeR);

    ListenableFuture<Value> deleteF = client.query(Delete(createInstance.ref()));
    deleteF.get();

    thrown.expectCause(isA(NotFoundException.class));
    ListenableFuture<Value> getF = client.query(Get(createInstance.ref()));
    getF.get();
  }

  @Test
  public void testSets() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<Value> create1F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("name", StringV("Magic Missile"), "element", StringV("arcane"), "cost", LongV(10))))));
    ListenableFuture<Value> create2F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("name", StringV("Fireball"), "element", StringV("fire"), "cost", LongV(10))))));
    ListenableFuture<Value> create3F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("name", StringV("Faerie Fire"), "element", ArrayV(StringV("arcane"), StringV("nature")), "cost", LongV(10))))));
    ListenableFuture<Value> create4F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("name", StringV("Summon Animal Companion"), "element", StringV("nature"), "cost", LongV(10))))));
    Value createNode1 = create1F.get();
    Value createNode2 = create2F.get();
    Value createNode3 = create3F.get();
    Value createNode4 = create4F.get();
    Instance createInstance1 = createNode1.asInstance();
    Instance createInstance2 = createNode2.asInstance();
    Instance createInstance3 = createNode3.asInstance();
    Instance createInstance4 = createNode4.asInstance();

    ListenableFuture<Value> createCharF = client.query(Create(Ref("classes/characters"), Quote(ObjectV("data", ObjectV()))));
    Instance createCharR = createCharF.get().asInstance();

    ListenableFuture<Value> createSpellbookF = client.query(Create(Ref("classes/spellbooks"), Quote(ObjectV("data", ObjectV("owner", createCharR.ref())))));
    Instance createSpellbookR = createSpellbookF.get().asInstance();

    ListenableFuture<Value> createSpellbookSpell1F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("spellbook", createSpellbookR.ref())))));
    ListenableFuture<Value> createSpellbookSpell2F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data", ObjectV("spellbook", createSpellbookR.ref())))));
    Instance createSpellbookSpell1R = createSpellbookSpell1F.get().asInstance();
    Instance createSpellbookSpell2R = createSpellbookSpell2F.get().asInstance();

    ListenableFuture<Value> matchF = client.query(Paginate(Match(StringV("arcane"), Ref("indexes/spells_by_element"))).build());
    Value matchResponse = matchF.get();
    Page matchList = matchResponse.asPage();
    assertThat(matchList.data().size(), greaterThanOrEqualTo(1));
    ImmutableList.Builder<Ref> matchRefsBuilder = ImmutableList.builder();
    for (Value matchNode : matchList.data()) {
      matchRefsBuilder.add(matchNode.asRef());
    }
    assertThat(matchRefsBuilder.build(), hasItem(createInstance1.ref()));

    ListenableFuture<Value> matchEventsF = client.query(Paginate(Match(StringV("arcane"), Ref("indexes/spells_by_element"))).withEvents(true).build());
    Value matchEventsResponse = matchEventsF.get();
    Page matchEventsPage = matchEventsResponse.asPage();
    assertThat(matchEventsPage.data().size(), greaterThanOrEqualTo(1));
    ImmutableList.Builder<Ref> matchRefEventsBuilder = ImmutableList.builder();
    for (Value matchEventNode : matchEventsPage.data()) {
      Event event = matchEventNode.asEvent();
      if (event.action().contentEquals("create")) {
        matchRefEventsBuilder.add(event.resource());
      }
    }
    assertThat(matchRefEventsBuilder.build(), hasItem(createInstance1.ref()));

    ListenableFuture<Value> unionF = client.query(Paginate(Union(Match(StringV("arcane"), Ref("indexes/spells_by_element")), Match(StringV("fire"), Ref("indexes/spells_by_element")))).build());
    Value unionResponse = unionF.get();
    Page unionPage = unionResponse.asPage();
    assertThat(unionPage.data().size(), greaterThanOrEqualTo(2));
    ImmutableList.Builder<Ref> unionRefsBuilder = ImmutableList.builder();
    for (Value unionNode : unionPage.data()) {
      unionRefsBuilder.add(unionNode.asRef());
    }
    assertThat(unionRefsBuilder.build(), hasItems(createInstance1.ref(), createInstance2.ref()));

    ListenableFuture<Value> unionEventsF = client.query(Paginate(Union(Match(StringV("arcane"), Ref("indexes/spells_by_element")), Match(StringV("fire"), Ref("indexes/spells_by_element")))).withEvents(true).build());
    Value unionEventsResponse = unionEventsF.get();
    Page unionEventsPage = unionEventsResponse.asPage();
    assertThat(unionEventsPage.data().size(), greaterThanOrEqualTo(2));
    ImmutableList.Builder<Ref> unionEventsRefsBuilder = ImmutableList.builder();
    for (Value unionEventsNode : unionEventsPage.data()) {
      Event event = unionEventsNode.asEvent();
      if (event.action().contentEquals("create")) {
        unionEventsRefsBuilder.add(event.resource());
      }
    }
    assertThat(unionEventsRefsBuilder.build(), hasItems(createInstance1.ref(), createInstance2.ref()));

    ListenableFuture<Value> intersectionF = client.query(Paginate(Intersection(Match(StringV("arcane"), Ref("indexes/spells_by_element")), Match(StringV("nature"), Ref("indexes/spells_by_element")))).build());
    Value intersectionResponse = intersectionF.get();
    Page intersectionPage = intersectionResponse.asPage();
    assertThat(intersectionPage.data().size(), greaterThanOrEqualTo(1));
    ImmutableList.Builder<Ref> intersectionRefsBuilder = ImmutableList.builder();
    for (Value intersectionNode : intersectionPage.data()) {
      intersectionRefsBuilder.add(intersectionNode.asRef());
    }
    assertThat(intersectionRefsBuilder.build(), hasItem(createInstance3.ref()));

    ListenableFuture<Value> differenceF = client.query(Paginate(Difference(Match(StringV("nature"), Ref("indexes/spells_by_element")), Match(StringV("arcane"), Ref("indexes/spells_by_element")))).build());
    Value differenceResponse = differenceF.get();
    Page differencePage = differenceResponse.asPage();
    assertThat(differencePage.data().size(), greaterThanOrEqualTo(1));
    ImmutableList.Builder<Ref> differenceRefsBuilder = ImmutableList.builder();
    for (Value differenceNode : differencePage.data()) {
      differenceRefsBuilder.add(differenceNode.asRef());
    }
    assertThat(differenceRefsBuilder.build(), hasItem(createInstance4.ref()));
    assertThat(differenceRefsBuilder.build(), not(hasItem(createInstance3.ref())));

    ListenableFuture<Value> joinF = client.query(Paginate(Join(Match(createCharR.ref(), Ref("indexes/spellbooks_by_owner")), Lambda("spellbook", Match(Var("spellbook"), Ref("indexes/spells_by_spellbook"))))).build());
    Page joinR = joinF.get().asPage();

    assertThat(joinR.data().size(), is(2));
    ImmutableList.Builder<Ref> joinRefsBuilder = ImmutableList.builder();
    for (Value joinNode : joinR.data()) {
      joinRefsBuilder.add(joinNode.asRef());
    }
    assertThat(joinRefsBuilder.build(), hasItems(createSpellbookSpell1R.ref(), createSpellbookSpell2R.ref()));
  }

  @Test
  public void testMiscFunctions() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<Value> equalsF = client.query(Equals(StringV("fire"), StringV("fire")));
    Value equalsR = equalsF.get();
    assertThat(equalsR.asBoolean(), is(true));

    ListenableFuture<Value> concatF = client.query(Concat(StringV("Magic"), StringV("Missile")));
    Value concatR = concatF.get();
    assertThat(concatR.asString(), is("MagicMissile"));

    ListenableFuture<Value> concat2F = client.query(Concat(ImmutableList.<Value>of(StringV("Magic"), StringV("Missile")), " "));
    Value concat2R = concat2F.get();
    assertThat(concat2R.asString(), is("Magic Missile"));

    ListenableFuture<Value> containsF = client.query(Contains(Path(Path.Object("favorites"), Path.Object("foods")), Quote(ObjectV("favorites", ObjectV("foods", ArrayV(StringV("crunchings"), StringV("munchings")))))));
    Value containsR = containsF.get();
    assertThat(containsR.asBoolean(), is(true));

    ListenableFuture<Value> selectF = client.query(Select(Path(Path.Object("favorites"), Path.Object("foods"), Path.Array(1)),
      Quote(ObjectV("favorites", ObjectV("foods", ArrayV(StringV("crunchings"), StringV("munchings"), StringV("lunchings")))))));
    Value selectNode = selectF.get();
    assertThat(selectNode.asString(), is("munchings"));

    ListenableFuture<Value> addF = client.query(Add(LongV(100), LongV(10)));
    Value addR = addF.get();
    assertThat(addR.asLong(), is(110L));

    ListenableFuture<Value> multiplyF = client.query(Multiply(LongV(100), LongV(10)));
    Value multiplyR = multiplyF.get();
    assertThat(multiplyR.asLong(), is(1000L));

    ListenableFuture<Value> subtractF = client.query(Subtract(LongV(100), LongV(10)));
    Value subtractR = subtractF.get();
    assertThat(subtractR.asLong(), is(90L));

    ListenableFuture<Value> divideF = client.query(Divide(LongV(100), LongV(10)));
    Value divideR = divideF.get();
    assertThat(divideR.asLong(), is(10L));

    ListenableFuture<Value> moduloF = client.query(Modulo(LongV(101), LongV(10)));
    Value moduloR = moduloF.get();
    assertThat(moduloR.asLong(), is(1L));

    ListenableFuture<Value> andF = client.query(And(ImmutableList.<Value>of(BooleanV(true), BooleanV(false))));
    Value andR = andF.get();
    assertThat(andR.asBoolean(), is(false));

    ListenableFuture<Value> orF = client.query(Or(ImmutableList.<Value>of(BooleanV(true), BooleanV(false))));
    Value orR = orF.get();
    assertThat(orR.asBoolean(), is(true));

    ListenableFuture<Value> notF = client.query(Not(BooleanV(false)));
    Value notR = notF.get();
    assertThat(notR.asBoolean(), is(true));
  }

  @Test
  public void testDateTimeFunctions() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<Value> timeF = client.query(Time(StringV("1970-01-01T00:00:00-04:00")));
    Value timeR = timeF.get();
    assertThat(timeR.asTs(), is(Instant.EPOCH.plus(4, ChronoUnit.HOURS)));

    ListenableFuture<Value> epochF = client.query(Epoch(LongV(30), TimeUnit.SECOND));
    Value epochR = epochF.get();
    assertThat(epochR.asTs(), is(Instant.EPOCH.plus(30, ChronoUnit.SECONDS)));

    ListenableFuture<Value> dateF = client.query(Date(StringV("1970-01-02")));
    Value dateR = dateF.get();
    assertThat(dateR.asDate(), is(LocalDate.ofEpochDay(1)));
  }

  @Test
  public void testAuthenticationFunctions() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<Value> createF = client.query(Create(Ref("classes/spells"), Quote(ObjectV("credentials", ObjectV("password", StringV("abcdefg"))))));
    Instance createR = createF.get().asInstance();

    ListenableFuture<Value> loginF = client.query(Login(createR.ref(), Quote(ObjectV("password", StringV("abcdefg")))));
    Token loginR = loginF.get().asToken();

    FaunaClient sessionClient = FaunaClient.create(Connection.builder().withFaunaRoot(config.get("root_url")).withAuthToken(loginR.secret()).build());
    ListenableFuture<Value> logoutF = sessionClient.query(Logout(false));
    Boolean logoutR = logoutF.get().asBoolean();
    assertThat(logoutR, is(true));

    ListenableFuture<Value> identifyF = client.query(Identify(createR.ref(), StringV("abcdefgfd")));
    Boolean identifyR = identifyF.get().asBoolean();
    assertThat(identifyR, is(false));

    sessionClient.close();


  }
}
