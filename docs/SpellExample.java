/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fauna.learn;

/*
 * These imports are for basic functionality around logging and JSON handling and Futures.
 * They should best be thought of as a convenience items for our demo apps.
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.FaunaClient;
import com.faunadb.client.types.Field;
import com.faunadb.client.types.Value;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.faunadb.client.query.Language.*;

/*
 * These are the required imports for Fauna.
 *
 * For these examples we are using the 2.1.0 version of the JVM driver. Also notice that we aliasing
 * the query and values part of the API to make it more obvious we we are using Fauna functionality.
 *
 */

public class SpellExample {

    private static ObjectMapper mapper = getMapper();

    private static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    private static String toPrettyJson(Value value) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }

    public static void main(String[] args) throws Exception {
        /*
         * Create an admin connection to FaunaDB.
         *
         * If you are using the FaunaDB-Cloud version:
         *  - remove the 'withEndpoint line below
         *  - substitute your secret for "secret" below
         */
        FaunaClient adminClient = FaunaClient.builder()
            .withEndpoint("http://127.0.0.1:8443")
            .withSecret("secret")
            .build();
        System.out.println("Succesfully connected to FaunaDB as Admin!");


        /*
         * Create a database
         */

        String DB_NAME = "demo";

        Value dbResults = adminClient.query(
            Arr(
                If(
                    Exists(Database(DB_NAME)),
                    Delete(Database(DB_NAME)),
                    Value(true)
                ),
                CreateDatabase(Obj("name", Value(DB_NAME)))
            )
        ).get();
        System.out.println("Successfully created database: " + DB_NAME + ":\n " + toPrettyJson(dbResults));

        /*
         *  Create a client connection to the demo database
         */
        Value keyResults = adminClient.query(
            CreateKey(Obj("database", Database(Value(DB_NAME)), "role", Value("server")))
        ).get();

        String key = keyResults.at("secret").to(String.class).get();
        FaunaClient client = adminClient.newSessionClient(key);
        System.out.println("Connected to Fauna database " + DB_NAME + " with server role\n");
        adminClient.close();


        /*
         * Create the spell class and index
         */
        String SPELLS_CLASS = "spells";
        String INDEX_NAME = "spells_index";

        Value classResults = client.query(
            CreateClass(
                Obj("name", Value(SPELLS_CLASS))
            )
        ).get();
        System.out.println("Create Class for " + DB_NAME + ":\n " + toPrettyJson(classResults) + "\n");

        Value indexResults = client.query(
            CreateIndex(
                Obj("name", Value(INDEX_NAME), "source", Class(Value(SPELLS_CLASS))
                )
            )
        ).get();
        System.out.println("Create Index for " + DB_NAME + ":\n " + toPrettyJson(indexResults) + "\n");


        /*
         * Add some entries to the spells class
         */

        Value addFireResults = client.query(
            Create(
                Class(Value(SPELLS_CLASS)),
                Obj("data",
                    Obj("name", Value("Fire Beak"), "element", Value("water"), "cost", Value(15))
                )
            )
        ).get();
        System.out.println("Added spell to class " + SPELLS_CLASS + ":\n " + toPrettyJson(addFireResults) + "\n");


        Value addDragonResults = client.query(
            Create(
                Class(Value(SPELLS_CLASS)),
                Obj("data",
                    Obj("name", Value("Water Dragon's Claw"), "element", Value("water"), "cost", Value(25))
                )
            )
        ).get();
        System.out.println("Added spell to class " + SPELLS_CLASS + ":\n " + toPrettyJson(addDragonResults) + "\n");

        Value addHippoResults = client.query(
            Create(
                Class(Value(SPELLS_CLASS)),
                Obj("data",
                    Obj("name", Value("Hippo's Wallow"), "element", Value("water"), "cost", Value(35))
                )
            )
        ).get();
        System.out.println("Added spell to class " + SPELLS_CLASS + ":\n " + toPrettyJson(addHippoResults) + "\n");

        Value.RefV hippoRef = addHippoResults.at("ref").to(Value.RefV.class).get();
        System.out.println("hippoRef = " + hippoRef);

        /*
         * Read the hippo back that we just created
         */

        Value getHippoResults = client.query(
            Get(Ref(
                Class("spells"),
                Value(hippoRef.getId())
                )
            )
        ).get();
        System.out.println("Hippo Spells:\n " + toPrettyJson(getHippoResults) + "\n");

        //convert the hippo results into primitive elements
        Value data = getHippoResults.get(Field.at("data"));
        String element = data.get(Field.at("element")).to(String.class).get();
        System.out.println("spell element = " + element);

        Value.ObjectV objectV = getHippoResults.at("data").to(Value.ObjectV.class).get();
        System.out.println("objectV = " + objectV);

        /*
         * Query for all the spells in the index
         */
        Value queryIndexResults = client.query(
            Select(Value("data"),
                Paginate(
                    Match(Index(Value(INDEX_NAME)))
                ))
        ).get();
        System.out.println("All spells:\n " + toPrettyJson(queryIndexResults) + "\n");

        //convert the query results to a collection of refrences
        Collection<Value.RefV> refVCollection = queryIndexResults.asCollectionOf(Value.RefV.class).get();
        List<String> listOfIds = refVCollection.stream().map(refV -> refV.getId()).collect(Collectors.toList());
        System.out.println("Spell ref ids = " + listOfIds);

        /*
         * Store a Spell java class
         */
        Spell newSpell = new Spell("Water Dragon's Claw", "water", 25);
        Value storeSpellResult = client.query(
            Create(
                Class(Value(SPELLS_CLASS)),
                Obj("data", Value(newSpell))
            )
        ).get();
        System.out.println("Stored spell:\n " + toPrettyJson(storeSpellResult));

        /*
         * Read the spell we just created
         */
        Value.RefV dragonRef = storeSpellResult.at("ref").to(Value.RefV.class).get();
        Value getDragonResult = client.query(
            Select(Value("data"),
                Get(Ref(
                    Class(SPELLS_CLASS),
                    Value(dragonRef.getId())
                    )
                )
            )
        ).get();
        Spell spell = getDragonResult.to(Spell.class).get();
        System.out.println("dragon spell: " + spell);

        /*
         * Just to keep things neat and tidy, delete the database and close the client connection
         */
        deleteDB(adminClient, DB_NAME);
        System.out.println("Disconnected from FaunaDB as Admin!");

        // add this at the end of execution to make things shut down nicely
        System.exit(0);
    }

    private static void deleteDB(FaunaClient adminClient, String dbName) throws InterruptedException, java.util.concurrent.ExecutionException, JsonProcessingException {
        /*
         * Delete the Database created
         */
        Value result = adminClient.query(
            If(
                Exists(Database(dbName)),
                Delete(Database(dbName)),
                Value(true)
            )
        ).get();
        System.out.println("Deleted database: " + dbName + ":\n " + toPrettyJson(result));
    }
}
