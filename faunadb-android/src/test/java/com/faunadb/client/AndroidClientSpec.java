package com.faunadb.client;

import com.faunadb.client.dsl.DslSpec;
import com.faunadb.client.errors.BadRequestException;
import com.faunadb.client.errors.PermissionDeniedException;
import com.faunadb.client.errors.UnauthorizedException;
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Field;
import com.faunadb.client.types.Value;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.faunadb.client.query.Language.Class;
import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.types.Codec.*;
import static com.faunadb.client.types.Value.Native;
import static com.faunadb.client.types.Value.NullV.NULL;
import static com.faunadb.client.types.Value.RefV;
import static com.google.common.base.Functions.constant;
import static com.google.common.util.concurrent.Futures.catching;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;

public class AndroidClientSpec extends DslSpec {
  private static final String DB_NAME = "faunadb-java-android-test";
  private static final Expr DB_REF = Database(DB_NAME);

  private static FaunaClient rootClient;
  private static FaunaClient serverClient;
  private static FaunaClient adminClient;

  @BeforeClass
  public static void setUpClient() throws Exception {
    rootClient = createFaunaClient(ROOT_TOKEN);

    catching(rootClient.query(Delete(DB_REF)), BadRequestException.class, constant(NULL)).get();
    rootClient.query(CreateDatabase(Obj("name", Value(DB_NAME)))).get();

    Value serverKey = rootClient.query(CreateKey(Obj("database", DB_REF, "role", Value("server")))).get();
    Value adminKey = rootClient.query(CreateKey(Obj("database", DB_REF, "role", Value("admin")))).get();

    serverClient = rootClient.newSessionClient(serverKey.get(SECRET_FIELD));
    adminClient = rootClient.newSessionClient(adminKey.get(SECRET_FIELD));
  }

  @AfterClass
  public static void closeClients() throws Exception {
    catching(rootClient.query(Delete(DB_REF)), BadRequestException.class, constant(NULL)).get();
    rootClient.close();
    serverClient.close();
    adminClient.close();
  }

  @Test
  public void shouldThrowUnauthorizedOnInvalidSecret() throws Exception {
    thrown.expectCause(isA(UnauthorizedException.class));

    FaunaClient client = createFaunaClient("invalid-secret");

    try {
      client.query(Get(Ref(Class("spells"), "1234"))).get();
    } finally {
      client.close();
    }
  }

  @Test
  public void shouldThrowPermissionDeniedException() throws Exception {
    thrown.expectCause(isA(PermissionDeniedException.class));

    Value key = rootClient.query(CreateKey(Obj("database", DB_REF, "role", Value("client")))).get();

    FaunaClient client = createFaunaClient(key.get(SECRET_FIELD));

    client.query(Paginate(Databases())).get();
  }

  @Test
  public void shouldThrowIllegalStateWhenCreateNewSessionClientOnClosedClient() throws IOException {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Can not create a session connection from a closed http connection");

    FaunaClient client = createFaunaClient("secret");
    client.close();

    client.newSessionClient("new-secret");
  }

  @Test
  public void shouldAuthenticateSession() throws Exception {
    Value createdInstance = serverClient.query(
      Create(onARandomClass(),
        Obj("credentials",
          Obj("password", Value("abcdefg"))))
    ).get();

    Value auth = serverClient.query(
      Login(
        createdInstance.get(REF_FIELD),
        Obj("password", Value("abcdefg")))
    ).get();

    String secret = auth.get(SECRET_FIELD);

    FaunaClient sessionClient = serverClient.newSessionClient(secret);

    try {
      Value loggedOut = sessionClient.query(Logout(Value(true))).get();
      assertThat(loggedOut.to(BOOLEAN).get(), is(true));

      Value identified = serverClient.query(
        Identify(
          createdInstance.get(REF_FIELD),
          Value("wrong-password")
        )
      ).get();

      assertThat(identified.to(BOOLEAN).get(), is(false));
    } finally {
      sessionClient.close();
    }
  }

  @Test
  public void shouldTestHasIdentity() throws Exception {
    Value createdInstance = serverClient.query(
      Create(onARandomClass(),
        Obj("credentials",
          Obj("password", Value("sekret"))))
    ).get();

    Value auth = serverClient.query(
      Login(
        createdInstance.get(REF_FIELD),
        Obj("password", Value("sekret")))
    ).get();

    String secret = auth.get(SECRET_FIELD);

    FaunaClient sessionClient = serverClient.newSessionClient(secret);

    try {
      assertThat(
        sessionClient.query(HasIdentity()).get().to(BOOLEAN).get(),
        equalTo(true)
      );
    } finally {
      sessionClient.close();
    }
  }

  @Test
  public void shouldTestIdentity() throws Exception {
    Value createdInstance = serverClient.query(
      Create(onARandomClass(),
        Obj("credentials",
          Obj("password", Value("sekret"))))
    ).get();

    Value auth = serverClient.query(
      Login(
        createdInstance.get(REF_FIELD),
        Obj("password", Value("sekret")))
    ).get();

    String secret = auth.get(SECRET_FIELD);

    FaunaClient sessionClient = serverClient.newSessionClient(secret);

    try {
      assertThat(
        sessionClient.query(Identity()).get(),
        equalTo((Value) createdInstance.get(REF_FIELD))
      );
    } finally {
      sessionClient.close();
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
    FaunaClient client1 = createNewDatabase(adminClient, "parent-database");
    createNewDatabase(client1, "child-database");

    Value key = client1.query(CreateKey(Obj("database", Database(Value("child-database")), "role", Value("server")))).get();

    FaunaClient client2 = client1.newSessionClient(key.get(SECRET_FIELD));

    client2.query(CreateClass(Obj("name", Value("a_class")))).get();

    Expr nestedDatabase = Database("child-database", Database("parent-database"));

    Expr nestedClassRef = Class("a_class", nestedDatabase);

    assertThat(
      serverClient.query(Exists(nestedClassRef)).get().to(BOOLEAN).get(),
      equalTo(true)
    );

    Expr allNestedClasses = Classes(nestedDatabase);

    List<RefV> results = new ArrayList();

    results.add(new RefV("a_class", Native.CLASSES,
                         new RefV("child-database", Native.DATABASES,
                                  new RefV("parent-database", Native.DATABASES))));

    assertThat(
      serverClient.query(Paginate(allNestedClasses)).get().get(REF_LIST),
      equalTo(results)
    );
  }

  @Test
  public void shouldTestNestedKeys() throws Exception {
    FaunaClient client = createNewDatabase(adminClient, "db-for-keys");

    client.query(CreateDatabase(Obj("name", Value("db-test")))).get();

    Value serverKey = client.query(CreateKey(Obj("database", Database("db-test"), "role", Value("server")))).get();
    Value adminKey = client.query(CreateKey(Obj("database", Database("db-test"), "role", Value("admin")))).get();

    assertThat(
      client.query(Paginate(Keys())).get().get(DATA).to(ARRAY).get(),
      CoreMatchers.<Value>hasItems(serverKey.get(REF_FIELD), adminKey.get(REF_FIELD))
    );

    assertThat(
      adminClient.query(Paginate(Keys(Database("db-for-keys")))).get().get(DATA).to(ARRAY).get(),
      CoreMatchers.<Value>hasItems(serverKey.get(REF_FIELD), adminKey.get(REF_FIELD))
    );
  }

  private FaunaClient createNewDatabase(FaunaClient client, String name) throws Exception {
    client.query(CreateDatabase(Obj("name", Value(name)))).get();
    Value key = client.query(CreateKey(Obj("database", Database(Value(name)), "role", Value("admin")))).get();
    return client.newSessionClient(key.get(SECRET_FIELD));
  }

  @Override
  protected ListenableFuture<Value> query(Expr expr) {
    return serverClient.query(expr);
  }

  @Override
  protected ListenableFuture<List<Value>> query(List<? extends Expr> exprs) {
    return serverClient.query(exprs);
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
