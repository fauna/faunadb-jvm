package com.faunadb.client;

import com.faunadb.client.dsl.DslSpec;
import com.faunadb.client.dsl.EnvVariables;
import com.faunadb.client.errors.BadRequestException;
import com.faunadb.client.errors.NotFoundException;
import com.faunadb.client.errors.UnauthorizedException;
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Field;
import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.ObjectV;
import com.faunadb.client.types.Value.RefV;
import com.faunadb.client.types.Value.StringV;
import com.faunadb.client.types.time.HighPrecisionTime;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.faunadb.client.database.SetupDatabase.*;
import static com.faunadb.client.query.Language.Action.CREATE;
import static com.faunadb.client.query.Language.Action.DELETE;
import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.query.Language.Class;
import static com.faunadb.client.query.Language.TimeUnit.*;
import static com.faunadb.client.types.Codec.*;
import static com.faunadb.client.types.Value.NullV.NULL;
import static com.google.common.base.Functions.constant;
import static com.google.common.util.concurrent.Futures.catching;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;
import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertThat;

public class JavaClientSpec extends DslSpec {
  private static final String ROOT_TOKEN = EnvVariables.require("FAUNA_ROOT_KEY");
  private static final String ROOT_URL = format("%s://%s:%s",
    EnvVariables.getOrElse("FAUNA_SCHEME", "https"),
    EnvVariables.getOrElse("FAUNA_DOMAIN", "cloud.faunadb.com"),
    EnvVariables.getOrElse("FAUNA_PORT", "443")
  );

  private static final String DB_NAME = "faunadb-java-test";
  private static final Expr DB_REF = Ref("databases/" + DB_NAME);

  private static FaunaClient rootClient;
  private static FaunaClient client;

  @BeforeClass
  public static void setUpClient() throws Exception {
    rootClient = createFaunaClient(ROOT_TOKEN);

    catching(rootClient.query(dropDatabase(DB_REF)), BadRequestException.class, constant(NULL)).get();
    Value db = rootClient.query(createDatabase(DB_NAME)).get();
    Value key = rootClient.query(createServerKey(db)).get();

    client = createClientWithServerKey(key);
  }

  @AfterClass
  public static void closeClients() throws Exception {
    catching(rootClient.query(dropDatabase(DB_REF)), BadRequestException.class, constant(NULL)).get();
    rootClient.close();
    client.close();
  }

  @Test
  public void shouldThrowUnauthorizedOnInvalidSecret() throws Exception {
    thrown.expectCause(isA(UnauthorizedException.class));

    createFaunaClient("invalid-secret")
      .query(Get(Ref(Class(Value("spells")), Value("1234"))))
      .get();
  }

  @Test
  public void shouldThrowNotFoundWhenInstanceDoesntExists() throws Exception {
    thrown.expectCause(isA(NotFoundException.class));
    client.query(Get(Ref(Class(Value("spells")), Value("1234")))).get();
  }

  @Test
  public void shouldCreateAComplexInstance() throws Exception {
    Value instance = client.query(
      Create(onARandomClass(),
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
    assertThat(testField.at("bool").to(STRING).getOptional(), is(Optional.<String>absent()));
    assertThat(testField.at("credentials").to(VALUE).getOptional(), is(Optional.<Value>absent()));
    assertThat(testField.at("credentials", "password").to(STRING).getOptional(), is(Optional.<String>absent()));

    Value array = testField.at("array");
    assertThat(array.to(ARRAY).get(), hasSize(4));
    assertThat(array.at(0).to(LONG).get(), equalTo(1L));
    assertThat(array.at(1).to(STRING).get(), equalTo("2"));
    assertThat(array.at(2).to(DOUBLE).get(), equalTo(3.4));
    assertThat(array.at(3).at("name").to(STRING).get(), equalTo("JR"));
    assertThat(array.at(4).to(VALUE).getOptional(), is(Optional.<Value>absent()));
  }

  @Test
  public void shouldBeAbleToGetAnInstance() throws Exception {
    Value instance = client.query(Get(magicMissile)).get();
    assertThat(instance.get(NAME_FIELD), equalTo("Magic Missile"));
  }

  @Test
  public void shouldBeAbleToIssueABatchedQuery() throws Exception {
    ImmutableList<Value> results = client.query(ImmutableList.of(
      Get(magicMissile),
      Get(thor)
    )).get();

    assertThat(results, hasSize(2));
    assertThat(results.get(0).get(NAME_FIELD), equalTo("Magic Missile"));
    assertThat(results.get(1).get(NAME_FIELD), equalTo("Thor"));

    ImmutableList<Value> data = client.query(ImmutableList.of(
      new ObjectV(ImmutableMap.of("k1", new StringV("v1"))),
      new ObjectV(ImmutableMap.of("k2", new StringV("v2")))
    )).get();

    assertThat(data, hasSize(2));
    assertThat(data.get(0).at("k1").to(STRING).get(), equalTo("v1"));
    assertThat(data.get(1).at("k2").to(STRING).get(), equalTo("v2"));
  }

  @Test
  public void shouldBeAbleToUpdateAnInstancesData() throws Exception {
    Value createdInstance = client.query(
      Create(onARandomClass(),
        Obj("data",
          Obj(
            "name", Value("Magic Missile"),
            "element", Value("arcane"),
            "cost", Value(10))))
    ).get();

    Value updatedInstance = client.query(
      Update(createdInstance.get(REF_FIELD),
        Obj("data",
          Obj(
            "name", Value("Faerie Fire"),
            "cost", Null())))
    ).get();

    assertThat(updatedInstance.get(REF_FIELD), equalTo(createdInstance.get(REF_FIELD)));
    assertThat(updatedInstance.get(NAME_FIELD), equalTo("Faerie Fire"));
    assertThat(updatedInstance.get(ELEMENT_FIELD), equalTo("arcane"));
    assertThat(updatedInstance.getOptional(COST_FIELD), is(Optional.<Long>absent()));
  }

  @Test
  public void shouldBeAbleToReplaceAnInstancesData() throws Exception {
    Value createdInstance = client.query(
      Create(onARandomClass(),
        Obj("data",
          Obj(
            "name", Value("Magic Missile"),
            "element", Value("arcane"),
            "cost", Value(10))))
    ).get();

    Value replacedInstance = client.query(
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
    Value createdInstance = client.query(
      Create(onARandomClass(),
        Obj("data", Obj("name", Value("Magic Missile"))))
    ).get();

    Value ref = createdInstance.get(REF_FIELD);
    client.query(Delete(ref)).get();

    Value exists = client.query(Exists(ref)).get();
    assertThat(exists.to(BOOLEAN).get(), is(false));

    thrown.expectCause(isA(NotFoundException.class));
    client.query(Get(ref)).get();
  }

  @Test
  public void shouldBeAbleToInsertAndRemoveEvents() throws Exception {
    Value createdInstance = client.query(
      Create(onARandomClass(),
        Obj("data", Obj("name", Value("Magic Missile"))))
    ).get();

    Value insertedEvent = client.query(
      Insert(createdInstance.get(REF_FIELD), Value(1L), CREATE,
        Obj("data",
          Obj("cooldown", Value(5L))))
    ).get();

    assertThat(insertedEvent.get(RESOURCE_FIELD), equalTo(createdInstance.get(REF_FIELD)));

    Value removedEvent = client.query(
      Remove(createdInstance.get(REF_FIELD), Value(2L), DELETE)
    ).get();

    assertThat(removedEvent, nullValue());
  }

  @Test
  public void shouldHandleConstraintViolations() throws Exception {
    RefV classRef = onARandomClass();

    client.query(
      CreateIndex(
        Obj(
          "name", Value(randomStartingWith("class_index_")),
          "source", classRef,
          "terms", Arr(Obj("field", Arr(Value("data"), Value("uniqueField")))),
          "unique", Value(true)
        ))
    ).get();

    client.query(
      Create(classRef,
        Obj("data", Obj("uniqueField", Value("same value"))))
    ).get();

    thrown.expectCause(isA(BadRequestException.class));
    client.query(
      Create(classRef,
        Obj("data", Obj("uniqueField", Value("same value"))))
    ).get();
  }

  @Test
  public void shouldFindASingleInstanceFromIndex() throws Exception {
    Value singleMatch = client.query(
      Paginate(Match(
        Index(Value("spells_by_element")),
        Value("fire"))
      )).get();

    assertThat(singleMatch.get(REF_LIST), contains(fireball));
  }

  @Test
  public void shouldListAllItensOnAClassIndex() throws Exception {
    Value allInstances = client.query(
      Paginate(Match(Ref("indexes/all_spells")))
    ).get();

    assertThat(allInstances.get(REF_LIST),
      contains(magicMissile, fireball, faerieFire, summon, thorSpell1, thorSpell2));
  }

  @Test
  public void shouldPaginateOverAnIndex() throws Exception {
    Value page1 = client.query(
      Paginate(Match(Ref("indexes/all_spells")))
        .size(3)
    ).get();

    assertThat(page1.get(DATA).to(ARRAY).get(), hasSize(3));
    assertThat(page1.at("after"), notNullValue());
    assertThat(page1.at("before").to(VALUE).getOptional(), is(Optional.<Value>absent()));

    Value page2 = client.query(
      Paginate(Match(Ref("indexes/all_spells")))
        .after(page1.at("after"))
        .size(3)
    ).get();

    assertThat(page2.get(DATA).to(ARRAY).get(), hasSize(3));
    assertThat(page2.get(DATA), not(page1.at("data")));
    assertThat(page2.at("before"), notNullValue());
    assertThat(page2.at("after").to(VALUE).getOptional(), is(Optional.<Value>absent()));
  }

  @Test
  public void shouldDealWithSetRef() throws Exception {
    Value res = client.query(
      Match(
        Ref("indexes/spells_by_element"),
        Value("arcane"))
    ).get();

    ImmutableMap<String, Value> set = res.to(SET_REF).get().parameters();
    assertThat(set.get("terms").to(STRING).get(), equalTo("arcane"));
    assertThat(set.get("match").to(REF).get(),
      equalTo(new RefV("indexes/spells_by_element")));
  }

  @Test
  public void shouldEvalLetExpression() throws Exception {
    Value res = client.query(
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
    Value res = client.query(
      If(Value(true),
        Value("was true"),
        Value("was false"))
    ).get();

    assertThat(res.to(STRING).get(), equalTo("was true"));
  }

  @Test
  public void shouldEvalDoExpression() throws Exception {
    Expr ref = new RefV(randomStartingWith(onARandomClass().strValue(), "/"));

    Value res = client.query(
      Do(
        Create(ref, Obj("data", Obj("name", Value("Magic Missile")))),
        Get(ref)
      )
    ).get();

    assertThat(res.get(REF_FIELD), equalTo(ref));
  }

  @Test
  public void shouldEchoAnObjectBack() throws Exception {
    Value res = client.query(
      Obj("name", Value("Hen Wen"), "age", Value(123))
    ).get();

    assertThat(res.at("name").to(STRING).get(), equalTo("Hen Wen"));
    assertThat(res.at("age").to(LONG).get(), equalTo(123L));

    res = client.query(res).get();
    assertThat(res.at("name").to(STRING).get(), equalTo("Hen Wen"));
    assertThat(res.at("age").to(LONG).get(), equalTo(123L));
  }

  @Test
  public void shouldMapOverCollections() throws Exception {
    Value res = client.query(
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
  public void shouldExecuteForeachExpression() throws Exception {
    Value res = client.query(
      Foreach(
        Arr(
          Value("Fireball Level 1"),
          Value("Fireball Level 2")),
        Lambda(Value("spell"),
          Create(onARandomClass(),
            Obj("data", Obj("name", Var("spell")))))
      )
    ).get();

    assertThat(res.collect(Field.as(STRING)),
      contains("Fireball Level 1", "Fireball Level 2"));
  }

  @Test
  public void shouldFilterACollection() throws Exception {
    Value filtered = client.query(
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
  public void shouldTakeElementsFromCollection() throws Exception {
    Value taken = client.query(Take(Value(2), Arr(Value(1), Value(2), Value(3)))).get();
    assertThat(taken.collect(Field.as(LONG)), contains(1L, 2L));
  }

  @Test
  public void shouldDropElementsFromCollection() throws Exception {
    Value dropped = client.query(Drop(Value(2), Arr(Value(1), Value(2), Value(3)))).get();
    assertThat(dropped.collect(Field.as(LONG)), contains(3L));
  }

  @Test
  public void shouldPrependElementsInACollection() throws Exception {
    Value prepended = client.query(
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
    Value appended = client.query(
      Append(
        Arr(Value(3), Value(4)),
        Arr(Value(1), Value(2))
      )
    ).get();

    assertThat(appended.collect(Field.as(LONG)),
      contains(1L, 2L, 3L, 4L));
  }

  @Test
  public void shouldReadEventsFromIndex() throws Exception {
    Value events = client.query(
      Paginate(Match(Ref("indexes/spells_by_element"), Value("arcane")))
        .events(true)
    ).get();

    assertThat(events.get(DATA).collect(Field.at("resource").to(REF)),
      contains(magicMissile, faerieFire));
  }

  @Test
  public void shouldPaginateUnion() throws Exception {
    Value union = client.query(
      Paginate(
        Union(
          Match(Ref("indexes/spells_by_element"), Value("arcane")),
          Match(Ref("indexes/spells_by_element"), Value("fire")))
      )
    ).get();

    assertThat(union.get(REF_LIST),
      contains(magicMissile, fireball, faerieFire));
  }

  @Test
  public void shouldPaginateIntersection() throws Exception {
    Value intersection = client.query(
      Paginate(
        Intersection(
          Match(Ref("indexes/spells_by_element"), Value("arcane")),
          Match(Ref("indexes/spells_by_element"), Value("nature"))
        ))
    ).get();

    assertThat(intersection.get(REF_LIST),
      contains(faerieFire));
  }

  @Test
  public void shouldPaginateDifference() throws Exception {
    Value difference = client.query(
      Paginate(
        Difference(
          Match(Ref("indexes/spells_by_element"), Value("nature")),
          Match(Ref("indexes/spells_by_element"), Value("arcane"))
        ))
    ).get();

    assertThat(difference.get(REF_LIST),
      contains(summon));
  }

  @Test
  public void shouldPaginateDistinctSets() throws Exception {
    Value distinct = client.query(
      Paginate(
        Distinct(
          Match(Ref("indexes/elements_of_spells")))
      )
    ).get();

    assertThat(distinct.get(DATA).collect(Field.as(STRING)),
      contains("arcane", "fire", "nature"));
  }

  @Test
  public void shouldPaginateJoin() throws Exception {
    Value join = client.query(
      Paginate(
        Join(
          Match(Ref("indexes/spellbooks_by_owner"), thor),
          Lambda(Value("spellbook"),
            Match(Ref("indexes/spells_by_spellbook"), Var("spellbook")))
        ))
    ).get();

    assertThat(join.get(REF_LIST),
      contains(thorSpell1, thorSpell2));
  }

  @Test
  public void shouldEvalEqualsExpression() throws Exception {
    Value equals = client.query(Equals(Value("fire"), Value("fire"))).get();
    assertThat(equals.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalConcatExpression() throws Exception {
    Value simpleConcat = client.query(Concat(Arr(Value("Magic"), Value("Missile")))).get();
    assertThat(simpleConcat.to(STRING).get(), equalTo("MagicMissile"));

    Value concatWithSeparator = client.query(
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
    Value res = client.query(Casefold(Value("Hen Wen"))).get();
    assertThat(res.to(STRING).get(), equalTo("hen wen"));
  }

  @Test
  public void shouldEvalContainsExpression() throws Exception {
    Value contains = client.query(
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
    Value selected = client.query(
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
  public void shouldEvalLTExpression() throws Exception {
    Value res = client.query(LT(Arr(Value(1), Value(2), Value(3)))).get();
    assertThat(res.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalLTEExpression() throws Exception {
    Value res = client.query(LTE(Arr(Value(1), Value(2), Value(2)))).get();
    assertThat(res.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalGTxpression() throws Exception {
    Value res = client.query(GT(Arr(Value(3), Value(2), Value(1)))).get();
    assertThat(res.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalGTExpression() throws Exception {
    Value res = client.query(GTE(Arr(Value(3), Value(2), Value(2)))).get();
    assertThat(res.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalAddExpression() throws Exception {
    Value res = client.query(Add(Value(100), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(110L));
  }

  @Test
  public void shouldEvalMultiplyExpression() throws Exception {
    Value res = client.query(Multiply(Value(100), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(1000L));
  }

  @Test
  public void shouldEvalSubtractExpression() throws Exception {
    Value res = client.query(Subtract(Value(100), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(90L));
  }

  @Test
  public void shouldEvalDivideExpression() throws Exception {
    Value res = client.query(Divide(Value(100), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(10L));
  }

  @Test
  public void shouldEvalModuloExpression() throws Exception {
    Value res = client.query(Modulo(Value(101), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(1L));
  }

  @Test
  public void shouldEvalAndExpression() throws Exception {
    Value res = client.query(And(Value(true), Value(false))).get();
    assertThat(res.to(BOOLEAN).get(), is(false));
  }

  @Test
  public void shouldEvalOrExpression() throws Exception {
    Value res = client.query(Or(Value(true), Value(false))).get();
    assertThat(res.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalNotExpression() throws Exception {
    Value notR = client.query(Not(Value(false))).get();
    assertThat(notR.to(BOOLEAN).get(), is(true));
  }

  @Test
  public void shouldEvalTimeExpression() throws Exception {
    Value res = client.query(Time(Value("1970-01-01T00:00:00-04:00"))).get();
    assertThat(res.to(TIME).get(), equalTo(new Instant(0).plus(Duration.standardHours(4))));
  }

  @Test
  public void shouldEvalEpochExpression() throws Exception {
    ImmutableList<Value> res = client.query(ImmutableList.of(
      Epoch(Value(30), SECOND),
      Epoch(Value(30), MILLISECOND),
      Epoch(Value(30), MICROSECOND),
      Epoch(Value(30), NANOSECOND)
    )).get();

    assertThat(res.get(0).to(TIME).get(), equalTo(new Instant(0).plus(Duration.standardSeconds(30))));
    assertThat(res.get(1).to(TIME).get(), equalTo(new Instant(0).plus(Duration.millis(30))));
    assertThat(res.get(2).to(HP_TIME).get(), equalTo(HighPrecisionTime.fromInstantWithMicros(new Instant(0), 30)));
    assertThat(res.get(3).to(HP_TIME).get(), equalTo(HighPrecisionTime.fromInstantWithNanos(new Instant(0), 30)));
  }

  @Test
  public void shouldOverflowOnHighPrecisionTime() throws Exception {
    ImmutableList<Value> res = client.query(ImmutableList.of(
      Epoch(Value(1001), MICROSECOND),
      Epoch(Value(1001), NANOSECOND)
    )).get();

    HighPrecisionTime micros = res.get(0).to(HP_TIME).get();
    assertThat(micros.toInstant(), equalTo(new Instant(1)));
    assertThat(micros.getMillisecondsFromEpoch(), equalTo(1L));
    assertThat(micros.getNanosecondsOffset(), equalTo(1001000L));

    HighPrecisionTime nanos = res.get(1).to(HP_TIME).get();
    assertThat(nanos.toInstant(), equalTo(new Instant(0)));
    assertThat(nanos.getMillisecondsFromEpoch(), equalTo(0L));
    assertThat(nanos.getNanosecondsOffset(), equalTo(1001L));
  }

  @Test
  public void shouldBeAbleToSortHighPrecisionTime() throws Exception {
    Value res = client.query(Arr(
      Epoch(Value(42), NANOSECOND),
      Epoch(Value(50), MILLISECOND),
      Epoch(Value(30), MICROSECOND),
      Epoch(Value(1), SECOND)
    )).get();

    HighPrecisionTime[] times = res.collect(Field.as(HP_TIME)).toArray(new HighPrecisionTime[0]);
    Arrays.sort(times);

    assertThat(times, arrayContaining(
      HighPrecisionTime.fromInstantWithNanos(new Instant(0), 42),
      HighPrecisionTime.fromInstantWithMicros(new Instant(0), 30),
      HighPrecisionTime.fromInstant(new Instant(50)),
      HighPrecisionTime.fromInstant(new Instant(1000))
    ));
  }

  @Test
  public void shouldEvalDateExpression() throws Exception {
    Value res = client.query(Date(Value("1970-01-02"))).get();
    assertThat(res.to(DATE).get(), equalTo(new LocalDate(0, UTC).plusDays(1)));
  }

  @Test
  public void shouldGetNextId() throws Exception {
    Value res = client.query(NextId()).get();
    assertThat(res.to(STRING).get(), notNullValue());
  }

  @Test
  public void shouldAuthenticateSession() throws Exception {
    Value createdInstance = client.query(
      Create(onARandomClass(),
        Obj("credentials",
          Obj("password", Value("abcdefg"))))
    ).get();

    Value auth = client.query(
      Login(
        createdInstance.get(REF_FIELD),
        Obj("password", Value("abcdefg")))
    ).get();

    String secret = auth.at("secret").to(STRING).get();

    try (FaunaClient sessionClient = client.newSessionClient(secret)) {
      Value loggedOut = sessionClient.query(Logout(Value(true))).get();
      assertThat(loggedOut.to(BOOLEAN).get(), is(true));
    }

    Value identified = client.query(
      Identify(
        createdInstance.get(REF_FIELD),
        Value("wrong-password")
      )
    ).get();

    assertThat(identified.to(BOOLEAN).get(), is(false));
  }

  @Override
  protected ListenableFuture<Value> query(Expr expr) {
    return client.query(expr);
  }

  @Override
  protected ListenableFuture<ImmutableList<Value>> query(List<? extends Expr> exprs) {
    return client.query(exprs);
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

  private static FaunaClient createClientWithServerKey(Value serverKey) {
    String secret = serverKey.at("secret").to(STRING).get();
    return rootClient.newSessionClient(secret);
  }

}
