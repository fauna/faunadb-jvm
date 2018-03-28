package com.faunadb.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Value.*;
import com.faunadb.client.types.time.HighPrecisionTime;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.query.Language.Class;
import static com.faunadb.client.query.Language.TimeUnit.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class SerializationSpec {

  private ObjectMapper json;

  @Before
  public void setUp() {
    json = new ObjectMapper();
  }

  @Test
  public void shouldSerializeLiteralValues() throws Exception {
    assertJson(Value(Long.MAX_VALUE), String.valueOf(Long.MAX_VALUE));
    assertJson(Value("a string"), "\"a string\"");
    assertJson(Value(10), "10");
    assertJson(Value(1.0), "1.0");
    assertJson(Value(true), "true");
    assertJson(Value(false), "false");
    assertJson(Null(), "null");
  }

  @Test
  public void shouldSerializeAnArray() throws Exception {
    assertJson(
      Arr(
        Value("a string"),
        Value(10),
        Obj("data", Value("str"))
      ), "[\"a string\",10,{\"object\":{\"data\":\"str\"}}]");

    assertJson(
      Arr(
        new ObjectV(ImmutableMap.of(
          "value", new StringV("test")
        ))
      ), "[{\"object\":{\"value\":\"test\"}}]");

    assertJson(
      Arr(ImmutableList.of(
        Value("other string"),
        Value(42)
      )), "[\"other string\",42]");
  }

  @Test
  public void shouldSerializeAnObject() throws Exception {
    assertJson(Obj(), "{\"object\":{}}");

    assertJson(
      Obj("k1", Value("v1")),
      "{\"object\":{\"k1\":\"v1\"}}");

    assertJson(
      Obj(
        "k1", new ObjectV(ImmutableMap.of(
          "v1", new ObjectV(ImmutableMap.of(
            "v2", new StringV("test")))
        ))),
      "{\"object\":{\"k1\":{\"object\":{\"v1\":{\"object\":{\"v2\":\"test\"}}}}}}");

    assertJson(
      Obj("k1", Obj("v1", Obj("v2", Value("test")))),
      "{\"object\":{\"k1\":{\"object\":{\"v1\":{\"object\":{\"v2\":\"test\"}}}}}}");

    assertJson(Obj("k1", Value("v1"), "k2", Value("v2")),
      "{\"object\":{\"k1\":\"v1\",\"k2\":\"v2\"}}");

    assertJson(
      Obj(
        "k1", Value("v1"),
        "k2", Value("v2"),
        "k3", Value("v3")
      ),
      "{\"object\":{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\"}}");

    assertJson(
      Obj(
        "k1", Value("v1"),
        "k2", Value("v2"),
        "k3", Value("v3"),
        "k4", Value("v4")
      ),
      "{\"object\":{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\",\"k4\":\"v4\"}}");

    assertJson(
      Obj(
        "k1", Value("v1"),
        "k2", Value("v2"),
        "k3", Value("v3"),
        "k4", Value("v4"),
        "k5", Value("v5")
      ),
      "{\"object\":{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\",\"k4\":\"v4\",\"k5\":\"v5\"}}");

    assertJson(
      Obj(ImmutableMap.of(
        "k1", Value("v1"),
        "k2", Value("v2"))
      ),
      "{\"object\":{\"k1\":\"v1\",\"k2\":\"v2\"}}");
  }

  @Test
  public void shouldSerializeRef() throws Exception {
    assertJson(Native.CLASSES, "{\"@ref\":{\"id\":\"classes\"}}");
    assertJson(new RefV("id1", new RefV("people", Native.CLASSES)), "{\"@ref\":{\"id\":\"id1\",\"class\":{\"@ref\":{\"id\":\"people\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}");
  }

  @Test
  public void shouldSerializeClass() throws Exception {
    assertJson(Class(Value("spells")), "{\"class\":\"spells\"}");
  }

  @Test
  public void shouldSerializeDatabase() throws Exception {
    assertJson(Database(Value("test-db")), "{\"database\":\"test-db\"}");
  }

  @Test
  public void shouldSerializeIndex() throws Exception {
    assertJson(Index(Value("spells_by_name")), "{\"index\":\"spells_by_name\"}");
  }

  @Test
  public void shouldSerializeInstantValue() throws Exception {
    assertJson(Value(new Instant(0)), "{\"@ts\":\"1970-01-01T00:00:00.000000000Z\"}");
  }

  @Test
  public void shouldSerializeHighPrecisionTimeValue() throws Exception {
    Instant initialTime = new Instant(10)
      .plus(Duration.standardMinutes(5))
      .plus(Duration.standardSeconds(2));

    assertJson(Value(HighPrecisionTime.fromInstant(initialTime)),
      "{\"@ts\":\"1970-01-01T00:05:02.010000000Z\"}");

    assertJson(Value(HighPrecisionTime.fromInstantWithMicros(initialTime, 1005)),
      "{\"@ts\":\"1970-01-01T00:05:02.011005000Z\"}");

    assertJson(Value(HighPrecisionTime.fromInstantWithNanos(initialTime, 20005)),
      "{\"@ts\":\"1970-01-01T00:05:02.010020005Z\"}");

    assertJson(Value(HighPrecisionTime.fromInstantWithNanos(initialTime, 3021001)),
      "{\"@ts\":\"1970-01-01T00:05:02.013021001Z\"}");
  }

  @Test
  public void shouldSerializeDateValue() throws Exception {
    assertJson(Value(new LocalDate(2015, 1, 15)), "{\"@date\":\"2015-01-15\"}");
  }

  @Test
  public void shouldSerializeBytesValue() throws Exception {
    assertJson(Value(new byte[] {0x1, 0x2, 0x3, 0x4}),
      "{\"@bytes\":\"AQIDBA==\"}");
  }

  @Test
  public void shouldSerializeBytesValueUrlSafe() throws Exception {
    assertJson(Value(new byte[] {(byte)0xf8}), "{\"@bytes\":\"-A==\"}");
    assertJson(Value(new byte[] {(byte)0xf9}), "{\"@bytes\":\"-Q==\"}");
    assertJson(Value(new byte[] {(byte)0xfa}), "{\"@bytes\":\"-g==\"}");
    assertJson(Value(new byte[] {(byte)0xfb}), "{\"@bytes\":\"-w==\"}");
    assertJson(Value(new byte[] {(byte)0xfc}), "{\"@bytes\":\"_A==\"}");
    assertJson(Value(new byte[] {(byte)0xfd}), "{\"@bytes\":\"_Q==\"}");
    assertJson(Value(new byte[] {(byte)0xfe}), "{\"@bytes\":\"_g==\"}");
    assertJson(Value(new byte[] {(byte)0xff}), "{\"@bytes\":\"_w==\"}");
  }

  @Test
  public void shouldSerializeAbort() throws Exception {
    assertJson(Abort(Value("a message")), "{\"abort\":\"a message\"}");
    assertJson(Abort("a message"), "{\"abort\":\"a message\"}");
  }

  @Test
  public void shouldSerializeLet() throws Exception {
    assertJson(
      Let(
        "v1", Obj("x1", Value("y1"))
      ).in(
        Value("x")
      ), "{\"let\":{\"v1\":{\"object\":{\"x1\":\"y1\"}}},\"in\":\"x\"}");

    assertJson(
      Let(
        "v1", Value("x1")
      ).in(
        Value("x")
      ), "{\"let\":{\"v1\":\"x1\"},\"in\":\"x\"}");

    assertJson(
      Let(
        "v1", Value("x1"),
        "v2", Value("x2")
      ).in(
        Value("x")
      ), "{\"let\":{\"v1\":\"x1\",\"v2\":\"x2\"},\"in\":\"x\"}");

    assertJson(
      Let(
        "v1", Value("x1"),
        "v2", Value("x2"),
        "v3", Value("x3")
      ).in(
        Value("x")
      ), "{\"let\":{\"v1\":\"x1\",\"v2\":\"x2\",\"v3\":\"x3\"},\"in\":\"x\"}");

    assertJson(
      Let(
        "v1", Value("x1"),
        "v2", Value("x2"),
        "v3", Value("x3"),
        "v4", Value("x4")
      ).in(
        Value("x")
      ), "{\"let\":{\"v1\":\"x1\",\"v2\":\"x2\",\"v3\":\"x3\",\"v4\":\"x4\"},\"in\":\"x\"}");

    assertJson(
      Let(
        "v1", Value("x1"),
        "v2", Value("x2"),
        "v3", Value("x3"),
        "v4", Value("x4"),
        "v5", Value("x5")
      ).in(
        Value("x")
      ), "{\"let\":{\"v1\":\"x1\",\"v2\":\"x2\",\"v3\":\"x3\",\"v4\":\"x4\",\"v5\":\"x5\"},\"in\":\"x\"}");

    assertJson(
      Let(ImmutableMap.of(
        "v1", Value("x1"),
        "v2", Value("x2")
        )
      ).in(
        Value("x")
      ), "{\"let\":{\"v1\":\"x1\",\"v2\":\"x2\"},\"in\":\"x\"}");
  }

  @Test
  public void shouldSerializeVar() throws Exception {
    assertJson(Var("x"), "{\"var\":\"x\"}");
  }

  @Test
  public void shouldSerializeIf() throws Exception {
    assertJson(
      If(Value(true), Value(true), Value(false)),
      "{\"if\":true,\"then\":true,\"else\":false}");
  }

  @Test
  public void shouldSerializeDo() throws Exception {
    assertJson(
      Do(
        If(Value(true), Value("x"), Value("y")),
        Value(42)
      ), "{\"do\":[{\"if\":true,\"then\":\"x\",\"else\":\"y\"},42]}");

    assertJson(
      Do(ImmutableList.of(
        If(Value(true), Value("xx"), Value("yy")),
        Value(45)
      )), "{\"do\":[{\"if\":true,\"then\":\"xx\",\"else\":\"yy\"},45]}");

    assertJson(
      Do(Arr(
        If(Value(true), Value("xx"), Value("yy")),
        Value(45)
      )), "{\"do\":[{\"if\":true,\"then\":\"xx\",\"else\":\"yy\"},45]}");
  }

  @Test
  public void shouldSerializeLambda() throws Exception {
    assertJson(
      Lambda(Value("x"),
        If(Var("x"), Value(42), Value(45))
      ), "{\"lambda\":\"x\",\"expr\":{\"if\":{\"var\":\"x\"},\"then\":42,\"else\":45}}");

    assertJson(
      Lambda(Arr(Value("x"), Value("_")),
        If(Var("x"), Value(42), Value(45))
      ), "{\"lambda\":[\"x\",\"_\"],\"expr\":{\"if\":{\"var\":\"x\"},\"then\":42,\"else\":45}}");
  }

  @Test
  public void shouldSerializeMap() throws Exception {
    assertJson(
      Map(
        Arr(Value(1), Value(2), Value(3)),
        Lambda(Value("x"), Var("x"))
      ), "{\"map\":{\"lambda\":\"x\",\"expr\":{\"var\":\"x\"}},\"collection\":[1,2,3]}");
  }

  @Test
  public void shouldSerializeForeach() throws Exception {
    assertJson(
      Foreach(
        Arr(Value(1), Value(2), Value(3)),
        Lambda(Value("x"), Var("x"))
      ), "{\"foreach\":{\"lambda\":\"x\",\"expr\":{\"var\":\"x\"}},\"collection\":[1,2,3]}");
  }

  @Test
  public void shouldSerializeFilter() throws Exception {
    assertJson(
      Filter(
        Arr(Value(true), Value(false)),
        Lambda(Value("x"), Var("x"))
      ), "{\"filter\":{\"lambda\":\"x\",\"expr\":{\"var\":\"x\"}},\"collection\":[true,false]}");
  }

  @Test
  public void shouldSerializeTake() throws Exception {
    assertJson(
      Take(
        Value(2),
        Arr(Value(1), Value(2), Value(3))
      ), "{\"take\":2,\"collection\":[1,2,3]}"
    );
  }

  @Test
  public void shouldSerializeDrop() throws Exception {
    assertJson(
      Drop(
        Value(2),
        Arr(Value(1), Value(2), Value(3))
      ), "{\"drop\":2,\"collection\":[1,2,3]}"
    );
  }

  @Test
  public void shouldSerializePrepend() throws Exception {
    assertJson(
      Prepend(
        Arr(Value(1), Value(2), Value(3)),
        Arr(Value(4), Value(5), Value(6))
      ), "{\"prepend\":[1,2,3],\"collection\":[4,5,6]}"
    );
  }

  @Test
  public void shouldSerializeAppend() throws Exception {
    assertJson(
      Append(
        Arr(Value(4), Value(5), Value(6)),
        Arr(Value(1), Value(2), Value(3))
      ), "{\"append\":[4,5,6],\"collection\":[1,2,3]}"
    );
  }

  @Test
  public void shouldSerializeGet() throws Exception {
    assertJson(
      Get(new RefV("104979509692858368", new RefV("spells", Native.CLASSES))),
      "{\"get\":{\"@ref\":{\"id\":\"104979509692858368\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}}"
    );

    assertJson(
      Get(new RefV("104979509692858368", new RefV("spells", Native.CLASSES)), Value(new Instant(0))),
      "{\"get\":{\"@ref\":{\"id\":\"104979509692858368\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"ts\":{\"@ts\":\"1970-01-01T00:00:00.000000000Z\"}}"
    );
  }

  @Test
  public void shouldSerializeKeyFromSecret() throws Exception {
    assertJson(KeyFromSecret(Value("s3cr3t")), "{\"key_from_secret\":\"s3cr3t\"}");
  }

  @Test
  public void shouldSerializePaginate() throws Exception {
    assertJson(
      Paginate(Native.DATABASES),
      "{\"paginate\":{\"@ref\":{\"id\":\"databases\"}}}"
    );

    assertJson(
      Paginate(Native.DATABASES)
        .after(new RefV("test", Native.DATABASES))
        .events(true)
        .sources(true)
        .ts(10L)
        .size(2),
      "{\"paginate\":{\"@ref\":{\"id\":\"databases\"}},\"after\":{\"@ref\":{\"id\":\"test\",\"class\":{\"@ref\":{\"id\":\"databases\"}}}}," +
        "\"events\":true,\"sources\":true,\"ts\":10,\"size\":2}"
    );

    assertJson(
      Paginate(Native.DATABASES)
        .after(new RefV("test", Native.DATABASES))
        .events(Value(true))
        .sources(Value(true))
        .ts(Value(10L))
        .size(Value(2)),
      "{\"paginate\":{\"@ref\":{\"id\":\"databases\"}},\"after\":{\"@ref\":{\"id\":\"test\",\"class\":{\"@ref\":{\"id\":\"databases\"}}}}," +
        "\"events\":true,\"sources\":true,\"ts\":10,\"size\":2}"
    );

    assertJson(
      Paginate(Native.DATABASES)
        .before(new RefV("test", Native.DATABASES))
        .events(false)
        .sources(false)
        .ts(10L)
        .size(2),
      "{\"paginate\":{\"@ref\":{\"id\":\"databases\"}},\"before\":{\"@ref\":{\"id\":\"test\",\"class\":{\"@ref\":{\"id\":\"databases\"}}}}," +
        "\"ts\":10,\"size\":2}"
    );
  }

  @Test
  public void shouldSerializeExists() throws Exception {
    assertJson(
      Exists(new RefV("104979509692858368", new RefV("spells", Native.CLASSES))),
      "{\"exists\":{\"@ref\":{\"id\":\"104979509692858368\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}}"
    );

    assertJson(
      Exists(new RefV("104979509692858368", new RefV("spells", Native.CLASSES)), Value(new Instant(0))),
      "{\"exists\":{\"@ref\":{\"id\":\"104979509692858368\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"ts\":{\"@ts\":\"1970-01-01T00:00:00.000000000Z\"}}"
    );
  }

  @Test
  public void shouldSerializeCreate() throws Exception {
    assertJson(
      Create(
        Native.DATABASES,
        Obj("name", Value("annuvin"))
      ), "{\"create\":{\"@ref\":{\"id\":\"databases\"}},\"params\":{\"object\":{\"name\":\"annuvin\"}}}");

  }

  @Test
  public void shouldSerializeUpdate() throws Exception {
    assertJson(
      Update(
        new RefV("annuvin", Native.DATABASES),
        Obj("name", Value("llyr"))
      ), "{\"update\":{\"@ref\":{\"id\":\"annuvin\",\"class\":{\"@ref\":{\"id\":\"databases\"}}}},\"params\":{\"object\":{\"name\":\"llyr\"}}}");

  }

  @Test
  public void shouldSerializeReplace() throws Exception {
    assertJson(
      Replace(
        new RefV("104979509696660483", new RefV("spells", Native.CLASSES)),
        Obj("data",
          Obj("name", Value("Mountain's Thunder")))
      ), "{\"replace\":{\"@ref\":{\"id\":\"104979509696660483\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}," +
        "\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Mountain's Thunder\"}}}}}");
  }

  @Test
  public void shouldSerializeDelete() throws Exception {
    assertJson(
      Delete(new RefV("104979509696660483", new RefV("spells", Native.CLASSES))),
      "{\"delete\":{\"@ref\":{\"id\":\"104979509696660483\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}}"
    );
  }

  @Test
  public void shouldSerializeInsert() throws Exception {
    assertJson(
      Insert(
        new RefV("104979509696660483", new RefV("spells", Native.CLASSES)),
        Value(new Instant(0)),
        Action.CREATE,
        Obj("data", Obj("name", Value("test")))
      ),
      "{\"insert\":{\"@ref\":{\"id\":\"104979509696660483\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"ts\":{\"@ts\":\"1970-01-01T00:00:00.000000000Z\"}," +
        "\"action\":\"create\",\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"test\"}}}}}"
    );

    assertJson(
      Insert(
        new RefV("104979509696660483", new RefV("spells", Native.CLASSES)),
        Value(new Instant(0)),
        Value("create"),
        Obj("data", Obj("name", Value("test")))
      ),
      "{\"insert\":{\"@ref\":{\"id\":\"104979509696660483\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"ts\":{\"@ts\":\"1970-01-01T00:00:00.000000000Z\"}," +
        "\"action\":\"create\",\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"test\"}}}}}"
    );
  }

  @Test
  public void shouldSerializeRemove() throws Exception {
    assertJson(
      Remove(
        new RefV("104979509696660483", new RefV("spells", Native.CLASSES)),
        Value(new Instant(0)),
        Action.DELETE
      ),
      "{\"remove\":{\"@ref\":{\"id\":\"104979509696660483\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}," +
        "\"ts\":{\"@ts\":\"1970-01-01T00:00:00.000000000Z\"},\"action\":\"delete\"}"
    );

    assertJson(
      Remove(
        new RefV("104979509696660483", new RefV("spells", Native.CLASSES)),
        Value(new Instant(0)),
        Value("delete")
      ),
      "{\"remove\":{\"@ref\":{\"id\":\"104979509696660483\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}," +
        "\"ts\":{\"@ts\":\"1970-01-01T00:00:00.000000000Z\"},\"action\":\"delete\"}"
    );
  }

  @Test
  public void shouldSerializeCreateClass() throws Exception {
    assertJson(
      CreateClass(Obj(
        "name", Value("spells")
      )),
      "{\"create_class\":{\"object\":{\"name\":\"spells\"}}}"
    );
  }

  @Test
  public void shouldSerializeCreateDatabase() throws Exception {
    assertJson(
      CreateDatabase(Obj(
        "name", Value("db-test")
      )),
      "{\"create_database\":{\"object\":{\"name\":\"db-test\"}}}"
    );
  }

  @Test
  public void shouldSerializeCreateKey() throws Exception {
    assertJson(
      CreateKey(Obj(
        "database", Database(Value("db-test")),
        "role", Value("server")
      )),
      "{\"create_key\":{\"object\":{\"database\":{\"database\":\"db-test\"},\"role\":\"server\"}}}"
    );
  }

  @Test
  public void shouldSerializeCreateIndex() throws Exception {
    assertJson(
      CreateIndex(Obj(
        "name", Value("all_spells"),
        "source", Class(Value("spells"))
      )),
      "{\"create_index\":{\"object\":{\"name\":\"all_spells\",\"source\":{\"class\":\"spells\"}}}}"
    );
  }

  @Test
  public void shouldSerializeSingletonFunction() throws Exception {
    assertJson(
      Singleton(Ref("classes/widget/1")),
      "{\"singleton\":{\"@ref\":\"classes/widget/1\"}}"
    );
  }

  @Test
  public void shouldSerializeEventsFunction() throws Exception {
    assertJson(
      Events(Ref("classes/widget/1")),
      "{\"events\":{\"@ref\":\"classes/widget/1\"}}"
    );
  }

  @Test
  public void shouldSerializeMatchFunction() throws Exception {
    assertJson(
      Match(new RefV("all_users", Native.INDEXES)),
      "{\"match\":{\"@ref\":{\"id\":\"all_users\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}}"
    );

    assertJson(
      Match(new RefV("spells_by_element", Native.INDEXES), Value("fire")),
      "{\"match\":{\"@ref\":{\"id\":\"spells_by_element\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}},\"terms\":\"fire\"}"
    );
  }

  @Test
  public void shouldSerializeUnion() throws Exception {
    assertJson(
      Union(Native.DATABASES, Native.KEYS),
      "{\"union\":[{\"@ref\":{\"id\":\"databases\"}},{\"@ref\":{\"id\":\"keys\"}}]}"
    );

    assertJson(
      Union(ImmutableList.of(Native.DATABASES, Native.KEYS)),
      "{\"union\":[{\"@ref\":{\"id\":\"databases\"}},{\"@ref\":{\"id\":\"keys\"}}]}"
    );

    assertJson(
      Union(Arr(Native.DATABASES, Native.KEYS)),
      "{\"union\":[{\"@ref\":{\"id\":\"databases\"}},{\"@ref\":{\"id\":\"keys\"}}]}"
    );
  }

  @Test
  public void shouldSerializeIntersection() throws Exception {
    assertJson(
      Intersection(Native.DATABASES, Native.KEYS),
      "{\"intersection\":[{\"@ref\":{\"id\":\"databases\"}},{\"@ref\":{\"id\":\"keys\"}}]}"
    );

    assertJson(
      Intersection(ImmutableList.of(Native.DATABASES, Native.KEYS)),
      "{\"intersection\":[{\"@ref\":{\"id\":\"databases\"}},{\"@ref\":{\"id\":\"keys\"}}]}"
    );

    assertJson(
      Intersection(Arr(Native.DATABASES, Native.KEYS)),
      "{\"intersection\":[{\"@ref\":{\"id\":\"databases\"}},{\"@ref\":{\"id\":\"keys\"}}]}"
    );
  }

  @Test
  public void shouldSerializeDifference() throws Exception {
    assertJson(
      Difference(Native.DATABASES, Native.KEYS),
      "{\"difference\":[{\"@ref\":{\"id\":\"databases\"}},{\"@ref\":{\"id\":\"keys\"}}]}"
    );

    assertJson(
      Difference(ImmutableList.of(Native.DATABASES, Native.KEYS)),
      "{\"difference\":[{\"@ref\":{\"id\":\"databases\"}},{\"@ref\":{\"id\":\"keys\"}}]}"
    );

    assertJson(
      Difference(Arr(Native.DATABASES, Native.KEYS)),
      "{\"difference\":[{\"@ref\":{\"id\":\"databases\"}},{\"@ref\":{\"id\":\"keys\"}}]}"
    );
  }

  @Test
  public void shouldSerializeDistinct() throws Exception {
    assertJson(
      Distinct(Match(new RefV("some_set", Native.INDEXES))),
      "{\"distinct\":{\"match\":{\"@ref\":{\"id\":\"some_set\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}}}"
    );
  }

  @Test
  public void shouldSerializeJoin() throws Exception {
    assertJson(
      Join(
        Match(new RefV("spellbooks_by_owner", Native.INDEXES), new RefV("104979509695139637", new RefV("characters", Native.CLASSES))),
        new RefV("spells_by_spellbook", Native.INDEXES)
      ),
      "{\"join\":{\"match\":{\"@ref\":{\"id\":\"spellbooks_by_owner\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}," +
        "\"terms\":{\"@ref\":{\"id\":\"104979509695139637\",\"class\":{\"@ref\":{\"id\":\"characters\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}}," +
        "\"with\":{\"@ref\":{\"id\":\"spells_by_spellbook\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}}"
    );
  }

  @Test
  public void shouldSerializeLogin() throws Exception {
    assertJson(
      Login(
        new RefV("104979509695139637", new RefV("characters", Native.CLASSES)),
        Obj("password", Value("abracadabra"))
      ),
      "{\"login\":{\"@ref\":{\"id\":\"104979509695139637\",\"class\":{\"@ref\":{\"id\":\"characters\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}," +
        "\"params\":{\"object\":{\"password\":\"abracadabra\"}}}"
    );
  }

  @Test
  public void shouldSerializeLogout() throws Exception {
    assertJson(Logout(Value(true)), "{\"logout\":true}");
  }

  @Test
  public void shouldSerializeIdentify() throws Exception {
    assertJson(
      Identify(new RefV("104979509695139637", new RefV("characters", Native.CLASSES)), Value("abracadabra")),
      "{\"identify\":{\"@ref\":{\"id\":\"104979509695139637\",\"class\":{\"@ref\":{\"id\":\"characters\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"password\":\"abracadabra\"}"
    );
  }

  @Test
  public void shouldSerializeIdentity() throws Exception {
    assertJson(Identity(), "{\"identity\":null}");
  }

  @Test
  public void shouldSerializeHasIdentity() throws Exception {
    assertJson(HasIdentity(), "{\"has_identity\":null}");
  }

  @Test
  public void shouldSerializeConcat() throws Exception {
    assertJson(
      Concat(
        Arr(
          Value("Hen"),
          Value("Wen")
        )
      ), "{\"concat\":[\"Hen\",\"Wen\"]}"
    );

    assertJson(
      Concat(
        Arr(
          Value("Hen"),
          Value("Wen")
        ),
        Value(" ")
      ), "{\"concat\":[\"Hen\",\"Wen\"],\"separator\":\" \"}"
    );
  }

  @Test
  public void shouldSerializeCasefold() throws Exception {
    assertJson(Casefold(Value("Hen Wen")), "{\"casefold\":\"Hen Wen\"}");

    assertJson(Casefold(Value("Hen Wen"), Normalizer.NFD), "{\"casefold\":\"Hen Wen\",\"normalizer\":\"NFD\"}");
    assertJson(Casefold(Value("Hen Wen"), Value("NFD")), "{\"casefold\":\"Hen Wen\",\"normalizer\":\"NFD\"}");
  }

  @Test
  public void shouldSerializeNGram() throws Exception {
    assertJson(NGram(Value("str")), "{\"ngram\":\"str\"}");
    assertJson(NGram(Arr(Value("str0"), Value("str1"))), "{\"ngram\":[\"str0\",\"str1\"]}");
    assertJson(NGram(ImmutableList.of(Value("str0"), Value("str1"))), "{\"ngram\":[\"str0\",\"str1\"]}");

    assertJson(NGram(Value("str"), Value(2), Value(4)), "{\"ngram\":\"str\",\"min\":2,\"max\":4}");
    assertJson(NGram(Arr(Value("str0"), Value("str1")), Value(2), Value(4)), "{\"ngram\":[\"str0\",\"str1\"],\"min\":2,\"max\":4}");
    assertJson(NGram(ImmutableList.of(Value("str0"), Value("str1")), Value(2), Value(4)), "{\"ngram\":[\"str0\",\"str1\"],\"min\":2,\"max\":4}");
  }

  @Test
  public void shouldSerializeTime() throws Exception {
    assertJson(
      Time(Value("1970-01-01T00:00:00+00:00")),
      "{\"time\":\"1970-01-01T00:00:00+00:00\"}"
    );
  }

  @Test
  public void shouldSerializeEpoch() throws Exception {
    assertJson(Epoch(Value(0), SECOND), "{\"epoch\":0,\"unit\":\"second\"}");
    assertJson(Epoch(Value(0), MILLISECOND), "{\"epoch\":0,\"unit\":\"millisecond\"}");
    assertJson(Epoch(Value(0), MICROSECOND), "{\"epoch\":0,\"unit\":\"microsecond\"}");
    assertJson(Epoch(Value(0), NANOSECOND), "{\"epoch\":0,\"unit\":\"nanosecond\"}");

    assertJson(Epoch(Value(0), Value("second")), "{\"epoch\":0,\"unit\":\"second\"}");
    assertJson(Epoch(Value(0), Value("millisecond")), "{\"epoch\":0,\"unit\":\"millisecond\"}");
    assertJson(Epoch(Value(0), Value("microsecond")), "{\"epoch\":0,\"unit\":\"microsecond\"}");
    assertJson(Epoch(Value(0), Value("nanosecond")), "{\"epoch\":0,\"unit\":\"nanosecond\"}");
  }

  @Test
  public void shouldSerializeDate() throws Exception {
    assertJson(Date(Value("1970-01-01")), "{\"date\":\"1970-01-01\"}");
  }

  @Test
  public void shouldSerializeNewId() throws Exception {
    assertJson(NewId(), "{\"new_id\":null}");
  }

  @Test
  public void shouldSerializeEquals() throws Exception {
    assertJson(Equals(Value("fire"), Value("fire")), "{\"equals\":[\"fire\",\"fire\"]}");
    assertJson(Equals(ImmutableList.of(Value("fire"), Value("fire"))), "{\"equals\":[\"fire\",\"fire\"]}");
    assertJson(Equals(Arr(Value("fire"), Value("fire"))), "{\"equals\":[\"fire\",\"fire\"]}");
  }

  @Test
  public void shouldSerializeContains() throws Exception {
    assertJson(
      Contains(
        Path("favorites", "foods"),
        Obj("favorites",
          Obj("foods", Arr(
            Value("crunchings"),
            Value("munchings"),
            Value("lunchings")
          ))
        )
      ),
      "{\"contains\":[\"favorites\",\"foods\"],\"in\":" +
        "{\"object\":{\"favorites\":{\"object\":{\"foods\":[\"crunchings\",\"munchings\",\"lunchings\"]}}}}}");

    assertJson(
      Contains(
        Arr(Value("favorites"), Value("foods")),
        Obj("favorites",
          Obj("foods", Arr(
            Value("crunchings"),
            Value("munchings"),
            Value("lunchings")
          ))
        )
      ),
      "{\"contains\":[\"favorites\",\"foods\"],\"in\":" +
        "{\"object\":{\"favorites\":{\"object\":{\"foods\":[\"crunchings\",\"munchings\",\"lunchings\"]}}}}}");
  }

  @Test
  public void shouldSerializeSelect() throws Exception {
    assertJson(
      Select(
        Path("favorites", "foods").at(1),
        Obj("favorites",
          Obj("foods", Arr(
            Value("crunchings"),
            Value("munchings"),
            Value("lunchings")
          ))
        )
      ),
      "{\"select\":[\"favorites\",\"foods\",1],\"from\":" +
        "{\"object\":{\"favorites\":{\"object\":{\"foods\":[\"crunchings\",\"munchings\",\"lunchings\"]}}}}}");

    assertJson(
      Select(
        Path(0).at("name"),
        Arr(Obj("name", Value("someone")))
      ),
      "{\"select\":[0,\"name\"],\"from\":" +
        "[{\"object\":{\"name\":\"someone\"}}]}");

    assertJson(
      Select(
        Arr(Value("favorites"), Value("foods"), Value(1)),
        Obj("favorites",
          Obj("foods", Arr(
            Value("crunchings"),
            Value("munchings"),
            Value("lunchings")
          ))
        )
      ),
      "{\"select\":[\"favorites\",\"foods\",1],\"from\":" +
        "{\"object\":{\"favorites\":{\"object\":{\"foods\":[\"crunchings\",\"munchings\",\"lunchings\"]}}}}}");

    assertJson(
      Select(
        Arr(Value("favorites"), Value("foods"), Value(1)),
        Obj("favorites", Obj("foods", Arr())),
        Value("munchings")
      ),
      "{\"select\":[\"favorites\",\"foods\",1]," +
        "\"from\":{\"object\":{\"favorites\":{\"object\":{\"foods\":[]}}}}," +
        "\"default\":\"munchings\"}");
  }

  @Test
  public void shouldSerializeSelectAllFunction() throws Exception {
    assertJson(
      SelectAll(
        Path("foo").at(1),
        Arr(
          Obj("foo", Arr(Value(0), Value(1))),
          Obj("foo", Arr(Value(2), Value(3)))
        )
      ),
      "{\"select_all\":[\"foo\",1],\"from\":[{\"object\":{\"foo\":[0,1]}},{\"object\":{\"foo\":[2,3]}}]}"
    );

    assertJson(
      SelectAll(
        Path(0).at("name"),
        Arr(Obj("name", Value("someone")))
      ),
      "{\"select_all\":[0,\"name\"],\"from\":[{\"object\":{\"name\":\"someone\"}}]}"
    );

    assertJson(
      SelectAll(
        Arr(Value("foo"), Value("bar")),
        Obj("foo", Obj("bar", Arr(Value(1), Value(2))))
      ),
      "{\"select_all\":[\"foo\",\"bar\"],\"from\":{\"object\":{\"foo\":{\"object\":{\"bar\":[1,2]}}}}}");
  }

  @Test
  public void shouldSerializeAdd() throws Exception {
    assertJson(Add(Value(100), Value(10)), "{\"add\":[100,10]}");
    assertJson(Add(ImmutableList.of(Value(100), Value(10))), "{\"add\":[100,10]}");
    assertJson(Add(Arr(Value(100), Value(10))), "{\"add\":[100,10]}");
  }

  @Test
  public void shouldSerializeMultiply() throws Exception {
    assertJson(Multiply(Value(100), Value(10)), "{\"multiply\":[100,10]}");
    assertJson(Multiply(ImmutableList.of(Value(100), Value(10))), "{\"multiply\":[100,10]}");
    assertJson(Multiply(Arr(Value(100), Value(10))), "{\"multiply\":[100,10]}");
  }

  @Test
  public void shouldSerializeSubtract() throws Exception {
    assertJson(Subtract(Value(100), Value(10)), "{\"subtract\":[100,10]}");
    assertJson(Subtract(ImmutableList.of(Value(100), Value(10))), "{\"subtract\":[100,10]}");
    assertJson(Subtract(Arr(Value(100), Value(10))), "{\"subtract\":[100,10]}");
  }

  @Test
  public void shouldSerializeDivide() throws Exception {
    assertJson(Divide(Value(100), Value(10)), "{\"divide\":[100,10]}");
    assertJson(Divide(ImmutableList.of(Value(100), Value(10))), "{\"divide\":[100,10]}");
    assertJson(Divide(Arr(Value(100), Value(10))), "{\"divide\":[100,10]}");
  }

  @Test
  public void shouldSerializeModulo() throws Exception {
    assertJson(Modulo(Value(100), Value(10)), "{\"modulo\":[100,10]}");
    assertJson(Modulo(ImmutableList.of(Value(100), Value(10))), "{\"modulo\":[100,10]}");
    assertJson(Modulo(Arr(Value(100), Value(10))), "{\"modulo\":[100,10]}");
  }

  @Test
  public void shouldSerializeLT() throws Exception {
    assertJson(LT(Value(1), Value(2), Value(3)), "{\"lt\":[1,2,3]}");
    assertJson(LT(ImmutableList.of(Value(1), Value(2), Value(3))), "{\"lt\":[1,2,3]}");
    assertJson(LT(Arr(Value(1), Value(2), Value(3))), "{\"lt\":[1,2,3]}");
  }

  @Test
  public void shouldSerializeLTE() throws Exception {
    assertJson(LTE(Value(1), Value(2), Value(2)), "{\"lte\":[1,2,2]}");
    assertJson(LTE(ImmutableList.of(Value(1), Value(2), Value(2))), "{\"lte\":[1,2,2]}");
    assertJson(LTE(Arr(Value(1), Value(2), Value(2))), "{\"lte\":[1,2,2]}");
  }

  @Test
  public void shouldSerializeGT() throws Exception {
    assertJson(GT(Value(3), Value(2), Value(1)), "{\"gt\":[3,2,1]}");
    assertJson(GT(ImmutableList.of(Value(3), Value(2), Value(1))), "{\"gt\":[3,2,1]}");
    assertJson(GT(Arr(Value(3), Value(2), Value(1))), "{\"gt\":[3,2,1]}");
  }

  @Test
  public void shouldSerializeGTE() throws Exception {
    assertJson(GTE(Value(3), Value(2), Value(2)), "{\"gte\":[3,2,2]}");
    assertJson(GTE(ImmutableList.of(Value(3), Value(2), Value(2))), "{\"gte\":[3,2,2]}");
    assertJson(GTE(Arr(Value(3), Value(2), Value(2))), "{\"gte\":[3,2,2]}");
  }

  @Test
  public void shouldSerializeAnd() throws Exception {
    assertJson(And(Value(true), Value(true), Value(false)), "{\"and\":[true,true,false]}");
    assertJson(And(ImmutableList.of(Value(true), Value(true), Value(false))), "{\"and\":[true,true,false]}");
    assertJson(And(Arr(Value(true), Value(true), Value(false))), "{\"and\":[true,true,false]}");
  }

  @Test
  public void shouldSerializeOr() throws Exception {
    assertJson(Or(Value(true), Value(true), Value(false)), "{\"or\":[true,true,false]}");
    assertJson(Or(ImmutableList.of(Value(true), Value(true), Value(false))), "{\"or\":[true,true,false]}");
    assertJson(Or(Arr(Value(true), Value(true), Value(false))), "{\"or\":[true,true,false]}");
  }

  @Test
  public void shouldSerializeNot() throws Exception {
    assertJson(Not(Value(true)), "{\"not\":true}");
  }

  @Test
  public void shouldSerializeAt() throws Exception {
    assertJson(At(Value(1L), Get(Native.CLASSES)), "{\"at\":1,\"expr\":{\"get\":{\"@ref\":{\"id\":\"classes\"}}}}");
    assertJson(At(Time(Value("1970-01-01T00:00:00+00:00")), Get(Native.CLASSES)),
      "{\"at\":{\"time\":\"1970-01-01T00:00:00+00:00\"},\"expr\":{\"get\":{\"@ref\":{\"id\":\"classes\"}}}}");
  }

  private void assertJson(Expr expr, String jsonString) throws JsonProcessingException {
    assertThat(json.writeValueAsString(expr),
      equalTo(jsonString));
  }

}
