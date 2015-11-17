package faunadb.response

import com.fasterxml.jackson.annotation.JsonProperty
import faunadb.types.{LazyValueMap, LazyValue, Ref}

import scala.annotation.meta.{field, param}

/**
 * A FaunaDB instance response.
 *
 * This, like other response types, is obtained  by coercing a [[LazyValue]] using
 * the conversion methods [[LazyValue.asInstance]] or [[LazyValue.asInstanceOpt]].
 */
case class Instance(@(JsonProperty)("ref") ref: Ref,
                    @(JsonProperty @field @param)("class") classRef: Ref,
                    @JsonProperty("ts") ts: Long,
                    @JsonProperty("data") data: LazyValueMap)

/**
 * A FaunaDB key response.
 *
 * This, like other response types, is obtained by coercing a [[LazyValue]] using
 * the conversion methods [[LazyValue.asKey]] or [[LazyValue.asKeyOpt]].
 */
case class Key(@JsonProperty("ref") ref: Ref,
               @(JsonProperty @field @param)("class") classRef: Ref,
               @JsonProperty("database") database: Ref,
               @JsonProperty("role") role: String,
               @JsonProperty("secret") secret: String,
               @(JsonProperty @field @param)("hashed_secret") hashedSecret: String,
               @JsonProperty("ts") ts: Long,
               @JsonProperty("data") data: LazyValueMap)

/**
 * A FaunaDB database response.
 *
 * This, like other response types, is obtained by coercing a [[LazyValue]] using its associated conversion methods,
 * [[LazyValue.asDatabase]] or [[LazyValue.asDatabaseOpt]].
 */
case class Database(@JsonProperty("ref") ref: Ref,
                    @(JsonProperty @field @param)("class") classRef: Ref,
                    @JsonProperty("ts") ts: Long,
                    @JsonProperty("name") name: String)

case class Token(@JsonProperty("ref") ref: Ref,
                 @(JsonProperty @field @param)("class") classRef: Ref,
                 @JsonProperty("ts") ts: Long,
                 @JsonProperty("credentials") credentials: Ref,
                 @JsonProperty("secret") secret: String)

/**
 * A FaunaDB class response.
 *
 * This, like other response types, is obtained by coercing a [[LazyValue]] using its associated conversion methods,
 * [[LazyValue.asClass]] or [[LazyValue.asClassOpt]].
 */
case class Class(@JsonProperty("ref") ref: Ref,
                 @(JsonProperty @field @param)("class") classRef: Ref,
                 @JsonProperty("ts") ts: Long,
                 @(JsonProperty @field @param)("history_days") historyDays: Long,
                 @JsonProperty("name") name: String)

/**
 * A FaunaDB index response.
 *
 * This, like other response types, is obtained by coercing a [[LazyValue]] using its associated conversion methods,
 * [[LazyValue.asIndex]] or [[LazyValue.asIndexOpt]]
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
case class Set(@JsonProperty("@set") parameters: LazyValueMap)
