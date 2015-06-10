package com.faunadb.client.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.java.query.Create;
import com.faunadb.client.java.query.Quote;
import com.faunadb.client.java.query.Value;
import com.faunadb.client.java.query.Value.*;
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

public class ClientSpec {
  static ImmutableMap<String, String> config = readConfig("config/test.yml");
  ObjectMapper json = new ObjectMapper();
  static FaunaClient rootClient;
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
    ListenableFuture<ResponseNode> dbCreateF = rootClient.query(Create.create(RefV.create(Ref.create("databases")), Value.ObjectV.create(ImmutableMap.<String, Value>of("name", Value.StringV.create(testDbName)))));
    ResponseNode dbCreateR = dbCreateF.get();
    Ref dbRef = dbCreateR.asDatabase().ref();

    ListenableFuture<ResponseNode> keyCreateF = rootClient.query(Create.create(RefV.create(Ref.create("keys")), ObjectV.create(ImmutableMap.<String, Value>of("database", RefV.create(dbRef), "role", StringV.create("server")))));
    ResponseNode keyCreateR = dbCreateF.get();
    Key key = keyCreateR.asKey();

    System.out.println(key);
  }

  @Test
  public void sup() {
    System.out.println(config);
  }
}
