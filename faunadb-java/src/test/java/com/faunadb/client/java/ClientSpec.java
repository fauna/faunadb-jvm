package com.faunadb.client.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.java.errors.NotFoundQueryException;
import com.faunadb.client.java.query.Create;
import com.faunadb.client.java.query.Get;
import com.faunadb.client.java.query.Value;
import com.faunadb.client.java.query.Value.*;
import com.faunadb.client.java.response.Instance;
import com.faunadb.client.java.response.Key;
import com.faunadb.client.java.response.ResponseNode;
import com.faunadb.client.java.types.Ref;
import com.faunadb.httpclient.Connection;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class ClientSpec {
  static ImmutableMap<String, String> config = readConfig("config/test.yml");
  ObjectMapper json = new ObjectMapper();
  static FaunaClient rootClient;
  static FaunaClient client;
  static String testDbName = RandomStringUtils.randomAlphanumeric(8);

  static ImmutableMap<String, String> readConfig(String filename) {
    try {
      System.out.println(new File(".").getCanonicalPath());
      FileInputStream reader = new FileInputStream(filename);
      ImmutableMap<String, String> rv = ImmutableMap.copyOf(new Yaml().loadAs(reader, Map.class));
      reader.close();
      return rv;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @BeforeClass
  public static void beforeAll() throws IOException, ExecutionException, InterruptedException {
    rootClient = FaunaClient.create(Connection.builder().withFaunaRoot(config.get("root_url")).withAuthToken(config.get("root_token")).build());
    ListenableFuture<ResponseNode> dbCreateF = rootClient.query(Create.create(RefV.create("databases"), ObjectV.create("name", StringV.create(testDbName))));
    ResponseNode dbCreateR = dbCreateF.get();
    Ref dbRef = dbCreateR.asDatabase().ref();

    ListenableFuture<ResponseNode> keyCreateF = rootClient.query(Create.create(RefV.create("keys"), ObjectV.create("database", RefV.create(dbRef), "role", StringV.create("server"))));
    ResponseNode keyCreateR = keyCreateF.get();
    Key key = keyCreateR.asKey();

    client = FaunaClient.create(Connection.builder().withFaunaRoot(config.get("root_url")).withAuthToken(key.secret()).build());

    ListenableFuture<ResponseNode> classCreateF = client.query(Create.create(RefV.create("classes"), ObjectV.create("name", StringV.create("spells"))));
    classCreateF.get();

    ListenableFuture<ResponseNode> indexCreateF = client.query(Create.create(RefV.create("indexes"), ObjectV.create(
      "name", StringV.create("spells_by_test"),
      "source", RefV.create("classes/spells"),
      "path", StringV.create("class"),
      "unique", BooleanV.create(false))));

    indexCreateF.get();

    ListenableFuture<ResponseNode> setIndexF = client.query(Create.create(RefV.create("indexes"), ObjectV.create(
      "name", StringV.create("spells_instances"),
      "source", RefV.create("classes/spells"),
      "path", StringV.create("class"),
      "unique", BooleanV.create(false)
    )));

    setIndexF.get();

    ListenableFuture<ResponseNode> uniqueIndexF = client.query(Create.create(RefV.create("indexes"), ObjectV.create(
      "name", StringV.create("spells_by_unique_test"),
      "source", RefV.create("classes/spells"),
      "path", StringV.create("data.uniqueTest1"),
      "unique", BooleanV.create(true)
    )));

    uniqueIndexF.get();
  }

  @Test(expected=NotFoundQueryException.class)
  public void testLookupMissingInstance() throws Throwable {
    ListenableFuture<ResponseNode> resp = client.query(Get.create(RefV.create("classes/spells/1234")));
    try {
      resp.get();
    } catch (ExecutionException ex) {
      throw ex.getCause();
    }
  }

  @Test
  public void testCreateNewInstance() throws IOException, ExecutionException, InterruptedException {
    ListenableFuture<ResponseNode> respF = client.query(Create.create(RefV.create("classes/spells"), ObjectV.create("data", ObjectV.create("testField", StringV.create("testValue")))));
    Instance resp = respF.get().asInstance();

    assertThat(resp.ref().value(), startsWith("classes/spells/"));
    assertThat(resp.classRef().value(), is("classes/spells"));
    assertThat(resp.data().get("testField").asString(), is("testValue"));
  }
}
