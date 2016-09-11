package com.faunadb.client.dsl;

import com.faunadb.client.FaunaClient;
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Value;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.List;

import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.types.Codec.STRING;
import static com.google.common.util.concurrent.Futures.*;

public abstract class FaunaDBTest extends DslSpec {

  private static final String DB_NAME = "faunadb-java-test";
  private static final Expr DB_REF = Ref("databases/" + DB_NAME);

  private static FaunaClient rootClient;
  protected static FaunaClient client;

  protected static Function<Expr, ListenableFuture<Value>> QUERY = new Function<Expr, ListenableFuture<Value>>() {
    @Override
    public ListenableFuture<Value> apply(Expr expr) {
      return client.query(expr);
    }
  };

  protected static Function<Expr, ListenableFuture<Value>> QUERY_ROOT = new Function<Expr, ListenableFuture<Value>>() {
    @Override
    public ListenableFuture<Value> apply(Expr expr) {
      return rootClient.query(expr);
    }
  };

  protected ListenableFuture<Value> query(Expr expr) {
    return client.query(expr);
  }

  protected ListenableFuture<ImmutableList<Value>> query(List<? extends Expr> exprs) {
    return client.query(exprs);
  }

  @BeforeClass
  public static void setUpClient() throws Exception {
    rootClient = createFaunaClient(ROOT_TOKEN);

    client = transform(
      transformAsync(
        setupDatabase(DB_REF, DB_NAME, QUERY_ROOT),
        createServerKey(QUERY_ROOT)
      ),
      createClientWithServerKey(rootClient)
    ).get();

    setUpSchema(QUERY);
  }

  @AfterClass
  public static void closeClients() throws Exception {
    dropDatabase(DB_REF, QUERY_ROOT).get();
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
