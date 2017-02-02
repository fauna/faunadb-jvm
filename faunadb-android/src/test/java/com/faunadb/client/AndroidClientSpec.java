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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.faunadb.client.database.SetupDatabase.createDatabase;
import static com.faunadb.client.database.SetupDatabase.createServerKey;
import static com.faunadb.client.database.SetupDatabase.dropDatabase;
import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.types.Codec.BOOLEAN;
import static com.faunadb.client.types.Codec.STRING;
import static com.faunadb.client.types.Value.NullV.NULL;
import static com.google.common.base.Functions.constant;
import static com.google.common.util.concurrent.Futures.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;

public class AndroidClientSpec extends DslSpec {
  private static final String DB_NAME = "faunadb-java-android-test";
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

    FaunaClient client = createFaunaClient("invalid-secret");

    try {
      client.query(Get(Ref("classes/spells/1234"))).get();
    } finally {
      client.close();
    }
  }

  @Test
  public void shouldThrowPermissionDeniedException() throws Exception {
    thrown.expectCause(isA(PermissionDeniedException.class));

    Value key = rootClient.query(Create(Ref("keys"), Obj("database", DB_REF, "role", Value("client")))).get();

    FaunaClient client = createFaunaClient(key.get(Field.at("secret").to(STRING)));

    client.query(Paginate(Ref("databases"))).get();
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

    FaunaClient sessionClient = client.newSessionClient(secret);

    try {
      Value loggedOut = sessionClient.query(Logout(Value(true))).get();
      assertThat(loggedOut.to(BOOLEAN).get(), is(true));

      Value identified = client.query(
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
  public void shouldGetKeyFromSecret() throws Exception {
    Value key = rootClient.query(
      CreateKey(Obj("database", DB_REF, "role", Value("server")))
    ).get();

    Value secret = key.at("secret");

    assertThat(rootClient.query(Get(key.get(REF_FIELD))).get(),
      equalTo(rootClient.query(KeyFromSecret(secret)).get()));
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
