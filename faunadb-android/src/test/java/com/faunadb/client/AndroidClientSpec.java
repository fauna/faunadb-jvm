package com.faunadb.client;

import com.faunadb.client.dsl.DslSpec;
import com.faunadb.client.errors.UnauthorizedException;
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Value;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.types.Codec.BOOLEAN;
import static com.faunadb.client.types.Codec.STRING;
import static com.google.common.util.concurrent.Futures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;

public class AndroidClientSpec extends DslSpec {

  private static final String DB_NAME = "faunadb-java-android-test";
  private static final Expr DB_REF = Ref("databases/" + DB_NAME);

  private static FaunaClient rootClient;
  private static FaunaClient client;

  @Test
  public void shouldThrowUnauthorizedOnInvalidSecret() throws Exception {
    thrown.expectCause(isA(UnauthorizedException.class));

    createFaunaClient("invalid-secret")
      .query(Get(Ref("classes/spells/1234")))
      .get();
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
    Value loggedOut = sessionClient.query(Logout(Value(true))).get();
    assertThat(loggedOut.to(BOOLEAN).get(), is(true));

    Value identified = client.query(
      Identify(
        createdInstance.get(REF_FIELD),
        Value("wrong-password")
      )
    ).get();

    assertThat(identified.to(BOOLEAN).get(), is(false));
  }

  @Before
  public void setUpClient() throws Exception {
    if (rootClient != null)
      return;

    rootClient = createFaunaClient(ROOT_TOKEN);

    client = transform(
      transformAsync(
        setupDatabase(DB_REF, DB_NAME),
        createServerKey()
      ),
      createClientWithServerKey()
    ).get();

    setUpSchema();
  }

  @AfterClass
  public static void closeClients() throws Exception {
    rootClient.query(Delete(DB_REF)).get();
  }

  protected ListenableFuture<Value> queryRoot(Expr expr) {
    return rootClient.query(expr);
  }

  protected ListenableFuture<Value> query(Expr expr) {
    return client.query(expr);
  }

  protected ListenableFuture<ImmutableList<Value>> query(List<? extends Expr> exprs) {
    return client.query(exprs);
  }

  protected static FaunaClient createFaunaClient(String secret) {
    try {
      return FaunaClient.builder()
        .withEndpoint(ROOT_URL)
        .withSecret(secret)
        .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Function<Value, FaunaClient> createClientWithServerKey() {
    return new Function<Value, FaunaClient>() {
      @Override
      public FaunaClient apply(Value serverKeyF) {
        String secret = serverKeyF.at("secret").to(STRING).get();
        return rootClient.newSessionClient(secret);
      }
    };
  }

}
