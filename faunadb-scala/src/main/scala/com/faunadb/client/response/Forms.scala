package com.faunadb.client.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.faunadb.client.query.Ref

import scala.annotation.meta.{field, param}

/**
 * A FaunaDB instance response.
 *
 * This, like other response types, is obtained  by coercing a [[ResponseNode]] using
 * the conversion methods [[ResponseNode.asInstance]] or [[ResponseNode.asInstanceOpt]].
 */
case class Instance(@(JsonProperty)("ref") ref: Ref,
                    @(JsonProperty @field @param)("class") classRef: Ref,
                    @JsonProperty("ts") ts: Long,
                    @JsonProperty("data") data: ResponseMap)

/**
 * A FaunaDB key response.
 *
 * This, like other response types, is obtained by coercing a [[ResponseNode]] using
 * the conversion methods [[ResponseNode.asKey]] or [[ResponseNode.asKeyOpt]].
 */
case class Key(@JsonProperty("ref") ref: Ref,
               @(JsonProperty @field @param)("class") classRef: Ref,
               @JsonProperty("database") database: Ref,
               @JsonProperty("role") role: String,
               @JsonProperty("secret") secret: String,
               @(JsonProperty @field @param)("hashed_secret") hashedSecret: String,
               @JsonProperty("ts") ts: Long,
               @JsonProperty("data") data: ResponseMap)

/**
 * A FaunaDB database response.
 *
 * This, like other response types, is obtained by coercing a [[ResponseNode]] using its associated conversion methods,
 * [[ResponseNode.asDatabase]] or [[ResponseNode.asDatabaseOpt]].
 */
case class Database(@JsonProperty("ref") ref: Ref,
                    @(JsonProperty @field @param)("class") classRef: Ref,
                    @JsonProperty("ts") ts: Long,
                    @JsonProperty("name") name: String)

/**
 * A FaunaDB class response.
 *
 * This, like other response types, is obtained by coercing a [[ResponseNode]] using its associated conversion methods,
 * [[ResponseNode.asClass]] or [[ResponseNode.asClassOpt]].
 */
case class Class(@JsonProperty("ref") ref: Ref,
                 @(JsonProperty @field @param)("class") classRef: Ref,
                 @JsonProperty("ts") ts: Long,
                 @(JsonProperty @field @param)("history_days") historyDays: Long,
                 @JsonProperty("name") name: String)

/**
 * A FaunaDB index response.
 *
 * This, like other response types, is obtained by coercing a [[ResponseNode]] using its associated conversion methods,
 * [[ResponseNode.asIndex]] or [[ResponseNode.asIndexOpt]]
 */
case class Index(@JsonProperty("ref") ref: Ref,
                 @(JsonProperty @field @param)("class") classRef: Ref,
                 @JsonProperty("ts") ts: Long,
                 @JsonProperty("unique") unique: Boolean,
                 @JsonProperty("active") active: Boolean,
                 @JsonProperty("name") name: String,
                 @JsonProperty("source") source: Ref,
                 @JsonProperty("path") path: String,
                 @JsonProperty("terms") terms: Seq[Map[String, String]])

/**
 * A FaunaDB set literal.
 */
case class Set(@JsonProperty("@set") parameters: ResponseMap)
