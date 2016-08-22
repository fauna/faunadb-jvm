package com.faunadb.client.test;

import com.faunadb.client.FaunaClient;
import com.faunadb.client.errors.BadRequestException;
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Value;
import com.faunadb.client.types.Value.RefV;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.types.Codec.REF;
import static com.faunadb.client.types.Codec.STRING;
import static com.faunadb.client.types.Value.NullV.NULL;
import static com.google.common.base.Functions.constant;
import static com.google.common.util.concurrent.Futures.*;
import static java.lang.String.format;

public class FaunaDBTest {

  private static final String DB_NAME = "faunadb-java-test";
  private static final Expr DB_REF = Ref("databases/" + DB_NAME);
  private static final String ROOT_TOKEN;
  private static final String ROOT_URL;

  private static FaunaClient rootClient;
  protected static FaunaClient client;

  static {
    ROOT_TOKEN = EnvVariables.require("FAUNA_ROOT_KEY");
    ROOT_URL = format(
      "%s://%s:%s",
      EnvVariables.getOrElse("FAUNA_SCHEME", "https"),
      EnvVariables.getOrElse("FAUNA_DOMAIN", "cloud.faunadb.com"),
      EnvVariables.getOrElse("FAUNA_PORT", "443")
    );
  }

  @BeforeClass
  public static void setUpClient() throws Exception {
    rootClient = createFaunaClient(ROOT_TOKEN);

    client = transform(
      transformAsync(
        setupDatabase(rootClient),
        createServerKey(rootClient)
      ),
      createClientWithServerKey(rootClient)
    ).get();
  }

  @AfterClass
  public static void closeClients() throws Exception {
    dropDatabase(rootClient).get();
    rootClient.close();
    client.close();
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

  private static ListenableFuture<Value> setupDatabase(FaunaClient rootClient) {
    return transformAsync(
      dropDatabase(rootClient),
      createDatabase(rootClient)
    );
  }

  private static ListenableFuture<Value> dropDatabase(FaunaClient rootClient) {
    ListenableFuture<Value> delete = rootClient.query(Delete(DB_REF));
    return catching(delete, BadRequestException.class, constant(NULL));
  }

  private static AsyncFunction<Value, Value> createDatabase(final FaunaClient rootClient) {
    return new AsyncFunction<Value, Value>() {
      @Override
      public ListenableFuture<Value> apply(Value ign) throws Exception {
        return rootClient.query(
          Create(
            Ref("databases"),
            Obj("name", Value(DB_NAME))
          )
        );
      }
    };
  }

  private static AsyncFunction<Value, Value> createServerKey(final FaunaClient rootClient) {
    return new AsyncFunction<Value, Value>() {
      @Override
      public ListenableFuture<Value> apply(Value dbCreateR) throws Exception {
        RefV dbRef = dbCreateR.at("ref").to(REF).get();

        return rootClient.query(
          Create(
            Ref("keys"),
            Obj("database", dbRef,
              "role", Value("server"))
          )
        );
      }
    };
  }

  private static Function<Value, FaunaClient> createClientWithServerKey(final FaunaClient rootClient) {
    return new Function<Value, FaunaClient>() {
      @Override
      public FaunaClient apply(Value serverKeyF) {
        String secret = serverKeyF.at("secret").to(STRING).get();
        return rootClient.newSessionClient(secret);
      }
    };
  }

}
