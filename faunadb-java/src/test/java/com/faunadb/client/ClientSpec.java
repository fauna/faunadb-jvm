package com.faunadb.client;

import com.faunadb.client.errors.BadRequestException;
import com.faunadb.client.errors.NotFoundException;
import com.faunadb.client.errors.PermissionDeniedException;
import com.faunadb.client.errors.UnauthorizedException;
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.*;
import com.faunadb.client.types.Value.*;
import io.netty.util.ResourceLeakDetector;
import org.hamcrest.collection.IsMapWithSize;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.Calendar;

import static com.faunadb.client.query.Language.Action.CREATE;
import static com.faunadb.client.query.Language.Action.DELETE;
import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.query.Language.Class;
import static com.faunadb.client.query.Language.TimeUnit.*;
import static com.faunadb.client.types.Codec.*;
import static com.faunadb.client.types.Value.NullV.NULL;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.assertThat;

public class ClientSpec {

  private static final String ROOT_TOKEN = EnvVariables.require("FAUNA_ROOT_KEY");
  private static final String ROOT_URL = format("%s://%s:%s",
    EnvVariables.getOrElse("FAUNA_SCHEME", "https"),
    EnvVariables.getOrElse("FAUNA_DOMAIN", "db.fauna.com"),
    EnvVariables.getOrElse("FAUNA_PORT", "443")
  );

  private static final String DB_NAME = "faunadb-java-test-" + new Random().nextLong();
  private static final Expr DB_REF = Database(DB_NAME);

  private static FaunaClient rootClient;
  private static FaunaClient serverClient;
  private static FaunaClient adminClient;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final Field<Value> DATA = Field.at("data");
  private static final Field<Spell> SPELL_FIELD = DATA.to(Spell.class);
  private static final Field<Long> TS_FIELD = Field.at("ts").to(LONG);
  private static final Field<RefV> REF_FIELD = Field.at("ref").to(REF);
  private static final Field<RefV> DOCUMENT_FIELD = Field.at("document").to(REF);
  private static final Field<List<RefV>> REF_LIST = DATA.collect(Field.as(REF));

  private static final Field<String> NAME_FIELD = DATA.at(Field.at("name")).to(STRING);
  private static final Field<String> ELEMENT_FIELD = DATA.at(Field.at("element")).to(STRING);
  private static final Field<Value> ELEMENTS_LIST = DATA.at(Field.at("elements"));
  private static final Field<Long> COST_FIELD = DATA.at(Field.at("cost")).to(LONG);
  private static final Field<String> SECRET_FIELD = Field.at("secret").to(STRING);

  private static RefV magicMissile;
  private static RefV fireball;
  private static RefV faerieFire;
  private static RefV summon;
  private static RefV thor;
  private static RefV thorSpell1;
  private static RefV thorSpell2;

  private static boolean initialized = false;

  private static Calendar cal = Calendar.getInstance();
  private static Expr nowStr = Time(cal.toInstant().toString());

  private static Value handleBadRequest(Value v, Throwable ex) {
    if (ex instanceof BadRequestException) {
      return NULL;
    } else {
      return v;
    }
  }

  @BeforeClass
  public static void setUpClient() throws Exception {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

    rootClient = createFaunaClient(ROOT_TOKEN);

    rootClient.query(Delete(DB_REF)).handle((v, ex) -> handleBadRequest(v, ex)).get();
    rootClient.query(CreateDatabase(Obj("name", Value(DB_NAME)))).get();

    Value serverKey = rootClient.query(CreateKey(Obj("database", DB_REF, "role", Value("server")))).get();
    Value adminKey = rootClient.query(CreateKey(Obj("database", DB_REF, "role", Value("admin")))).get();

    serverClient = rootClient.newSessionClient(serverKey.get(SECRET_FIELD));
    adminClient = rootClient.newSessionClient(adminKey.get(SECRET_FIELD));
  }

  @AfterClass
  public static void closeClients() throws Exception {
    rootClient.query(Delete(DB_REF)).handle((v, ex) -> handleBadRequest(v, ex)).get();
    rootClient.close();
    serverClient.close();
    adminClient.close();
  }

  @Before
  public void setUpSchema() throws Exception {
    if (initialized)
      return;

    initialized = true;

    query(Arrays.asList(
      CreateCollection(Obj("name", Value("spells"))),
      CreateCollection(Obj("name", Value("characters"))),
      CreateCollection(Obj("name", Value("spellbooks")))
    )).get();

    query(Arrays.asList(
      CreateIndex(Obj(
        "name", Value("all_spells"),
        "active", Value(true),
        "source", Collection("spells")
      )),

      CreateIndex(Obj(
        "name", Value("spells_by_element"),
        "active", Value(true),
        "source", Collection("spells"),
        "terms", Arr(Obj("field", Arr(Value("data"), Value("element"))))
      )),

      CreateIndex(Obj(
        "name", Value("elements_of_spells"),
        "active", Value(true),
        "source", Collection("spells"),
        "values", Arr(Obj("field", Arr(Value("data"), Value("element"))))
      )),

      CreateIndex(Obj(
        "name", Value("spellbooks_by_owner"),
        "active", Value(true),
        "source", Collection("spellbooks"),
        "terms", Arr(Obj("field", Arr(Value("data"), Value("owner"))))
      )),

      CreateIndex(Obj(
        "name", Value("spells_by_spellbook"),
        "active", Value(true),
        "source", Collection("spells"),
        "terms", Arr(Obj("field", Arr(Value("data"), Value("spellbook"))))
      ))
    )).get();

    magicMissile = query(
      Create(Collection("spells"),
        Obj("data",
          Obj(
            "name", Value("Magic Missile"),
            "element", Value("arcane"),
            "cost", Value(10))))
    ).get().get(REF_FIELD);

    fireball = query(
      Create(Collection("spells"),
        Obj("data",
          Obj(
            "name", Value("Fireball"),
            "element", Value("fire"),
            "cost", Value(10))))
    ).get().get(REF_FIELD);

    faerieFire = query(
      Create(Collection("spells"),
        Obj("data",
          Obj(
            "name", Value("Faerie Fire"),
            "cost", Value(10),
            "element", Arr(
              Value("arcane"),
              Value("nature")
            ))))
    ).get().get(REF_FIELD);

    summon = query(
      Create(Collection("spells"),
        Obj("data",
          Obj(
            "name", Value("Summon Animal Companion"),
            "element", Value("nature"),
            "cost", Value(10))))
    ).get().get(REF_FIELD);

    thor = query(
      Create(Collection("characters"),
        Obj("data", Obj("name", Value("Thor"))))
    ).get().get(REF_FIELD);

    RefV thorsSpellbook = query(
      Create(Collection("spellbooks"),
        Obj("data",
          Obj("owner", thor)))
    ).get().get(REF_FIELD);

    thorSpell1 = query(
      Create(Collection("spells"),
        Obj("data",
          Obj("spellbook", thorsSpellbook)))
    ).get().get(REF_FIELD);

    thorSpell2 = query(
      Create(Collection("spells"),
        Obj("data",
          Obj("spellbook", thorsSpellbook)))
    ).get().get(REF_FIELD);
  }

  @Test
  public void shouldThrowNotFoundWhenInstanceDoesntExists() throws Exception {
    thrown.expectCause(isA(NotFoundException.class));
    query(Get(Ref(Collection("spells"), "1234"))).get();
  }

  @Test
  public void shouldAbort() throws Exception {
    thrown.expectCause(isA(BadRequestException.class));
    thrown.expectMessage(containsString("transaction aborted: a message"));
    query(Abort("a message")).get();
  }

  @Test
  public void shouldCreateAComplexInstance() throws Exception {
    Value instance = query(
      Create(onARandomCollection(),
        Obj("data",
          Obj("testField",
            Obj(
              "array", Arr(
                Value(1),
                Value("2"),
                Value(3.4),
                Obj("name", Value("JR"))),
              "bool", Value(true),
              "num", Value(1234),
              "string", Value("sup"),
              "float", Value(1.234))
          )))
    ).get();

    Value testField = instance.get(DATA).at("testField");
    assertThat(testField.at("string").to(STRING).get(), equalTo("sup"));
    assertThat(testField.at("num").to(LONG).get(), equalTo(1234L));
    assertThat(testField.at("bool").to(BOOLEAN).get(), is(true));
    assertThat(testField.at("bool").to(STRING).getOptional(), is(Optional.<String>empty()));
    assertThat(testField.at("credentials").to(VALUE).getOptional(), is(Optional.<Value>empty()));
    assertThat(testField.at("credentials", "password").to(STRING).getOptional(), is(Optional.<String>empty()));

    Value array = testField.at("array");
    assertThat(array.to(ARRAY).get(), hasSize(4));
    assertThat(array.at(0).to(LONG).get(), equalTo(1L));
    assertThat(array.at(1).to(STRING).get(), equalTo("2"));
    assertThat(array.at(2).to(DOUBLE).get(), equalTo(3.4));
    assertThat(array.at(3).at("name").to(STRING).get(), equalTo("JR"));
    assertThat(array.at(4).to(VALUE).getOptional(), is(Optional.<Value>empty()));
  }

  @Test
  public void shouldParseComplexIndex() throws Exception {
    query(CreateClass(Obj("name", Value("reservations")))).get();

    CompletableFuture<Value> indexF = query(CreateIndex(Obj(
      "name", Value("reservations_by_lastName"),
      "source", Obj(
        "class", Class("reservations"),
        "fields", Obj(
          "cfLastName", Query(Lambda("x", Casefold(Select(Path("data", "guestInfo", "lastName"), Var("x"))))),
          "fActive", Query(Lambda("x", Select(Path("data", "active"), Var("x"))))
        )
      ),
      "terms", Arr(Obj("binding", Value("cfLastName")), Obj("binding", Value("fActive"))),
      "values", Arr(
        Obj("field", Arr(Value("data"), Value("checkIn"))),
        Obj("field", Arr(Value("data"), Value("checkOut"))),
        Obj("field", Arr(Value("ref")))
      ),
      "active", Value(true)
    )));

    Value index = indexF.get();
    assertThat(index.at("name").to(String.class).get(), equalTo("reservations_by_lastName"));
  }

  @Test
  public void shouldBeAbleToGetAnInstance() throws Exception {
    Value instance = query(Get(magicMissile)).get();
    assertThat(instance.get(NAME_FIELD), equalTo("Magic Missile"));
  }

  @Test
  public void shouldBeAbleToIssueABatchedQuery() throws Exception {
    List<Value> results = query(Arrays.asList(
      Get(magicMissile),
      Get(thor)
    )).get();

    assertThat(results, hasSize(2));
    assertThat(results.get(0).get(NAME_FIELD), equalTo("Magic Missile"));
    assertThat(results.get(1).get(NAME_FIELD), equalTo("Thor"));

    Map<String, Value> k1 = new HashMap<>();
    k1.put("k1", new StringV("v1"));

    Map<String, Value> k2 = new HashMap<>();
    k2.put("k2", new StringV("v2"));

    List<Value> data = query(Arrays.asList(
            new ObjectV(k1),
            new ObjectV(k2)
    )).get();

    assertThat(data, hasSize(2));
    assertThat(data.get(0).at("k1").to(STRING).get(), equalTo("v1"));
    assertThat(data.get(1).at("k2").to(STRING).get(), equalTo("v2"));
  }

  @Test
  public void shouldBeAbleToUpdateAnInstancesData() throws Exception {
    Value createdInstance = query(
      Create(onARandomCollection(),
        Obj("data",
          Obj(
            "name", Value("Magic Missile"),
            "element", Value("arcane"),
            "cost", Value(10))))
    ).get();

    Value updatedInstance = query(
      Update(createdInstance.get(REF_FIELD),
        Obj("data",
          Obj(
            "name", Value("Faerie Fire"),
            "cost", Null())))
    ).get();

    assertThat(updatedInstance.get(REF_FIELD), equalTo(createdInstance.get(REF_FIELD)));
    assertThat(updatedInstance.get(NAME_FIELD), equalTo("Faerie Fire"));
    assertThat(updatedInstance.get(ELEMENT_FIELD), equalTo("arcane"));
    assertThat(updatedInstance.getOptional(COST_FIELD), is(Optional.<Long>empty()));
  }

  @Test
  public void shouldBeAbleToReplaceAnInstancesData() throws Exception {
    Value createdInstance = query(
      Create(onARandomCollection(),
        Obj("data",
          Obj(
            "name", Value("Magic Missile"),
            "element", Value("arcane"),
            "cost", Value(10))))
    ).get();

    Value replacedInstance = query(
      Replace(createdInstance.get(REF_FIELD),
        Obj("data",
          Obj("name", Value("Volcano"),
            "elements", Arr(Value("fire"), Value("earth")),
            "cost", Value(10))))
    ).get();

    assertThat(replacedInstance.get(REF_FIELD), equalTo(createdInstance.get(REF_FIELD)));
    assertThat(replacedInstance.get(NAME_FIELD), equalTo("Volcano"));
    assertThat(replacedInstance.get(COST_FIELD), equalTo(10L));
    assertThat(replacedInstance.get(ELEMENTS_LIST).collect(Field.as(STRING)),
      contains("fire", "earth"));
  }

  @Test
  public void shouldBeAbleToDeleteAnInstance() throws Exception {
    Value createdInstance = query(
      Create(onARandomCollection(),
        Obj("data", Obj("name", Value("Magic Missile"))))
    ).get();

    Value ref = createdInstance.get(REF_FIELD);
    query(Delete(ref)).get();

    Value exists = query(Exists(ref)).get();
    assertThat(exists.to(BOOLEAN).get(), is(false));

    thrown.expectCause(isA(NotFoundException.class));
    query(Get(ref)).get();
  }

  @Test
  public void shouldBeAbleToInsertAndRemoveEvents() throws Exception {
    Value createdInstance = query(
      Create(onARandomCollection(),
        Obj("data", Obj("name", Value("Magic Missile"))))
    ).get();

    Value insertedEvent = query(
      Insert(createdInstance.get(REF_FIELD), Value(1L), CREATE,
        Obj("data",
          Obj("cooldown", Value(5L))))
    ).get();

    assertThat(insertedEvent.get(DOCUMENT_FIELD), equalTo(createdInstance.get(REF_FIELD)));

    Value removedEvent = query(
      Remove(createdInstance.get(REF_FIELD), Value(2L), DELETE)
    ).get();

    assertThat(removedEvent, nullValue());
  }

  static class Event {
    @FaunaField
    public String action;
    @FaunaField
    public RefV document;
  }

  @Test
  public void shouldTestEvents() throws Exception {
    RefV ref = query(
      Create(onARandomCollection(), Obj("data", Obj("x", Value(1))))
    ).get().get(REF_FIELD);

    query(
      Update(ref, Obj("data", Obj("x", Value(2))))
    ).get();

    query(
      Delete(ref)
    ).get();

    Value data = query(
      Paginate(Events(ref))
    ).get().get(DATA);

    List<Event> events = new ArrayList<>(data.asCollectionOf(Event.class).get());

    assertThat(events, hasSize(3));

    assertThat(events.get(0).action, equalTo("create"));
    assertThat(events.get(0).document, equalTo(ref));

    assertThat(events.get(1).action, equalTo("update"));
    assertThat(events.get(1).document, equalTo(ref));

    assertThat(events.get(2).action, equalTo("delete"));
    assertThat(events.get(2).document, equalTo(ref));
  }

  @Test
  public void shouldTestSingleton() throws Exception {
    RefV ref = query(
      Create(onARandomCollection(), Obj("data", Obj("x", Value(1))))
    ).get().get(REF_FIELD);

    query(
      Update(ref, Obj("data", Obj("x", Value(2))))
    ).get();

    query(
      Delete(ref)
    ).get();

    Value data = query(
      Paginate(Events(Singleton(ref)))
    ).get().get(DATA);

    List<Event> events = new ArrayList<>(data.asCollectionOf(Event.class).get());

    assertThat(events, hasSize(2));

    assertThat(events.get(0).action, equalTo("add"));
    assertThat(events.get(0).document, equalTo(ref));

    assertThat(events.get(1).action, equalTo("remove"));
    assertThat(events.get(1).document, equalTo(ref));
  }

  @Test
  public void shouldHandleConstraintViolations() throws Exception {
    RefV collectionRef = onARandomCollection();

    query(
      CreateIndex(
        Obj(
          "name", Value(randomStartingWith("collection_index_")),
          "active", Value(true),
          "source", collectionRef,
          "terms", Arr(Obj("field", Arr(Value("data"), Value("uniqueField")))),
          "unique", Value(true)
        ))
    ).get();

    query(
      Create(collectionRef,
        Obj("data", Obj("uniqueField", Value("same value"))))
    ).get();

    thrown.expectCause(isA(BadRequestException.class));
    query(
      Create(collectionRef,
        Obj("data", Obj("uniqueField", Value("same value"))))
    ).get();
  }

  @Test
  public void shouldFindASingleInstanceFromIndex() throws Exception {
    Value singleMatch = query(
      Paginate(Match(Index("spells_by_element"), Value("fire")))
    ).get();

    assertThat(singleMatch.get(REF_LIST), contains(fireball));
  }

  @Test
  public void shouldListAllItensOnACollectionIndex() throws Exception {
    Value allInstances = query(
      Paginate(Match(Index("all_spells")))
    ).get();

    assertThat(allInstances.get(REF_LIST),
      contains(magicMissile, fireball, faerieFire, summon, thorSpell1, thorSpell2));
  }

  @Test
  public void shouldPaginateOverAnIndex() throws Exception {
    Value page1 = query(
      Paginate(Match(Index("all_spells")))
        .size(3)
    ).get();

    assertThat(page1.get(DATA).to(ARRAY).get(), hasSize(3));
    assertThat(page1.at("after"), notNullValue());
    assertThat(page1.at("before").to(VALUE).getOptional(), is(Optional.<Value>empty()));

    Value page2 = query(
      Paginate(Match(Index("all_spells")))
        .after(page1.at("after"))
        .size(3)
    ).get();

    assertThat(page2.get(DATA).to(ARRAY).get(), hasSize(3));
    assertThat(page2.get(DATA), not(page1.at("data")));
    assertThat(page2.at("before"), notNullValue());
    assertThat(page2.at("after").to(VALUE).getOptional(), is(Optional.<Value>empty()));
  }

  @Test
  public void shouldDealWithSetRef() throws Exception {
    Value res = query(
      Match(
        Index("spells_by_element"),
        Value("arcane"))
    ).get();

    Map<String, Value> set = res.to(SET_REF).get().parameters();
    assertThat(set.get("terms").to(STRING).get(), equalTo("arcane"));
    assertThat(set.get("match").to(REF).get(),
      equalTo(new RefV("spells_by_element", Native.INDEXES)));
  }

  @Test
  public void shouldEvalLetExpression() throws Exception {
    Value res = query(
      Let(
        "x", Value(1),
        "y", Value(2)
      ).in(
        Arr(Var("y"), Var("x"))
      )
    ).get();

    assertThat(res.collect(Field.as(LONG)), contains(2L, 1L));
  }

  @Test
  public void shouldEvalIfExpression() throws Exception {
    Value res = query(
      If(Value(true),
        Value("was true"),
        Value("was false"))
    ).get();

    assertThat(res.to(STRING).get(), equalTo("was true"));
  }

  @Test
  public void shouldEvalDoExpression() throws Exception {
    RefV ref = new RefV(randomStartingWith(), onARandomCollection());

    Value res = query(
      Do(
        Create(ref, Obj("data", Obj("name", Value("Magic Missile")))),
        Get(ref)
      )
    ).get();

    assertThat(res.get(REF_FIELD), equalTo(ref));
  }

  @Test
  public void shouldEchoAnObjectBack() throws Exception {
    Value res = query(
      Obj("name", Value("Hen Wen"), "age", Value(123))
    ).get();

    assertThat(res.at("name").to(STRING).get(), equalTo("Hen Wen"));
    assertThat(res.at("age").to(LONG).get(), equalTo(123L));

    res = query(res).get();
    assertThat(res.at("name").to(STRING).get(), equalTo("Hen Wen"));
    assertThat(res.at("age").to(LONG).get(), equalTo(123L));
  }

  @Test
  public void shouldMapOverCollections() throws Exception {
    Value res = query(
      Map(
        Arr(
          Value(1), Value(2), Value(3)),
        Lambda(Value("i"),
          Add(Var("i"), Value(1)))
      )
    ).get();

    assertThat(res.collect(Field.as(LONG)), contains(2L, 3L, 4L));
  }

  @Test
  public void shouldMapOverCollectionsLambda() throws Exception {
      Value res = query(
        Map(
          Arr(Value(1), Value(2), Value(3)),
          i -> Add(i, Value(1)))).get();

    assertThat(res.collect(Field.as(LONG)), contains(2L, 3L, 4L));
  }

  @Test
  public void shouldExecuteForeachExpression() throws Exception {
    Value res = query(
      Foreach(
        Arr(
          Value("Fireball Level 1"),
          Value("Fireball Level 2")),
        Lambda(Value("spell"),
          Create(onARandomCollection(),
            Obj("data", Obj("name", Var("spell")))))
      )
    ).get();

    assertThat(res.collect(Field.as(STRING)),
      contains("Fireball Level 1", "Fireball Level 2"));
  }

  @Test
  public void shouldForeachWithLambda() throws Exception {
    Value cls = onARandomCollection();
    Value res = query(
      Foreach(
        Arr(
          Value("Fireball Level 1"),
          Value("Fireball Level 2")),
        spell -> Create(cls, Obj("data", Obj("name", spell))))
    ).get();

    assertThat(res.collect(Field.as(STRING)),
      contains("Fireball Level 1", "Fireball Level 2"));
  }

  @Test
  public void shouldFilterACollection() throws Exception {
    Value filtered = query(
      Filter(
        Arr(Value(1), Value(2), Value(3)),
        Lambda(Value("i"),
          Equals(
            Value(0),
            Modulo(Var("i"), Value(2)))
        )
      )).get();

    assertThat(filtered.collect(Field.as(LONG)), contains(2L));
  }

  @Test
  public void shouldFilterACollectionLambda() throws Exception {
    Value filtered = query(
      Filter(
        Arr(Value(1), Value(2), Value(3)),
        i -> Equals(Value(0), Modulo(i, Value(2)))
      )).get();

    assertThat(filtered.collect(Field.as(LONG)), contains(2L));
  }

  @Test
  public void shouldTakeElementsFromCollection() throws Exception {
    Value taken = query(Take(Value(2), Arr(Value(1), Value(2), Value(3)))).get();
    assertThat(taken.collect(Field.as(LONG)), contains(1L, 2L));
  }

  @Test
  public void shouldDropElementsFromCollection() throws Exception {
    Value dropped = query(Drop(Value(2), Arr(Value(1), Value(2), Value(3)))).get();
    assertThat(dropped.collect(Field.as(LONG)), contains(3L));
  }

  @Test
  public void shouldPrependElementsInACollection() throws Exception {
    Value prepended = query(
      Prepend(
        Arr(Value(1), Value(2)),
        Arr(Value(3), Value(4))
      )
    ).get();

    assertThat(prepended.collect(Field.as(LONG)),
      contains(1L, 2L, 3L, 4L));
  }

  @Test
  public void shouldAppendElementsInACollection() throws Exception {
    Value appended = query(
      Append(
        Arr(Value(3), Value(4)),
        Arr(Value(1), Value(2))
      )
    ).get();

    assertThat(appended.collect(Field.as(LONG)),
      contains(1L, 2L, 3L, 4L));
  }

  @Test
  public void shouldTestCollectionPredicatesForArrays() throws Exception {
    assertThat(
      query(IsEmpty(Arr())).get().to(BOOLEAN).get(),
      is(true)
    );

    assertThat(
      query(IsEmpty(Arr(Value(1), Value(2)))).get().to(BOOLEAN).get(),
      is(false)
    );

    assertThat(
      query(IsNonEmpty(Arr())).get().to(BOOLEAN).get(),
      is(false)
    );

    assertThat(
      query(IsNonEmpty(Arr(Value(1), Value(2)))).get().to(BOOLEAN).get(),
      is(true)
    );
  }

  @Test
  public void shouldTestCollectionPredicatesForPages() throws Exception {
    String randomElement = randomStartingWith("element-");
    Value created = query(Create(Collection("spells"), Obj("data", Obj("name", Value("predicate test"), "element", Value(randomElement))))).get();

    assertThat(
      query(IsEmpty(Paginate(Match(Index("spells_by_element"), Value("invalid element"))))).get().to(BOOLEAN).get(),
      is(true)
    );

    assertThat(
      query(IsEmpty(Paginate(Match(Index("spells_by_element"), Value(randomElement))))).get().to(BOOLEAN).get(),
      is(false)
    );

    assertThat(
      query(IsNonEmpty(Paginate(Match(Index("spells_by_element"), Value("invalid element"))))).get().to(BOOLEAN).get(),
      is(false)
    );

    assertThat(
      query(IsNonEmpty(Paginate(Match(Index("spells_by_element"), Value(randomElement))))).get().to(BOOLEAN).get(),
      is(true)
    );

    query(Delete(Select(Value("ref"), created))).get();
  }

  @Test
  public void shouldReadEventsFromIndex() throws Exception {
    Value events = query(
      Paginate(Match(Index("spells_by_element"), Value("arcane")))
        .events(true)
    ).get();

    assertThat(events.get(DATA).collect(DOCUMENT_FIELD),
      contains(magicMissile, faerieFire));
  }

  @Test
  public void shouldPaginateUnion() throws Exception {
    Value union = query(
      Paginate(
        Union(
          Match(Index("spells_by_element"), Value("arcane")),
          Match(Index("spells_by_element"), Value("fire")))
      )
    ).get();

    assertThat(union.get(REF_LIST),
      contains(magicMissile, fireball, faerieFire));
  }

  @Test
  public void shouldPaginateIntersection() throws Exception {
    Value intersection = query(
      Paginate(
        Intersection(
          Match(Index("spells_by_element"), Value("arcane")),
          Match(Index("spells_by_element"), Value("nature"))
        ))
    ).get();

    assertThat(intersection.get(REF_LIST),
      contains(faerieFire));
  }

  @Test
  public void shouldPaginateDifference() throws Exception {
    Value difference = query(
      Paginate(
        Difference(
          Match(Index("spells_by_element"), Value("nature")),
          Match(Index("spells_by_element"), Value("arcane"))
        ))
    ).get();

    assertThat(difference.get(REF_LIST),
      contains(summon));
  }

  @Test
  public void shouldPaginateDistinctSets() throws Exception {
    Value distinct = query(
      Paginate(
        Distinct(
          Match(Index("elements_of_spells")))
      )
    ).get();

    assertThat(distinct.get(DATA).collect(Field.as(STRING)),
      contains("arcane", "fire", "nature"));
  }

  @Test
  public void shouldPaginateJoin() throws Exception {
    Value join = query(
      Paginate(
        Join(
          Match(Index("spellbooks_by_owner"), thor),
          Lambda(Value("spellbook"),
            Match(Index("spells_by_spellbook"), Var("spellbook")))
        ))
    ).get();

    assertThat(join.get(REF_LIST),
      contains(thorSpell1, thorSpell2));
  }

  @Test
  public void shouldPaginateLambdaJoin() throws Exception {
      Value join = query(
        Paginate(
          Join(
               Match(Index("spellbooks_by_owner"), thor),
               spellbook -> Match(Index("spells_by_spellbook"), spellbook)))).get();

      assertThat(join.get(REF_LIST), contains(thorSpell1, thorSpell2));
  }

  @Test
  public void shouldEvalEqualsExpression() throws Exception {
    Value equals = query(Equals(Value("fire"), Value("fire"))).get();
    assertThat(equals.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalConcatExpression() throws Exception {
    Value simpleConcat = query(Concat(Arr(Value("Magic"), Value("Missile")))).get();
    assertThat(simpleConcat.to(STRING).get(), equalTo("MagicMissile"));

    Value concatWithSeparator = query(
      Concat(
        Arr(
          Value("Magic"),
          Value("Missile")
        ),
        Value(" ")
      )).get();

    assertThat(concatWithSeparator.to(STRING).get(), equalTo("Magic Missile"));
  }

  @Test
  public void shouldEvalCasefoldExpression() throws Exception {
    Value res = query(Casefold(Value("Hen Wen"))).get();
    assertThat(res.to(STRING).get(), equalTo("hen wen"));

    // https://unicode.org/reports/tr15/
    assertThat(
      query(Casefold(Value("\u212B"), Normalizer.NFD)).get().to(STRING).get(),
      equalTo("A\u030A")
    );

    assertThat(
      query(Casefold(Value("\u212B"), Normalizer.NFC)).get().to(STRING).get(),
      equalTo("\u00C5")
    );

    assertThat(
      query(Casefold(Value("\u1E9B\u0323"), Normalizer.NFKD)).get().to(STRING).get(),
      equalTo("\u0073\u0323\u0307")
    );

    assertThat(
      query(Casefold(Value("\u1E9B\u0323"), Normalizer.NFKC)).get().to(STRING).get(),
      equalTo("\u1E69")
    );

    assertThat(
      query(Casefold(Value("\u212B"), Normalizer.NFKCCaseFold)).get().to(STRING).get(),
      equalTo("\u00E5")
    );
  }

  @Test
  public void shouldEvalFindStrExpression() throws Exception {
    Value res = query(FindStr(Value("fire and  Ice"), Value("fire"))).get();
    assertThat(res.to(LONG).get(), equalTo(0L));
    Value res1 = query(FindStr("hello TO aLL", "TO")).get();
    assertThat(res1.to(LONG).get(), equalTo(6L));
    Value res2 = query(FindStr("hello TO aLL", "aLL", 6)).get();
    assertThat(res2.to(LONG).get(), equalTo(9L));
  }

  @Test
  public void shouldEvalLengthExpression() throws Exception {
    Value res = query(Length(Value("fire and  Ice"))).get();
    assertThat(res.to(LONG).get(), equalTo(13L));
    Value res1 = query(Length("hello")).get();
    assertThat(res1.to(LONG).get(), equalTo(5L));
    Value res2 = query(Length("")).get();
    assertThat(res2.to(LONG).get(), equalTo(0L));
  }

  @Test
  public void shouldEvalLowerExpression() throws Exception {
    Value res = query(LowerCase(Value("fire and  Ice"))).get();
    assertThat(res.to(STRING).get(), equalTo("fire and  ice"));
    Value res1 = query(LowerCase("hello TO aLL")).get();
    assertThat(res1.to(STRING).get(), equalTo("hello to all"));
  }

  @Test
  public void shouldEvalLTrimExpression() throws Exception {
    Value res0 = query(LTrim("   fire")).get();
    assertThat(res0.to(STRING).get(), equalTo("fire"));
    Value res = query(LTrim(Value("\t\tfire and ice"))).get();
    assertThat(res.to(STRING).get(), equalTo("fire and ice"));
    Value res1 = query(LTrim("\n\n\nhello to all")).get();
    assertThat(res1.to(STRING).get(), equalTo("hello to all"));
  }

  @Test
  public void shouldEvalNGramExpression() throws Exception {
    assertThat(
      query(NGram(Value("what"))).get().asCollectionOf(String.class).get(),
      contains("w", "wh", "h", "ha", "a", "at", "t")
    );
    assertThat(
      query(NGram(Value("what"), Value(2), Value(3))).get().asCollectionOf(String.class).get(),
      contains("wh", "wha", "ha", "hat", "at")
    );

    assertThat(
      query(NGram(Arr(Value("john"), Value("doe")))).get().asCollectionOf(String.class).get(),
      contains("j", "jo", "o", "oh", "h", "hn", "n", "d", "do", "o", "oe", "e")
    );
    assertThat(
      query(NGram(Arr(Value("john"), Value("doe")), Value(3), Value(4))).get().asCollectionOf(String.class).get(),
      contains("joh", "john", "ohn", "doe")
    );
  }

  @Test
  public void shouldEvalRepeatExpression() throws Exception {
    Value res = query(Repeat("f")).get();
    assertThat(res.to(STRING).get(), equalTo("ff"));
    Value res1 = query(Repeat("abc",3)).get();
    assertThat(res1.to(STRING).get(), equalTo("abcabcabc"));
  }

 @Test
  public void shouldEvalReplaceStringExpression() throws Exception {
    Value res = query(ReplaceStr("fire and ice and everything nice","and","or")).get();
    assertThat(res.to(STRING).get(), equalTo("fire or ice or everything nice"));
  }

  @Test
  public void shouldEvalReplaceStrRegexExpression() throws Exception {
    Value res = query(ReplaceStrRegex("fire and ice and everything nice","and","or")).get();
    assertThat(res.to(STRING).get(), equalTo("fire or ice or everything nice"));
    Value res1 = query(ReplaceStrRegex("fire and ice and everything nice","and","or",true)).get();
    assertThat(res1.to(STRING).get(), equalTo("fire or ice and everything nice"));
  }

  @Test
  public void shouldEvalRTrimExpression() throws Exception {
    Value res = query(RTrim("fire   ")).get();
    assertThat(res.to(STRING).get(), equalTo("fire"));
    Value res1 = query(RTrim("ice\t\t\t")).get();
    assertThat(res1.to(STRING).get(), equalTo("ice"));
    Value res2 = query(RTrim("nice\n\n\n")).get();
    assertThat(res2.to(STRING).get(), equalTo("nice"));
  }

  @Test
  public void shouldEvalSpaceExpression() throws Exception {
    Value res = query(Space(1)).get();
    assertThat(res.to(STRING).get(), equalTo(" "));
    Value res1 = query(Space(4)).get();
    assertThat(res1.to(STRING).get(), equalTo("    "));
  }

  @Test
  public void shouldEvalSubStringExpression() throws Exception {
    Value res = query(SubString("basketball")).get();
    assertThat(res.to(STRING).get(), equalTo("basketball"));
    Value res1 = query(SubString("basketball", 6)).get();
    assertThat(res1.to(STRING).get(), equalTo("ball"));
    Value res2 = query(SubString("basketball", 6,2)).get();
    assertThat(res2.to(STRING).get(), equalTo("ba"));
  }

  @Test
  public void shouldEvalTrimExpression() throws Exception {
    Value res = query(Trim("   fire   ")).get();
    assertThat(res.to(STRING).get(), equalTo("fire"));
    Value res1 = query(Trim("\t\t\tice\t\t\t")).get();
    assertThat(res1.to(STRING).get(), equalTo("ice"));
    Value res2 = query(Trim("\n\n\nnice\n\n\n")).get();
    assertThat(res2.to(STRING).get(), equalTo("nice"));
  }

  @Test
  public void shouldEvalUpperExpression() throws Exception {
    Value res = query(UpperCase(Value("fire and  Ice"))).get();
    assertThat(res.to(STRING).get(), equalTo("FIRE AND  ICE"));
    Value res1 = query(UpperCase("hello TO aLL")).get();
    assertThat(res1.to(STRING).get(), equalTo("HELLO TO ALL"));
  }

  @Test
  public void shouldEvalTitleCaseExpression() throws Exception {
    Value res = query(TitleCase(Value("fire and  Ice"))).get();
    assertThat(res.to(STRING).get(), equalTo("Fire And  Ice"));
    Value res1 = query(TitleCase("hello TO aLL")).get();
    assertThat(res1.to(STRING).get(), equalTo("Hello To All"));
  }

  @Test
  public void shouldEvalFormatExpression() throws Exception {
    assertThat(
      query(Format("%3$s%1$s %2$s", Value("DB"), Value("rocks"), Value("Fauna"))).get().to(String.class).get(),
      equalTo("FaunaDB rocks")
    );
  }

  @Test
  public void shouldEvalContainsExpression() throws Exception {
    Value contains = query(
      Contains(
        Path("favorites", "foods"),
        Obj("favorites",
          Obj("foods",
            Arr(Value("crunchings"), Value("munchings")))))
    ).get();

    assertThat(contains.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalSelectExpression() throws Exception {
    Value selected = query(
      Select(
        Path("favorites", "foods").at(1),
        Obj("favorites",
          Obj("foods",
            Arr(
              Value("crunchings"),
              Value("munchings"),
              Value("lunchings")))))
    ).get();

    assertThat(selected.to(STRING).get(), equalTo("munchings"));
  }

  @Test
  public void shouldEvalSelectAllExpression() throws Exception {
    Result<Collection<String>> selected1 = query(
      SelectAll(
        Value("foo"),
        Arr(
          Obj("foo", Value("bar")),
          Obj("foo", Value("baz"))
        )
      )
    ).get().asCollectionOf(String.class);

    assertThat(selected1.get(), contains("bar", "baz"));

    Result<Collection<Integer>> selected2 = query(
      SelectAll(
        Path("foo", "bar"),
        Arr(
          Obj("foo", Obj("bar", Value(1))),
          Obj("foo", Obj("bar", Value(2)))
        )
      )
    ).get().asCollectionOf(Integer.class);

    assertThat(selected2.get(), contains(1, 2));

    Result<Collection<Integer>> selected3 = query(
      SelectAll(
        Path("foo").at(0),
        Arr(
          Obj("foo", Arr(Value(0), Value(1))),
          Obj("foo", Arr(Value(2), Value(3)))
        )
      )
    ).get().asCollectionOf(Integer.class);

    assertThat(selected3.get(), contains(0, 2));
  }

  @Test
  public void shouldEvalLTExpression() throws Exception {
    Value res = query(LT(Arr(Value(1), Value(2), Value(3)))).get();
    assertThat(res.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalLTEExpression() throws Exception {
    Value res = query(LTE(Arr(Value(1), Value(2), Value(2)))).get();
    assertThat(res.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalGTxpression() throws Exception {
    Value res = query(GT(Arr(Value(3), Value(2), Value(1)))).get();
    assertThat(res.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalGTExpression() throws Exception {
    Value res = query(GTE(Arr(Value(3), Value(2), Value(2)))).get();
    assertThat(res.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalAbs() throws Exception {
    Value res = query(Abs(Value(-100))).get();
    assertThat(res.to(LONG).get(), equalTo(100L));
  }

  @Test
  public void shouldEvalAcos() throws Exception {
    Value res = query(Trunc(Acos(Value(0.5)),Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(1.04D));
  }

  @Test
  public void shouldEvalAddExpression() throws Exception {
    Value res = query(Add(Value(100), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(110L));
  }

  @Test
  public void shouldEvalAsin() throws Exception {
    Value res = query(Trunc(Asin(Value(0.5)),Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(0.52D));
  }

  @Test
  public void shouldEvalAtan() throws Exception {
    Value res = query(Trunc(Atan(Value(0.5)),Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(0.46D));
  }

  @Test
  public void shouldEvalBitAnd() throws Exception {
    Value res = query(BitAnd(Value(7), Value(3))).get();
    assertThat(res.to(LONG).get(), equalTo(3L));
  }

  @Test
  public void shouldEvalBitNot() throws Exception {
    Value res = query(BitNot(Value(3))).get();
    assertThat(res.to(LONG).get(), equalTo(-4L));
  }

  @Test
  public void shouldEvalBitOr() throws Exception {
    Value res = query(BitOr(Value(6), Value(3))).get();
    assertThat(res.to(LONG).get(), equalTo(7L));
  }

  @Test
  public void shouldEvalBitXOr() throws Exception {
    Value res = query(BitXor(Value(2), Value(1))).get();
    assertThat(res.to(LONG).get(), equalTo(3L));
  }

  @Test
  public void shouldEvalCeil() throws Exception {
    Value res = query(Ceil(Value(1.01))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(2.0D));
  }

  @Test
  public void shouldEvalCos() throws Exception {
    Value res = query(Trunc(Cos(Value(0.5)),Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo( 0.87));
  }

  @Test
  public void shouldEvalCosh() throws Exception {
    Value res = query(Trunc(Cosh(Value(0.5)),Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo( 1.12));
  }

  @Test
  public void shouldEvalDegrees() throws Exception {
    Value res = query(Trunc(Degrees(Value(2.0)),Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo( 114.59));
  }

  @Test
  public void shouldEvalDivideExpression() throws Exception {
    Value res = query(Divide(Value(100), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(10L));
  }

  @Test
  public void shouldEvalExp() throws Exception {
    Value res = query(Trunc(Exp(Value(2)), Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(7.38D));
  }

  @Test
  public void shouldEvalFloor() throws Exception {
    Value res = query(Floor(Value(1.91))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(1.0D));
  }

  @Test
  public void shouldEvalHypot() throws Exception {
    Value res = query(Hypot(Value(3), Value(4))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(5.0D));
  }

  @Test
  public void shouldEvalLn() throws Exception {
    Value res = query(Trunc(Ln(Value(2)), Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(0.69D));
  }

  @Test
  public void shouldEvalLog() throws Exception {
    Value res = query(Trunc(Log(Value(2)), Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(0.30D));
  }

  @Test
  public void shouldEvalMax() throws Exception {
    Value res = query(Max(Value(101), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(101L));
  }

  @Test
  public void shouldEvalMin() throws Exception {
    Value res = query(Min(Value(101), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(10L));
  }

  @Test
  public void shouldEvalModuloExpression() throws Exception {
    Value res = query(Modulo(Value(101), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(1L));
  }

  @Test
  public void shouldEvalMultiplyExpression() throws Exception {
    Value res = query(Multiply(Value(100), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(1000L));
  }

  @Test
  public void shouldEvalRadians() throws Exception {
    Value res = query(Trunc(Radians(Value(500.0D)))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(8.72D));
  }

  @Test
  public void shouldEvalRound() throws Exception {
    Value res = query(Round(Value(123.666D),Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(123.67D));
  }

  @Test
  public void shouldEvalSign() throws Exception {
    Value res = query(Sign(Value(1))).get();
    assertThat(res.to(LONG).get(), equalTo(1L));
  }

  @Test
  public void shouldEvalSin() throws Exception {
    Value res = query(Trunc(Sin(Value(0.5)),Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(0.47D));
  }

  @Test
  public void shouldEvalSqrt() throws Exception {
    Value res = query(Sqrt(Value(16))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(4.0D));
  }

  @Test
  public void shouldEvalSubtractExpression() throws Exception {
    Value res = query(Subtract(Value(100), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(90L));
  }

  @Test
  public void shouldEvalTan() throws Exception {
    Value res = query(Trunc(Tan(Value(0.5)),Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(0.54D));
  }

  @Test
  public void shouldEvalTrunc() throws Exception {
    Value res = query(Trunc(Value(123.456D),Value(2))).get();
    assertThat(res.to(DOUBLE).get(), equalTo(123.45D));
  }

  @Test
  public void shouldEvalAndExpression() throws Exception {
    Value res = query(And(Value(true), Value(false))).get();
    assertThat(res.to(BOOLEAN).get(), is(false));
  }

  @Test
  public void shouldEvalOrExpression() throws Exception {
    Value res = query(Or(Value(true), Value(false))).get();
    assertThat(res.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalNotExpression() throws Exception {
    Value notR = query(Not(Value(false))).get();
    assertThat(notR.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalTimeExpression() throws Exception {
    Value res = query(Time(Value("1970-01-01T00:00:00-04:00"))).get();
    assertThat(res.to(TIME).get(), equalTo(Instant.ofEpochMilli(0).plus(4, ChronoUnit.HOURS)));
  }

  @Test
  public void shouldEvalToStringExpression() throws Exception {
    Value res = query(ToString(Value(100))).get();
    assertThat(res.to(STRING).get(), equalTo("100"));
  }

  @Test
  public void shouldEvalToNumberExpression() throws Exception {
    Value res1 = query(ToNumber(Value("100"))).get();
    assertThat(res1.to(LONG).get(), equalTo(100L));

    Value res2 = query(ToNumber(Value("3.14"))).get();
    assertThat(res2.to(DOUBLE).get(), equalTo(3.14));
  }

  @Test
  public void shouldEvalToTimeExpression() throws Exception {
    Value res = query(ToTime(Value("1970-01-01T00:00:00Z"))).get();
    assertThat(res.to(TIME).get(), equalTo(Instant.ofEpochMilli(0)));
  }

  @Test
  public void shouldEvalToSecondsExpression() throws Exception {
    Value res = query(ToSeconds(ToTime(Value("1970-01-01T00:00:00Z")))).get();
    assertThat(res.to(LONG).get(), equalTo(0L));
  }

  @Test
  public void shouldEvalToMillisExpression() throws Exception {
    Value res = query(ToMillis(nowStr)).get();
    long expected = cal.getTimeInMillis();
    assertThat(res.to(LONG).get(), equalTo(expected));
  }

  @Test
  public void shouldEvalToMicrosExpression() throws Exception {
    Value res = query(ToMicros(Epoch(1552733214259L, SECOND))).get();
    assertThat(res.to(LONG).get(), equalTo(1552733214259000000L));
  }

  @Test
  public void shouldEvalDayOfYearExpression() throws Exception {
    Value res = query(DayOfYear(nowStr)).get();
    long expected = cal.get(Calendar.DAY_OF_YEAR);
    assertThat(res.to(LONG).get(), equalTo(expected));
  }

  @Test
  public void shouldEvalDayOfMonthExpression() throws Exception {
    Value res = query(DayOfMonth(nowStr)).get();
    long expected = cal.get(Calendar.DAY_OF_MONTH);
    assertThat(res.to(LONG).get(), equalTo(expected));
  }

  @Test
  public void shouldEvalDayOfWeekExpression() throws Exception {
    Value res = query(DayOfWeek(nowStr)).get();
    long expected = cal.get(Calendar.DAY_OF_WEEK) - 1;
    assertThat(res.to(LONG).get(), equalTo(expected));
  }

  @Test
  public void shouldEvalYearExpression() throws Exception {
    Value res = query(Year(nowStr)).get();
    long expected = cal.get(Calendar.YEAR);
    assertThat(res.to(LONG).get(), equalTo(expected));
  }

  @Test
  public void shouldEvalMonthExpression() throws Exception {
    Value res = query(Month(nowStr)).get();
    long expected = cal.get(Calendar.MONTH) + 1;
    assertThat(res.to(LONG).get(), equalTo(expected));
  }

  @Test
  public void shouldEvalHourExpression() throws Exception {
    Value res = query(Hour(Epoch(0, SECOND))).get();
    assertThat(res.to(LONG).get(), equalTo(0L));
  }

  @Test
  public void shouldEvalMinuteExpression() throws Exception {
    Value res = query(Minute(nowStr)).get();
    long expected = cal.get(Calendar.MINUTE);
    assertThat(res.to(LONG).get(), equalTo(expected));
  }

  @Test
  public void shouldEvalSecondExpression() throws Exception {
    Value res = query(Second(nowStr)).get();
    long expected = cal.get(Calendar.SECOND);
    assertThat(res.to(LONG).get(), equalTo(expected));
  }

  @Test
  public void shouldEvalToDateExpression() throws Exception {
    Value res = query(ToDate(Value("1970-01-01"))).get();
    assertThat(res.to(DATE).get(), equalTo(LocalDate.ofEpochDay(0)));
  }

  @Test
  public void shouldEvalEpochExpression() throws Exception {
    List<Value> res = query(Arrays.asList(
      Epoch(Value(30), SECOND),
      Epoch(Value(30), MILLISECOND),
      Epoch(Value(30), MICROSECOND),
      Epoch(Value(30), NANOSECOND)
    )).get();

    assertThat(res.get(0).to(TIME).get(), equalTo(Instant.ofEpochMilli(0).plus(30, ChronoUnit.SECONDS)));
    assertThat(res.get(1).to(TIME).get(), equalTo(Instant.ofEpochMilli(30)));
    assertThat(res.get(2).to(TIME).get(), equalTo(Instant.ofEpochMilli(0).plus(30, ChronoUnit.MICROS)));
    assertThat(res.get(3).to(TIME).get(), equalTo(Instant.ofEpochMilli(0).plus(30, ChronoUnit.NANOS)));
  }

  @Test
  public void shouldEvalDateExpression() throws Exception {
    Value res = query(Date(Value("1970-01-02"))).get();
    assertThat(res.to(DATE).get(), equalTo(LocalDate.ofEpochDay(1)));
  }

  @Test
  public void shouldGetNewId() throws Exception {
    Value res = query(NewId()).get();
    assertThat(res.to(STRING).get(), notNullValue());
  }

  @Test
  public void shouldPaginateAtTimestamp() throws Exception {
    Value summonData = query(Get(summon)).get();

    long beforeSummon = summonData.get(TS_FIELD) - 1;

    Value allSpells = query(
      At(Value(beforeSummon), Paginate(Match(Index(Value("all_spells")))))
    ).get();

    assertThat(allSpells.get(REF_LIST),
      contains(magicMissile, fireball, faerieFire));
  }

  @Test
  public void shouldEchoBytes() throws Exception {
    Value bytes = query(Value(new byte[] {0x1, 0x2, 0x3, 0x4})).get();

    assertThat(bytes, equalTo(Value(new byte[] {0x1, 0x2, 0x3, 0x4})));
  }

  @Test
  public void shouldEchoQuery() throws Exception {
    Value query = query(Query(Lambda(Arr(Value("x"), Value("y")), Concat(Arr(Var("x"), Var("y")), Value("/"))))).get();
    Value echoed = query(query).get();

    assertThat(query, equalTo(echoed));
  }

  @Test
  public void shouldCreateFunction() throws Exception {
    String functionName = randomStartingWith();

    Expr lambda = Lambda(Arr(Value("x"), Value("y")), Concat(Arr(Var("x"), Var("y")), Value("/")));
    query(CreateFunction(Obj("name", Value(functionName), "body", Query(lambda)))).get();

    assertThat(query(Exists(Function(functionName))).get(), equalTo(BooleanV.TRUE));
  }

  @Test
  public void shouldCallFunction() throws Exception {
    Expr lambda = Lambda(Arr(Value("x"), Value("y")), Concat(Arr(Var("x"), Var("y")), Value("/")));
    query(CreateFunction(Obj("name", Value("concat_with_slash"), "body", Query(lambda)))).get();

    assertThat(
      query(Call(Function("concat_with_slash"), Value("a"), Value("b"))).get(),
      equalTo(Value("a/b"))
    );
  }

  @Test
  public void shouldCreateRole() throws Exception {
    adminClient.query(CreateRole(Obj(
      "name", Value("a_role"),
      "privileges", Obj(
        "resource", Databases(),
        "actions", Obj("read", Value(true))
      )
    ))).get();

    assertThat(adminClient.query(Exists(Role("a_role"))).get(), equalTo(BooleanV.TRUE));
  }

  static class Spell {
    @FaunaField
    private final String name;
    @FaunaField
    private final String element;
    @FaunaField
    private final Integer cost;

    @FaunaConstructor
    Spell(@FaunaField("name") String name, @FaunaField("element") String element, @FaunaField("cost") Integer cost) {
      this.name = name;
      this.element = element;
      this.cost = cost;
    }
  }

  @Test
  public void shouldCreateASpellUsingEncoderDecoder() throws Exception {
    RefV ref = query(
      Create(Collection("spells"),
        Obj("data", Value(new Spell("Blah", "blah", 10)))
      )).get().get(REF_FIELD);


    Spell spell = query(Get(ref)).get().get(SPELL_FIELD);

    assertThat(spell.name, equalTo("Blah"));
    assertThat(spell.element, equalTo("blah"));
    assertThat(spell.cost, equalTo(10));

    query(Delete(ref)).get();
  }

  @Test
  public void shouldTestReferences() throws Exception {
    assertThat(
      query(Index("all_spells")).get(),
      equalTo(new RefV("all_spells", Native.INDEXES))
    );

    assertThat(
      query(Collection("spells")).get(),
      equalTo(new RefV("spells", Native.COLLECTIONS))
    );

    assertThat(
      query(Database("faunadb-database")).get(),
      equalTo(new RefV("faunadb-database", Native.DATABASES))
    );

    assertThat(
      query(new RefV("1234567890", Native.KEYS)).get(),
      equalTo(new RefV("1234567890", Native.KEYS))
    );

    assertThat(
      query(Function("function_name")).get(),
      equalTo(new RefV("function_name", Native.FUNCTIONS))
    );

    assertThat(
        query(Role("role_name")).get(),
        equalTo(new RefV("role_name", Native.ROLES))
    );

    assertThat(
      query(Ref(Collection("spells"), Value("1234567890"))).get(),
      equalTo(new RefV("1234567890", new RefV("spells", Native.COLLECTIONS)))
    );

    assertThat(
      query(Ref(Collection("spells"), "1234567890")).get(),
      equalTo(new RefV("1234567890", new RefV("spells", Native.COLLECTIONS)))
    );
  }

  @Test
  public void shouldCreateNestedRefFromString() throws Exception {
    assertThat(
      query(Ref("collections/widget/123")).get(),
      equalTo(new RefV("123", new RefV("widget", Native.COLLECTIONS)))
    );
  }

  @Test
  public void shouldNotBreakDoWithOneElement() throws Exception {
    assertThat(
      query(Do(Value(1))).get().to(Long.class).get(),
      equalTo(1L)
    );

    assertThat(
      query(Do(Value(1), Value(2))).get().to(Long.class).get(),
      equalTo(2L)
    );
  }


  @Test
  public void shouldThrowUnauthorizedOnInvalidSecret() throws Exception {
    thrown.expectCause(isA(UnauthorizedException.class));

    createFaunaClient("invalid-secret")
            .query(Get(Ref(Collection("spells"), "1234")))
            .get();
  }

  @Test
  public void shouldThrowPermissionDeniedException() throws Exception {
    thrown.expectCause(isA(PermissionDeniedException.class));

    Value key = rootClient.query(CreateKey(Obj("database", DB_REF, "role", Value("client")))).get();

    FaunaClient client = createFaunaClient(key.get(SECRET_FIELD));

    client.query(Paginate(Databases())).get();
  }

  @Test
  public void shouldAuthenticateSession() throws Exception {
    Value createdInstance = serverClient.query(
            Create(onARandomCollection(),
                    Obj("credentials",
                            Obj("password", Value("abcdefg"))))
    ).get();

    Value auth = serverClient.query(
            Login(
                    createdInstance.get(REF_FIELD),
                    Obj("password", Value("abcdefg")))
    ).get();

    String secret = auth.get(SECRET_FIELD);

    try (FaunaClient sessionClient = serverClient.newSessionClient(secret)) {
      Value loggedOut = sessionClient.query(Logout(Value(true))).get();
      assertThat(loggedOut.to(BOOLEAN).get(), is(true));
    }

    Value identified = serverClient.query(
            Identify(
                    createdInstance.get(REF_FIELD),
                    Value("wrong-password")
            )
    ).get();

    assertThat(identified.to(BOOLEAN).get(), is(false));
  }

  @Test
  public void shouldTestHasIdentity() throws Exception {
    Value createdInstance = serverClient.query(
            Create(onARandomCollection(),
                    Obj("credentials",
                            Obj("password", Value("sekret"))))
    ).get();

    Value auth = serverClient.query(
            Login(
                    createdInstance.get(REF_FIELD),
                    Obj("password", Value("sekret")))
    ).get();

    String secret = auth.get(SECRET_FIELD);

    try (FaunaClient sessionClient = serverClient.newSessionClient(secret)) {
      assertThat(
              sessionClient.query(HasIdentity()).get().to(BOOLEAN).get(),
              equalTo(true)
      );
    }
  }

  @Test
  public void shouldTestIdentity() throws Exception {
    Value createdInstance = serverClient.query(
            Create(onARandomCollection(),
                    Obj("credentials",
                            Obj("password", Value("sekret"))))
    ).get();

    Value auth = serverClient.query(
            Login(
                    createdInstance.get(REF_FIELD),
                    Obj("password", Value("sekret")))
    ).get();

    String secret = auth.get(SECRET_FIELD);

    try (FaunaClient sessionClient = serverClient.newSessionClient(secret)) {
      assertThat(
              sessionClient.query(Identity()).get(),
              equalTo(createdInstance.get(REF_FIELD))
      );
    }
  }

  @Test
  public void shouldGetKeyFromSecret() throws Exception {
    Value key = rootClient.query(
            CreateKey(Obj("database", DB_REF, "role", Value("server")))
    ).get();

    Value secret = key.at("secret");

    assertThat(rootClient.query(Get(key.get(REF_FIELD))).get(),
            equalTo(rootClient.query(KeyFromSecret(secret)).get()));
  }

  @Test
  public void shouldTestNestedReferences() throws Exception {
    FaunaClient parentClient = createNewDatabase(adminClient, "parent-database");
    FaunaClient childClient = createNewDatabase(parentClient, "child-database");

    childClient.query(
      CreateCollection(Obj(
        "name", Value("a_collection")
      ))
    ).get();

    childClient.query(
      CreateRole(Obj(
        "name", Value("a_role"),
        "privileges", Obj(
          "resource", Databases(),
          "actions", Obj("read", Value(true))
        )
      ))
    ).get();

    Expr nestedDatabase = Database("child-database", Database("parent-database"));
    Expr nestedCollectionRef = Collection("a_collection", nestedDatabase);
    Expr nestedRoleRef = Role("a_role", nestedDatabase);

    assertThat(adminClient.query(Exists(nestedCollectionRef)).get(), equalTo(BooleanV.TRUE));
    assertThat(adminClient.query(Exists(nestedRoleRef)).get(), equalTo(BooleanV.TRUE));

    RefV childDBRef =
      new RefV("child-database", Native.DATABASES,
        new RefV("parent-database", Native.DATABASES));

    List<RefV> refs = adminClient.query(
      Paginate(Union(
        Collections(nestedDatabase),
        Roles(nestedDatabase)
      ))
    ).get().get(REF_LIST);

    assertThat(refs, hasSize(2));
    assertThat(refs, containsInAnyOrder(
      new RefV("a_collection", Native.COLLECTIONS, childDBRef),
      new RefV("a_role", Native.ROLES, childDBRef)
    ));
  }

  @Test
  public void shouldTestNestedKeys() throws Exception {
    FaunaClient client = createNewDatabase(adminClient, "db-for-keys");

    client.query(CreateDatabase(Obj("name", Value("db-test")))).get();

    Value serverKey = client.query(CreateKey(Obj("database", Database("db-test"), "role", Value("server")))).get().get(REF_FIELD);
    Value adminKey = client.query(CreateKey(Obj("database", Database("db-test"), "role", Value("admin")))).get().get(REF_FIELD);

    assertThat(
            client.query(Paginate(Keys())).get().get(DATA).to(ARRAY).get(),
            hasItems(serverKey, adminKey)
    );

    assertThat(
            adminClient.query(Paginate(Keys(Database("db-for-keys")))).get().get(DATA).to(ARRAY).get(),
            hasItems(serverKey, adminKey)
    );
  }

  @Test
  public void shouldAllowForScopedKeys() throws Exception {
    FaunaClient parentClient = createNewDatabase(adminClient, "scoped-database");
    FaunaClient childClient = createNewDatabase(parentClient, "child-database1");
    createNewDatabase(childClient, "child-database2");

    Value key = adminClient.query(
      CreateKey(Obj(
        "database", Database("scoped-database"),
        "role", Value("admin")
      ))
    ).get();

    String secret = key.at("secret").get(String.class);
    String scopedSecret = secret + ":child-database1/child-database2:admin";
    FaunaClient scopedClient = adminClient.newSessionClient(scopedSecret);

    try {
      scopedClient.query(
        CreateCollection(Obj(
          "name", Value("foo")
        ))
      ).get();
    } finally {
      scopedClient.close();
    }

    Value collectionsPage =
      adminClient.query(
        Paginate(
          Collections(
            Database("child-database2",
              Database("child-database1",
                Database("scoped-database")))))
      ).get();

    Collection<Value> collections =
      collectionsPage
        .at("data")
        .collect(Value.class);

    assertThat(collections, hasSize(1));
  }

  @Test
  public void shouldTestMerge() throws Exception {
    //empty object
    assertThat(
      query(
        Merge(
          Obj(),
          Obj("x", Value(10), "y", Value(20))
        )
      ).get().asMapOf(int.class).get(),
      allOf(aMapWithSize(2), hasEntry("x", 10), hasEntry("y", 20))
    );

    //adds field
    assertThat(
      query(
        Merge(
          Obj("x", Value(10), "y", Value(20)),
          Obj("z", Value(30))
        )
      ).get().asMapOf(int.class).get(),
      allOf(aMapWithSize(3), hasEntry("x", 10), hasEntry("y", 20), hasEntry("z", 30))
    );

    //replace field
    assertThat(
      query(
        Merge(
          Obj("x", Value(10), "y", Value(20), "z", Value(-1)),
          Obj("z", Value(30))
        )
      ).get().asMapOf(int.class).get(),
      allOf(aMapWithSize(3), hasEntry("x", 10), hasEntry("y", 20), hasEntry("z", 30))
    );

    //remove field
    assertThat(
      query(
        Merge(
          Obj("x", Value(10), "y", Value(20), "z", Value(-1)),
          Obj("z", Null())
        )
      ).get().asMapOf(int.class).get(),
      allOf(aMapWithSize(2), hasEntry("x", 10), hasEntry("y", 20))
    );

    //merge multiple objects
    assertThat(
      query(
        Merge(
          Obj(),
          Arr(
            Obj("x", Value(10)),
            Obj("y", Value(20)),
            Obj("z", Value(30))
          )
        )
      ).get().asMapOf(int.class).get(),
      allOf(aMapWithSize(3), hasEntry("x", 10), hasEntry("y", 20), hasEntry("z", 30))
    );

    //ignore left value
    assertThat(
      query(
        Merge(
          Obj("x", Value(10), "y", Value(20)),
          Obj("x", Value(100), "y", Value(200)),
          Lambda(Arr(Value("key"), Value("left"), Value("right")), Var("right"))
        )
      ).get().asMapOf(int.class).get(),
      allOf(aMapWithSize(2), hasEntry("x", 100), hasEntry("y", 200))
    );

    //ignore right value
    assertThat(
      query(
        Merge(
          Obj("x", Value(10), "y", Value(20)),
          Obj("x", Value(100), "y", Value(200)),
          Lambda(Arr(Value("key"), Value("left"), Value("right")), Var("left"))
        )
      ).get().asMapOf(int.class).get(),
      allOf(aMapWithSize(2), hasEntry("x", 10), hasEntry("y", 20))
    );

    //lambda 1-arity -> return [key, leftValue, rightValue]
    assertThat(
      query(
        Merge(
          Obj("x", Value(10), "y", Value(20)),
          Obj("x", Value(100), "y", Value(200)),
          Lambda("value", Var("value"))
        )
      ).get().asMapOf(ArrayList.class).get(),
      allOf(
        aMapWithSize(2),
        hasEntry("x", Arrays.<Value>asList(new StringV("x"), new LongV(10), new LongV(100))),
        hasEntry("y", Arrays.<Value>asList(new StringV("y"), new LongV(20), new LongV(200)))
      )
    );
  }

  private CompletableFuture<Value> query(Expr expr) {
    return serverClient.query(expr);
  }

  private CompletableFuture<List<Value>> query(List<? extends Expr> exprs) {
    return serverClient.query(exprs);
  }

  private FaunaClient createNewDatabase(FaunaClient client, String name) throws Exception {
    client.query(CreateDatabase(Obj("name", Value(name)))).get();
    Value key = client.query(CreateKey(Obj("database", Database(Value(name)), "role", Value("admin")))).get();
    return client.newSessionClient(key.get(SECRET_FIELD));
  }

  private RefV onARandomCollection() throws Exception {

    Value clazz = query(
      CreateCollection(
        Obj("name", Value(randomStartingWith("some_collection_"))))
    ).get();

    return clazz.get(REF_FIELD);
  }

  private String randomStartingWith(String... parts) {
    StringBuilder builder = new StringBuilder();
    for (String part : parts)
      builder.append(part);

    builder.append(Math.abs(new Random().nextLong()));
    return builder.toString();
  }

  private static FaunaClient createFaunaClient(String secret) {
    try {
      return FaunaClient.builder()
              .withEndpoint(ROOT_URL)
              .withSecret(secret)
              .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

