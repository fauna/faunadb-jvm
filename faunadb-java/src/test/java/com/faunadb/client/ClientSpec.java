package com.faunadb.client;

import com.faunadb.client.errors.BadRequestException;
import com.faunadb.client.errors.NotFoundException;
import com.faunadb.client.errors.UnauthorizedException;
import com.faunadb.client.query.Expr;
import com.faunadb.client.test.FaunaDBTest;
import com.faunadb.client.types.Ref;
import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.ObjectV;
import com.faunadb.client.types.Value.StringV;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static com.faunadb.client.query.Language.Action.CREATE;
import static com.faunadb.client.query.Language.Action.DELETE;
import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.query.Language.TimeUnit.SECOND;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class ClientSpec extends FaunaDBTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static Ref magicMissile;
  private static Ref fireball;
  private static Ref faerieFire;
  private static Ref summon;
  private static Ref thor;
  private static Ref thorSpell1;
  private static Ref thorSpell2;

  @BeforeClass
  public static void setUpSchema() throws Exception {
    client.query(ImmutableList.of(
      Create(Ref("classes"), Obj("name", Value("spells"))),
      Create(Ref("classes"), Obj("name", Value("characters"))),
      Create(Ref("classes"), Obj("name", Value("spellbooks")))
    )).get();

    client.query(ImmutableList.of(
      Create(Ref("indexes"), Obj(
        "name", Value("all_spells"),
        "source", Ref("classes/spells")
      )),

      Create(Ref("indexes"), Obj(
        "name", Value("spells_by_element"),
        "source", Ref("classes/spells"),
        "terms", Arr(Obj("field", Arr(Value("data"), Value("element"))))
      )),

      Create(Ref("indexes"), Obj(
        "name", Value("elements_of_spells"),
        "source", Ref("classes/spells"),
        "values", Arr(Obj("field", Arr(Value("data"), Value("element"))))
      )),

      Create(Ref("indexes"), Obj(
        "name", Value("spellbooks_by_owner"),
        "source", Ref("classes/spellbooks"),
        "terms", Arr(Obj("field", Arr(Value("data"), Value("owner"))))
      )),

      Create(Ref("indexes"), Obj(
        "name", Value("spells_by_spellbook"),
        "source", Ref("classes/spells"),
        "terms", Arr(Obj("field", Arr(Value("data"), Value("spellbook"))))
      ))
    )).get();

    magicMissile = client.query(
      Create(Ref("classes/spells"),
        Obj("data",
          Obj(
            "name", Value("Magic Missile"),
            "element", Value("arcane"),
            "cost", Value(10))))
    ).get().get("ref").asRef();

    fireball = client.query(
      Create(Ref("classes/spells"),
        Obj("data",
          Obj(
            "name", Value("Fireball"),
            "element", Value("fire"),
            "cost", Value(10))))
    ).get().get("ref").asRef();

    faerieFire = client.query(
      Create(Ref("classes/spells"),
        Obj("data",
          Obj(
            "name", Value("Faerie Fire"),
            "cost", Value(10),
            "element", Arr(
              Value("arcane"),
              Value("nature")
            ))))
    ).get().get("ref").asRef();

    summon = client.query(
      Create(Ref("classes/spells"),
        Obj("data",
          Obj(
            "name", Value("Summon Animal Companion"),
            "element", Value("nature"),
            "cost", Value(10))))
    ).get().get("ref").asRef();

    thor = client.query(
      Create(Ref("classes/characters"),
        Obj("data", Obj("name", Value("Thor"))))
    ).get().get("ref").asRef();

    Ref thorsSpellbook = client.query(
      Create(Ref("classes/spellbooks"),
        Obj("data",
          Obj("owner", thor)))
    ).get().get("ref").asRef();

    thorSpell1 = client.query(
      Create(Ref("classes/spells"),
        Obj("data",
          Obj("spellbook", thorsSpellbook)))
    ).get().get("ref").asRef();

    thorSpell2 = client.query(
      Create(Ref("classes/spells"),
        Obj("data",
          Obj("spellbook", thorsSpellbook)))
    ).get().get("ref").asRef();
  }

  @Test
  public void shouldThrowUnauthorizedOnInvalidSecret() throws Exception {
    thrown.expectCause(isA(UnauthorizedException.class));

    createFaunaClient("invalid-secret")
      .query(Get(Ref("classes/spells/1234")))
      .get();
  }

  @Test
  public void shouldThrowNotFoundWhenInstanceDoesntExists() throws Exception {
    thrown.expectCause(isA(NotFoundException.class));
    client.query(Get(Ref("classes/spells/1234"))).get();
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

    Value testField = instance.get("data", "testField");
    assertThat(testField.get("string").asString(), equalTo("sup"));
    assertThat(testField.get("num").asLong(), equalTo(1234L));
    assertThat(testField.get("bool").asBoolean(), is(true));
    assertThat(testField.get("bool").asStringOption(), is(Optional.<String>absent()));
    assertThat(testField.getOption("credentials"), is(Optional.<Value>absent()));
    assertThat(testField.getOption("credentials", "password"), is(Optional.<Value>absent()));

    Value array = testField.get("array");
    assertThat(array.asArray(), hasSize(4));
    assertThat(array.get(0).asLong(), equalTo(1L));
    assertThat(array.get(1).asString(), equalTo("2"));
    assertThat(array.get(2).asDouble(), equalTo(3.4));
    assertThat(array.get(3).get("name").asString(), equalTo("JR"));
    assertThat(array.getOption(4), is(Optional.<Value>absent()));
  }

  @Test
  public void shouldBeAbleToGetAnInstance() throws Exception {
    Value instance = client.query(Get(magicMissile)).get();
    assertThat(instance.get("data", "name").asString(), equalTo("Magic Missile"));
  }

  @Test
  public void shouldBeAbleToIssueABatchedQuery() throws Exception {
    ImmutableList<Value> results = client.query(ImmutableList.of(
      Get(magicMissile),
      Get(thor)
    )).get();

    assertThat(results, hasSize(2));
    assertThat(results.get(0).get("data", "name").asString(), equalTo("Magic Missile"));
    assertThat(results.get(1).get("data", "name").asString(), equalTo("Thor"));

    ImmutableList<Value> data = client.query(ImmutableList.of(
      new ObjectV(ImmutableMap.of("k1", new StringV("v1"))),
      new ObjectV(ImmutableMap.of("k2", new StringV("v2")))
    )).get();

    assertThat(data, hasSize(2));
    assertThat(data.get(0).get("k1").asString(), equalTo("v1"));
    assertThat(data.get(1).get("k2").asString(), equalTo("v2"));
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
      Update(createdInstance.get("ref"),
        Obj("data",
          Obj(
            "name", Value("Faerie Fire"),
            "cost", Null())))
    ).get();

    assertThat(updatedInstance.get("ref"), equalTo(createdInstance.get("ref")));
    assertThat(updatedInstance.get("data", "name").asString(), equalTo("Faerie Fire"));
    assertThat(updatedInstance.get("data", "element").asString(), equalTo("arcane"));
    assertThat(updatedInstance.get("data", "cost"), nullValue());
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
      Replace(createdInstance.get("ref"),
        Obj("data",
          Obj("name", Value("Volcano"),
            "element", Arr(Value("fire"), Value("earth")),
            "cost", Value(10))))
    ).get();

    assertThat(replacedInstance.get("ref"), equalTo(createdInstance.get("ref")));
    assertThat(replacedInstance.get("data", "name").asString(), equalTo("Volcano"));
    assertThat(replacedInstance.get("data", "element").get(0).asString(), equalTo("fire"));
    assertThat(replacedInstance.get("data", "element").get(1).asString(), equalTo("earth"));
    assertThat(replacedInstance.get("data", "cost").asLong(), equalTo(10L));
  }

  @Test
  public void shouldBeAbleToDeleteAnInstance() throws Exception {
    Value createdInstance = client.query(
      Create(onARandomClass(),
        Obj("data", Obj("name", Value("Magic Missile"))))
    ).get();

    Value ref = createdInstance.get("ref");
    client.query(Delete(ref)).get();

    Value exists = client.query(Exists(ref)).get();
    assertThat(exists.asBoolean(), is(false));

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
      Insert(createdInstance.get("ref"), Value(1L), CREATE,
        Obj("data",
          Obj("cooldown", Value(5L))))
    ).get();

    assertThat(insertedEvent.get("ref"), equalTo(createdInstance.get("ref")));
    assertThat(insertedEvent.get("data").asObject().size(), equalTo(1));
    assertThat(insertedEvent.get("data", "cooldown").asLong(), is(5L));

    Value removedEvent = client.query(
      Remove(createdInstance.get("ref"), Value(2L), DELETE)
    ).get();

    assertThat(removedEvent, nullValue());
  }

  @Test
  public void shouldHandleConstraintViolations() throws Exception {
    Ref classRef = onARandomClass();

    client.query(
      Create(Ref("indexes"),
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
      Paginate(Match(Ref("indexes/spells_by_element"), Value("fire")))
    ).get();

    assertThat(singleMatch.get("data").asArray(), hasSize(1));
    assertThat(singleMatch.get("data").get(0).asRef(), equalTo(fireball));
  }

  @Test
  public void shouldCountElementsOnAIndex() throws Exception {
    Value count = client.query(Count(Match(Ref("indexes/all_spells")))).get();
    assertThat(count.asLong(), equalTo(6L));
  }

  @Test
  public void shouldListAllItensOnAClassIndex() throws Exception {
    Value allInstances = client.query(
      Paginate(Match(Ref("indexes/all_spells")))
    ).get();

    assertThat(allInstances.get("data").asArray(), hasSize(6));
    assertThat(allInstances.get("data").get(0).asRef(), equalTo(magicMissile));
    assertThat(allInstances.get("data").get(1).asRef(), equalTo(fireball));
    assertThat(allInstances.get("data").get(2).asRef(), equalTo(faerieFire));
    assertThat(allInstances.get("data").get(3).asRef(), equalTo(summon));
    assertThat(allInstances.get("data").get(4).asRef(), equalTo(thorSpell1));
    assertThat(allInstances.get("data").get(5).asRef(), equalTo(thorSpell2));
  }

  @Test
  public void shouldPaginateOverAnIndex() throws Exception {
    Value page1 = client.query(
      Paginate(Match(Ref("indexes/all_spells")))
        .withSize(3)
    ).get();

    assertThat(page1.get("data").asArray(), hasSize(3));
    assertThat(page1.get("after"), notNullValue());
    assertThat(page1.get("before"), nullValue());

    Value page2 = client.query(
      Paginate(Match(Ref("indexes/all_spells")))
        .withCursor(After(page1.get("after")))
        .withSize(3)
    ).get();

    assertThat(page2.get("data").asArray(), hasSize(3));
    assertThat(page2.get("data"), not(page1.get("data")));
    assertThat(page2.get("before"), notNullValue());
    assertThat(page2.get("after"), nullValue());
  }

  @Test
  public void shouldDealWithSetRef() throws Exception {
    Value res = client.query(
      Match(
        Ref("indexes/spells_by_element"),
        Value("arcane"))
    ).get();

    ImmutableMap<String, Value> set = res.asSetRef().parameters();
    assertThat(set.get("terms").asString(), equalTo("arcane"));
    assertThat(set.get("match").asRef(),
      equalTo(new Ref("indexes/spells_by_element")));
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

    assertThat(res.get(0).asLong(), equalTo(2L));
    assertThat(res.get(1).asLong(), equalTo(1L));
  }

  @Test
  public void shouldEvalIfExpression() throws Exception {
    Value res = client.query(
      If(Value(true),
        Value("was true"),
        Value("was false"))
    ).get();

    assertThat(res.asString(), equalTo("was true"));
  }

  @Test
  public void shouldEvalDoExpression() throws Exception {
    Expr ref = new Ref(randomStartingWith(onARandomClass().strValue(), "/"));

    Value res = client.query(
      Do(
        Create(ref, Obj("data", Obj("name", Value("Magic Missile")))),
        Get(ref)
      )
    ).get();

    assertThat(res.get("ref").asRef(),
      equalTo(ref));
  }

  @Test
  public void shouldEchoAnObjectBack() throws Exception {
    Value res = client.query(
      Obj("name", Value("Hen Wen"), "age", Value(123))
    ).get();

    assertThat(res.get("name").asString(), equalTo("Hen Wen"));
    assertThat(res.get("age").asLong(), equalTo(123L));

    res = client.query(res).get();
    assertThat(res.get("name").asString(), equalTo("Hen Wen"));
    assertThat(res.get("age").asLong(), equalTo(123L));
  }

  @Test
  public void shouldMapOverCollections() throws Exception {
    Value res = client.query(
      Map(
        Lambda(Value("i"),
          Add(Var("i"), Value(1))),
        Arr(
          Value(1), Value(2), Value(3))
      )
    ).get();

    assertThat(res.asArray(), hasSize(3));
    assertThat(res.get(0).asLong(), equalTo(2L));
    assertThat(res.get(1).asLong(), equalTo(3L));
    assertThat(res.get(2).asLong(), equalTo(4L));
  }

  @Test
  public void shouldExecuteForeachExpression() throws Exception {
    Value res = client.query(
      Foreach(
        Lambda(Value("spell"),
          Create(onARandomClass(),
            Obj("data", Obj("name", Var("spell"))))),
        Arr(
          Value("Fireball Level 1"),
          Value("Fireball Level 2")))
    ).get();

    assertThat(res.asArray(), hasSize(2));
    assertThat(res.get(0).asString(), equalTo("Fireball Level 1"));
    assertThat(res.get(1).asString(), equalTo("Fireball Level 2"));
  }

  @Test
  public void shouldFilterACollection() throws Exception {
    Value filtered = client.query(
      Filter(
        Lambda(Value("i"),
          Equals(
            Value(0),
            Modulo(Var("i"), Value(2)))
        ),
        Arr(Value(1), Value(2), Value(3))
      )).get();

    assertThat(filtered.asArray(), hasSize(1));
    assertThat(filtered.get(0).asLong(), equalTo(2L));
  }

  @Test
  public void shouldTakeElementsFromCollection() throws Exception {
    Value taken = client.query(Take(Value(2), Arr(Value(1), Value(2), Value(3)))).get();

    assertThat(taken.asArray(), hasSize(2));
    assertThat(taken.get(0).asLong(), equalTo(1L));
    assertThat(taken.get(1).asLong(), equalTo(2L));
  }

  @Test
  public void shouldDropElementsFromCollection() throws Exception {
    Value dropped = client.query(Drop(Value(2), Arr(Value(1), Value(2), Value(3)))).get();

    assertThat(dropped.asArray(), hasSize(1));
    assertThat(dropped.get(0).asLong(), equalTo(3L));
  }

  @Test
  public void shouldPrependElementsInACollection() throws Exception {
    Value prepended = client.query(
      Prepend(
        Arr(Value(1), Value(2)),
        Arr(Value(3), Value(4))
      )
    ).get();

    assertThat(prepended.asArray(), hasSize(4));
    assertThat(prepended.get(0).asLong(), equalTo(1L));
    assertThat(prepended.get(1).asLong(), equalTo(2L));
    assertThat(prepended.get(2).asLong(), equalTo(3L));
    assertThat(prepended.get(3).asLong(), equalTo(4L));
  }

  @Test
  public void shouldAppendElementsInACollection() throws Exception {
    Value appended = client.query(
      Append(
        Arr(Value(3), Value(4)),
        Arr(Value(1), Value(2))
      )
    ).get();

    assertThat(appended.asArray(), hasSize(4));
    assertThat(appended.get(0).asLong(), equalTo(1L));
    assertThat(appended.get(1).asLong(), equalTo(2L));
    assertThat(appended.get(2).asLong(), equalTo(3L));
    assertThat(appended.get(3).asLong(), equalTo(4L));
  }

  @Test
  public void shouldReadEventsFromIndex() throws Exception {
    Value events = client.query(
      Paginate(Match(Ref("indexes/spells_by_element"), Value("arcane")))
        .withEvents(true)
    ).get();

    assertThat(events.get("data").asArray(), hasSize(2));
    assertThat(collectResourcesFrom(events.get("data")), hasItems(magicMissile, faerieFire));
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

    assertThat(union.get("data").asArray(), hasSize(3));
    assertThat(collectRefsFom(union.get("data")), hasItems(magicMissile, faerieFire, fireball));

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

    assertThat(intersection.get("data").asArray(), hasSize(1));
    assertThat(collectRefsFom(intersection.get("data")), hasItem(faerieFire));
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

    assertThat(difference.get("data").asArray(), hasSize(1));
    assertThat(collectRefsFom(difference.get("data")), hasItem(summon));
  }

  @Test
  public void shouldPaginateDistinctSets() throws Exception {
    Value distinct = client.query(
      Paginate(
        Distinct(
          Match(Ref("indexes/elements_of_spells")))
      )
    ).get();

    assertThat(distinct.get("data").asArray(), hasSize(3));
    assertThat(distinct.get("data").get(0).asString(), equalTo("arcane"));
    assertThat(distinct.get("data").get(1).asString(), equalTo("fire"));
    assertThat(distinct.get("data").get(2).asString(), equalTo("nature"));
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

    assertThat(join.get("data").asArray(), hasSize(2));
    assertThat(collectRefsFom(join.get("data")), hasItems(thorSpell1, thorSpell2));
  }

  @Test
  public void shouldEvalEqualsExpression() throws Exception {
    Value equals = client.query(Equals(Value("fire"), Value("fire"))).get();
    assertThat(equals.asBoolean(), is(true));
  }

  @Test
  public void shouldEvalConcatExpression() throws Exception {
    Value simpleConcat = client.query(Concat(Arr(Value("Magic"), Value("Missile")))).get();
    assertThat(simpleConcat.asString(), equalTo("MagicMissile"));

    Value concatWithSeparator = client.query(
      Concat(
        Arr(
          Value("Magic"),
          Value("Missile")
        ),
        Value(" ")
      )).get();

    assertThat(concatWithSeparator.asString(), equalTo("Magic Missile"));
  }

  @Test
  public void shouldEvalCasefoldExpression() throws Exception {
    Value res = client.query(Casefold(Value("Hen Wen"))).get();
    assertThat(res.asString(), equalTo("hen wen"));
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

    assertThat(contains.asBoolean(), is(true));
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

    assertThat(selected.asString(), equalTo("munchings"));
  }

  @Test
  public void shouldEvalLTExpression() throws Exception {
    Value res = client.query(LT(Arr(Value(1), Value(2), Value(3)))).get();
    assertThat(res.asBoolean(), is(true));
  }

  @Test
  public void shouldEvalLTEExpression() throws Exception {
    Value res = client.query(LTE(Arr(Value(1), Value(2), Value(2)))).get();
    assertThat(res.asBoolean(), is(true));
  }

  @Test
  public void shouldEvalGTxpression() throws Exception {
    Value res = client.query(GT(Arr(Value(3), Value(2), Value(1)))).get();
    assertThat(res.asBoolean(), is(true));
  }

  @Test
  public void shouldEvalGTExpression() throws Exception {
    Value res = client.query(GTE(Arr(Value(3), Value(2), Value(2)))).get();
    assertThat(res.asBoolean(), is(true));
  }

  @Test
  public void shouldEvalAddExpression() throws Exception {
    Value res = client.query(Add(Value(100), Value(10))).get();
    assertThat(res.asLong(), equalTo(110L));
  }

  @Test
  public void shouldEvalMultiplyExpression() throws Exception {
    Value res = client.query(Multiply(Value(100), Value(10))).get();
    assertThat(res.asLong(), equalTo(1000L));
  }

  @Test
  public void shouldEvalSubtractExpression() throws Exception {
    Value res = client.query(Subtract(Value(100), Value(10))).get();
    assertThat(res.asLong(), equalTo(90L));
  }

  @Test
  public void shouldEvalDivideExpression() throws Exception {
    Value res = client.query(Divide(Value(100), Value(10))).get();
    assertThat(res.asLong(), equalTo(10L));
  }

  @Test
  public void shouldEvalModuloExpression() throws Exception {
    Value res = client.query(Modulo(Value(101), Value(10))).get();
    assertThat(res.asLong(), equalTo(1L));
  }

  @Test
  public void shouldEvalAndExpression() throws Exception {
    Value res = client.query(And(Value(true), Value(false))).get();
    assertThat(res.asBoolean(), is(false));
  }

  @Test
  public void shouldEvalOrExpression() throws Exception {
    Value res = client.query(Or(Value(true), Value(false))).get();
    assertThat(res.asBoolean(), is(true));
  }

  @Test
  public void shouldEvalNotExpression() throws Exception {
    Value notR = client.query(Not(Value(false))).get();
    assertThat(notR.asBoolean(), is(true));
  }

  @Test
  public void shouldEvalTimeExpression() throws Exception {
    Value res = client.query(Time(Value("1970-01-01T00:00:00-04:00"))).get();
    assertThat(res.asTs(), equalTo(Instant.EPOCH.plus(4, HOURS)));
  }

  @Test
  public void shouldEvalEpochExpression() throws Exception {
    Value res = client.query(Epoch(Value(30), SECOND)).get();
    assertThat(res.asTs(), equalTo(Instant.EPOCH.plus(30, SECONDS)));
  }

  @Test
  public void shouldEvalDateExpression() throws Exception {
    Value res = client.query(Date(Value("1970-01-02"))).get();
    assertThat(res.asDate(), equalTo(LocalDate.ofEpochDay(1)));
  }

  @Test
  public void shouldGetNextId() throws Exception {
    Value res = client.query(NextId()).get();
    assertThat(res.asString(), notNullValue());
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
        createdInstance.get("ref"),
        Obj("password", Value("abcdefg")))
    ).get();

    String token = auth.get("secret").asString();

    FaunaClient sessionClient = createFaunaClient(token);

    try {
      Value loggedOut = sessionClient.query(Logout(Value(true))).get();
      assertThat(loggedOut.asBoolean(), is(true));
    } finally {
      sessionClient.close();
    }

    Value identified = client.query(
      Identify(
        createdInstance.get("ref"),
        Value("wrong-password")
      )
    ).get();

    assertThat(identified.asBoolean(), is(false));
  }

  private Ref onARandomClass() throws Exception {
    Value clazz = client.query(
      Create(Ref("classes"),
        Obj("name", Value(randomStartingWith("some_class_"))))
    ).get();

    return clazz.get("ref").asRef();
  }

  private List<Ref> collectRefsFom(Value response) {
    ImmutableList.Builder<Ref> refs = ImmutableList.builder();
    for (Value value : response.asArray())
      refs.add(value.asRef());

    return refs.build();
  }

  private List<Ref> collectResourcesFrom(Value response) {
    ImmutableList.Builder<Ref> refs = ImmutableList.builder();
    for (Value value : response.asArray())
      refs.add(value.get("resource").asRef());

    return refs.build();
  }

  private String randomStartingWith(String... parts) {
    StringBuilder builder = new StringBuilder();
    for (String part : parts)
      builder.append(part);

    builder.append(Math.abs(new Random().nextLong()));
    return builder.toString();
  }

}