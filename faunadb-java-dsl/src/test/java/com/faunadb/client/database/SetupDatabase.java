package com.faunadb.client.database;

import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Value;

import static com.faunadb.client.query.Language.*;
import static com.faunadb.client.types.Codec.REF;

public class SetupDatabase {

  public static Expr dropDatabase(Expr dbRef) {
    return Delete(dbRef);
  }

  public static Expr createDatabase(String dbName) {
    return Create(
      Ref("databases"),
      Obj("name", Value(dbName))
    );
  }

  public static Expr createServerKey(Value dbCreate) {
    Value.RefV dbRef = dbCreate.at("ref").to(REF).get();

    return Create(
        Ref("keys"),
        Obj("database", dbRef, "role", Value("server"))
      );
  }
}
