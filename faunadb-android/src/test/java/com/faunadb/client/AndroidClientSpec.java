package com.faunadb.client;

import com.faunadb.client.errors.UnauthorizedException;
import com.faunadb.client.dsl.FaunaDBTest;
import com.faunadb.client.types.Value;
import org.junit.Test;

import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.types.Codec.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class AndroidClientSpec extends FaunaDBTest {

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

}