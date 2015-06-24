package com.faunadb.client.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.faunadb.client.query.Ref

import scala.annotation.meta.{field, param}

case class Instance(@(JsonProperty)("ref") ref: Ref,
                    @(JsonProperty @field @param)("class") classRef: Ref,
                    @JsonProperty("ts") ts: Long,
                    @JsonProperty("data") data: ResponseMap)

case class Key(@JsonProperty("ref") ref: Ref,
               @(JsonProperty @field @param)("class") classRef: Ref,
               @JsonProperty("database") database: Ref,
               @JsonProperty("role") role: String,
               @JsonProperty("secret") secret: String,
               @(JsonProperty @field @param)("hashed_secret") hashedSecret: String,
               @JsonProperty("ts") ts: Long,
               @JsonProperty("data") data: ResponseMap)

case class Database(@JsonProperty("ref") ref: Ref,
                    @(JsonProperty @field @param)("class") classRef: Ref,
                    @JsonProperty("ts") ts: Long,
                    @JsonProperty("name") name: String)

case class Class(@JsonProperty("ref") ref: Ref,
                 @(JsonProperty @field @param)("class") classRef: Ref,
                 @JsonProperty("ts") ts: Long,
                 @(JsonProperty @field @param)("history_days") historyDays: Long,
                 @JsonProperty("name") name: String)

case class Index(@JsonProperty("ref") ref: Ref,
                 @(JsonProperty @field @param)("class") classRef: Ref,
                 @JsonProperty("ts") ts: Long,
                 @JsonProperty("unique") unique: Boolean,
                 @JsonProperty("active") active: Boolean,
                 @JsonProperty("name") name: String,
                 @JsonProperty("source") source: Ref,
                 @JsonProperty("path") path: String,
                 @JsonProperty("terms") terms: Seq[Map[String, String]])

case class Set(@JsonProperty("@set") parameters: ResponseMap)
