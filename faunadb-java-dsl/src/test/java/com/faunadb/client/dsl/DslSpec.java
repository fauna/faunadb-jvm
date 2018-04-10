package com.faunadb.client.dsl;

import com.faunadb.client.errors.BadRequestException;
import com.faunadb.client.errors.NotFoundException;
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.*;
import com.faunadb.client.types.Value.*;
import com.faunadb.client.types.time.HighPrecisionTime;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;

import static com.faunadb.client.query.Language.Action.CREATE;
import static com.faunadb.client.query.Language.Action.DELETE;
import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.query.Language.Class;
import static com.faunadb.client.query.Language.TimeUnit.*;
import static com.faunadb.client.types.Codec.*;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertThat;

public abstract class DslSpec {

  protected static final String ROOT_TOKEN = EnvVariables.require("FAUNA_ROOT_KEY");
  protected static final String ROOT_URL = format("%s://%s:%s",
    EnvVariables.getOrElse("FAUNA_SCHEME", "https"),
    EnvVariables.getOrElse("FAUNA_DOMAIN", "db.fauna.com"),
    EnvVariables.getOrElse("FAUNA_PORT", "443")
  );

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  protected static final Field<Value> DATA = Field.at("data");
  protected static final Field<Spell> SPELL_FIELD = DATA.to(Spell.class);
  protected static final Field<Long> TS_FIELD = Field.at("ts").to(LONG);
  protected static final Field<RefV> REF_FIELD = Field.at("ref").to(REF);
  protected static final Field<RefV> INSTANCE_FIELD = Field.at("instance").to(REF);
  protected static final Field<ImmutableList<RefV>> REF_LIST = DATA.collect(Field.as(REF));

  protected static final Field<String> NAME_FIELD = DATA.at(Field.at("name")).to(STRING);
  protected static final Field<String> ELEMENT_FIELD = DATA.at(Field.at("element")).to(STRING);
  protected static final Field<Value> ELEMENTS_LIST = DATA.at(Field.at("elements"));
  protected static final Field<Long> COST_FIELD = DATA.at(Field.at("cost")).to(LONG);
  protected static final Field<String> SECRET_FIELD = Field.at("secret").to(STRING);

  protected static RefV magicMissile;
  protected static RefV fireball;
  protected static RefV faerieFire;
  protected static RefV summon;
  protected static RefV thor;
  protected static RefV thorSpell1;
  protected static RefV thorSpell2;

  protected abstract ListenableFuture<Value> query(Expr expr);
  protected abstract ListenableFuture<ImmutableList<Value>> query(List<? extends Expr> exprs);

  private static boolean initialized = false;

  @Before
  public void setUpSchema() throws Exception {
    if (initialized)
      return;

    initialized = true;

    query(ImmutableList.of(
      CreateClass(Obj("name", Value("spells"))),
      CreateClass(Obj("name", Value("characters"))),
      CreateClass(Obj("name", Value("spellbooks")))
    )).get();

    query(ImmutableList.of(
      CreateIndex(Obj(
        "name", Value("all_spells"),
        "source", Class("spells")
      )),

      CreateIndex(Obj(
        "name", Value("spells_by_element"),
        "source", Class("spells"),
        "terms", Arr(Obj("field", Arr(Value("data"), Value("element"))))
      )),

      CreateIndex(Obj(
        "name", Value("elements_of_spells"),
        "source", Class("spells"),
        "values", Arr(Obj("field", Arr(Value("data"), Value("element"))))
      )),

      CreateIndex(Obj(
        "name", Value("spellbooks_by_owner"),
        "source", Class("spellbooks"),
        "terms", Arr(Obj("field", Arr(Value("data"), Value("owner"))))
      )),

      CreateIndex(Obj(
        "name", Value("spells_by_spellbook"),
        "source", Class("spells"),
        "terms", Arr(Obj("field", Arr(Value("data"), Value("spellbook"))))
      ))
    )).get();

    magicMissile = query(
      Create(Class("spells"),
        Obj("data",
          Obj(
            "name", Value("Magic Missile"),
            "element", Value("arcane"),
            "cost", Value(10))))
    ).get().get(REF_FIELD);

    fireball = query(
      Create(Class("spells"),
        Obj("data",
          Obj(
            "name", Value("Fireball"),
            "element", Value("fire"),
            "cost", Value(10))))
    ).get().get(REF_FIELD);

    faerieFire = query(
      Create(Class("spells"),
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
      Create(Class("spells"),
        Obj("data",
          Obj(
            "name", Value("Summon Animal Companion"),
            "element", Value("nature"),
            "cost", Value(10))))
    ).get().get(REF_FIELD);

    thor = query(
      Create(Class("characters"),
        Obj("data", Obj("name", Value("Thor"))))
    ).get().get(REF_FIELD);

    RefV thorsSpellbook = query(
      Create(Class("spellbooks"),
        Obj("data",
          Obj("owner", thor)))
    ).get().get(REF_FIELD);

    thorSpell1 = query(
      Create(Class("spells"),
        Obj("data",
          Obj("spellbook", thorsSpellbook)))
    ).get().get(REF_FIELD);

    thorSpell2 = query(
      Create(Class("spells"),
        Obj("data",
          Obj("spellbook", thorsSpellbook)))
    ).get().get(REF_FIELD);
  }

  @Test
  public void shouldThrowNotFoundWhenInstanceDoesntExists() throws Exception {
    thrown.expectCause(isA(NotFoundException.class));
    query(Get(Ref(Class("spells"), "1234"))).get();
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
    Value instance = query(Get(magicMissile)).get();
    assertThat(instance.get(NAME_FIELD), equalTo("Magic Missile"));
  }

  @Test
  public void shouldBeAbleToIssueABatchedQuery() throws Exception {
    ImmutableList<Value> results = query(ImmutableList.of(
      Get(magicMissile),
      Get(thor)
    )).get();

    assertThat(results, hasSize(2));
    assertThat(results.get(0).get(NAME_FIELD), equalTo("Magic Missile"));
    assertThat(results.get(1).get(NAME_FIELD), equalTo("Thor"));

    ImmutableList<Value> data = query(ImmutableList.of(
      new ObjectV(ImmutableMap.of("k1", new StringV("v1"))),
      new ObjectV(ImmutableMap.of("k2", new StringV("v2")))
    )).get();

    assertThat(data, hasSize(2));
    assertThat(data.get(0).at("k1").to(STRING).get(), equalTo("v1"));
    assertThat(data.get(1).at("k2").to(STRING).get(), equalTo("v2"));
  }

  @Test
  public void shouldBeAbleToUpdateAnInstancesData() throws Exception {
    Value createdInstance = query(
      Create(onARandomClass(),
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
    assertThat(updatedInstance.getOptional(COST_FIELD), is(Optional.<Long>absent()));
  }

  @Test
  public void shouldBeAbleToReplaceAnInstancesData() throws Exception {
    Value createdInstance = query(
      Create(onARandomClass(),
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
      Create(onARandomClass(),
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
      Create(onARandomClass(),
        Obj("data", Obj("name", Value("Magic Missile"))))
    ).get();

    Value insertedEvent = query(
      Insert(createdInstance.get(REF_FIELD), Value(1L), CREATE,
        Obj("data",
          Obj("cooldown", Value(5L))))
    ).get();

    assertThat(insertedEvent.get(INSTANCE_FIELD), equalTo(createdInstance.get(REF_FIELD)));

    Value removedEvent = query(
      Remove(createdInstance.get(REF_FIELD), Value(2L), DELETE)
    ).get();

    assertThat(removedEvent, nullValue());
  }

  static class Event {
    @FaunaField
    public String action;
    @FaunaField
    public RefV instance;
  }

  @Test
  public void shouldTestEvents() throws Exception {
    RefV ref = query(
      Create(onARandomClass(), Obj("data", Obj("x", Value(1))))
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

    List<Event> events = ImmutableList.copyOf(data.asCollectionOf(Event.class).get());

    assertThat(events, hasSize(3));

    assertThat(events.get(0).action, equalTo("create"));
    assertThat(events.get(0).instance, equalTo(ref));

    assertThat(events.get(1).action, equalTo("update"));
    assertThat(events.get(1).instance, equalTo(ref));

    assertThat(events.get(2).action, equalTo("delete"));
    assertThat(events.get(2).instance, equalTo(ref));
  }

  @Test
  public void shouldTestSingleton() throws Exception {
    RefV ref = query(
      Create(onARandomClass(), Obj("data", Obj("x", Value(1))))
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

    List<Event> events = ImmutableList.copyOf(data.asCollectionOf(Event.class).get());

    assertThat(events, hasSize(2));

    assertThat(events.get(0).action, equalTo("add"));
    assertThat(events.get(0).instance, equalTo(ref));

    assertThat(events.get(1).action, equalTo("remove"));
    assertThat(events.get(1).instance, equalTo(ref));
  }

  @Test
  public void shouldHandleConstraintViolations() throws Exception {
    RefV classRef = onARandomClass();

    query(
      CreateIndex(
        Obj(
          "name", Value(randomStartingWith("class_index_")),
          "source", classRef,
          "terms", Arr(Obj("field", Arr(Value("data"), Value("uniqueField")))),
          "unique", Value(true)
        ))
    ).get();

    query(
      Create(classRef,
        Obj("data", Obj("uniqueField", Value("same value"))))
    ).get();

    thrown.expectCause(isA(BadRequestException.class));
    query(
      Create(classRef,
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
  public void shouldListAllItensOnAClassIndex() throws Exception {
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
    assertThat(page1.at("before").to(VALUE).getOptional(), is(Optional.<Value>absent()));

    Value page2 = query(
      Paginate(Match(Index("all_spells")))
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
    Value res = query(
      Match(
        Index("spells_by_element"),
        Value("arcane"))
    ).get();

    ImmutableMap<String, Value> set = res.to(SET_REF).get().parameters();
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
    RefV ref = new RefV(randomStartingWith(), onARandomClass());

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
  public void shouldExecuteForeachExpression() throws Exception {
    Value res = query(
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
  public void shouldReadEventsFromIndex() throws Exception {
    Value events = query(
      Paginate(Match(Index("spells_by_element"), Value("arcane")))
        .events(true)
    ).get();

    assertThat(events.get(DATA).collect(INSTANCE_FIELD),
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
  public void shouldEvalAddExpression() throws Exception {
    Value res = query(Add(Value(100), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(110L));
  }

  @Test
  public void shouldEvalMultiplyExpression() throws Exception {
    Value res = query(Multiply(Value(100), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(1000L));
  }

  @Test
  public void shouldEvalSubtractExpression() throws Exception {
    Value res = query(Subtract(Value(100), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(90L));
  }

  @Test
  public void shouldEvalDivideExpression() throws Exception {
    Value res = query(Divide(Value(100), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(10L));
  }

  @Test
  public void shouldEvalModuloExpression() throws Exception {
    Value res = query(Modulo(Value(101), Value(10))).get();
    assertThat(res.to(LONG).get(), equalTo(1L));
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
    assertThat(res.to(TIME).get(), equalTo(new Instant(0).plus(Duration.standardHours(4))));
  }

  @Test
  public void shouldEvalEpochExpression() throws Exception {
    ImmutableList<Value> res = query(ImmutableList.of(
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
    ImmutableList<Value> res = query(ImmutableList.of(
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
    Value res = query(Arr(
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
    Value res = query(Date(Value("1970-01-02"))).get();
    assertThat(res.to(DATE).get(), equalTo(new LocalDate(0, UTC).plusDays(1)));
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

    assertThat(query(Exists(Function(functionName))).get(), equalTo((Value)BooleanV.TRUE));
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
      Create(Class("spells"),
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
      equalTo((Value) new RefV("all_spells", Native.INDEXES))
    );

    assertThat(
      query(Class("spells")).get(),
      equalTo((Value) new RefV("spells", Native.CLASSES))
    );

    assertThat(
      query(Database("faunadb-database")).get(),
      equalTo((Value) new RefV("faunadb-database", Native.DATABASES))
    );

    assertThat(
      query(new RefV("1234567890", Native.KEYS)).get(),
      equalTo((Value) new RefV("1234567890", Native.KEYS))
    );

    assertThat(
      query(Function("function_name")).get(),
      equalTo((Value) new RefV("function_name", Native.FUNCTIONS))
    );

    assertThat(
      query(Ref(Class("spells"), Value("1234567890"))).get(),
      equalTo((Value) new RefV("1234567890", new RefV("spells", Native.CLASSES)))
    );

    assertThat(
      query(Ref(Class("spells"), "1234567890")).get(),
      equalTo((Value) new RefV("1234567890", new RefV("spells", Native.CLASSES)))
    );
  }

  @Test
  public void shouldCreateNestedRefFromString() throws Exception {
    assertThat(
      query(Ref("classes/widget/123")).get(),
      equalTo((Value) new RefV("123", new RefV("widget", Native.CLASSES)))
    );
  }

  protected RefV onARandomClass() throws Exception {

    Value clazz = query(
      CreateClass(
        Obj("name", Value(randomStartingWith("some_class_"))))
    ).get();

    return clazz.get(REF_FIELD);
  }

  protected String randomStartingWith(String... parts) {
    StringBuilder builder = new StringBuilder();
    for (String part : parts)
      builder.append(part);

    builder.append(Math.abs(new Random().nextLong()));
    return builder.toString();
  }
}

