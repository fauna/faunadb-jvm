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
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Field;
import com.faunadb.client.types.Value;

import java.util.Arrays;
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
            Do(
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

        //The results at 'ref' are are a resource pointer to the class that was just created.
        Value hippoRef = addHippoResults.at("ref");
        System.out.println("hippoRef = " + hippoRef);

        /*
         * Read the hippo back that we just created
         */

        Value getHippoResults = client.query(
            Get(hippoRef)
        ).get();
        System.out.println("Hippo Spells:\n " + toPrettyJson(getHippoResults) + "\n");

        //convert the hippo results into primitive elements
        Value data = getHippoResults.get(Field.at("data"));
        String element = data.get(Field.at("element")).to(String.class).get();
        System.out.println("spell element = " + element);

        /*
         * Query for all the spells in the index
         */
        Value queryIndexResults = client.query(
            SelectAll(Path("data", "id"),
                Paginate(
                    Match(Index(Value(INDEX_NAME)))
                ))
        ).get();

        Collection<String>  spellsRefIds = queryIndexResults.asCollectionOf(String.class).get();
        System.out.println("spellsRefIds = " + spellsRefIds);

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
        Value dragonRef = storeSpellResult.at("ref");
        Value getDragonResult = client.query(
            Select(Value("data"),
                Get(dragonRef)
            )
        ).get();
        Spell spell = getDragonResult.to(Spell.class).get();
        System.out.println("dragon spell: " + spell);

        /*
         * Store a list of Spells
         */

        Spell spellOne = new Spell("Chill Touch", "ice", 18);
        Spell spellTwo = new Spell("Dancing Lights", "fire", 45);
        Spell spellThree = new Spell("Fire Bolt", "fire", 32);
        List<Spell> spellList = Arrays.asList(spellOne, spellTwo, spellThree);

        //Lambda Variable for each spell
        String NXT_SPELL = "NXT_SPELL";

        //Encode the list of spells into an expression
        Expr encodedSpellsList = Value(spellList);

        //This query can be approximately read as for each spell in the list of spells evaluate the lambda function.
        //That lambda function creates a temporary variable with each spell in the list and passes it to the create function.
        //The create function then stores that spell in the database
        Value spellsListSave = client.query(
            Foreach(
                encodedSpellsList,
                Lambda(Value(NXT_SPELL),
                    Create(
                        Class(Value(SPELLS_CLASS)),
                        Obj("data", Var(NXT_SPELL))
                    )
                )
            )
        ).get();

        System.out.println("Created list of spells from java list: \n" + toPrettyJson(spellsListSave));
        Collection<Spell> spellCollection = spellsListSave.asCollectionOf(Spell.class).get();
        System.out.println("save " + spellCollection.size() + " spells:");
        spellCollection.forEach(nextSpell -> System.out.println("   " + nextSpell));

        System.out.println("\n");

        /*
         * Read all Spells for the Spells Index
         */

        //Lambda Variable for each spell ref
        String REF_SPELL_ID = "NXT_SPELL";

        //Select causes the return data to be stored in the data field that is expected when the data is covered to a collection
        //The Map is equivalent to a functional map which maps over the set of all values returned by the paginate.
        //Then for each value in the list it runs the lambda function which gets and returns the value.
        Value findAllSpells = client.query(
            Select(Value("data"),
                Map(
                    Paginate(
                        Match(Index(Value(INDEX_NAME)))
                    ),
                    Lambda(Value(REF_SPELL_ID), Select(Value("data"), Get(Var(REF_SPELL_ID))))
                )
            )
        ).get();

        Collection<Spell> allSpellsCollection = findAllSpells.asCollectionOf(Spell.class).get();
        System.out.println("read " + allSpellsCollection.size() + " spells:");
        allSpellsCollection.forEach(nextSpell -> System.out.println("   " + nextSpell));

        System.out.println("\n");

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
