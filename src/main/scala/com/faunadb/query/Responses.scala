package com.faunadb.query

import com.fasterxml.jackson.annotation.JsonProperty

import scala.annotation.meta.field

trait Response

case class Instance(ref: Ref, @(JsonProperty @field)("class") classRef: Ref, ts: Long, data: ObjectPrimitive) extends Response
case class Key(ref: Ref, @(JsonProperty @field)("class") classRef: Ref, database: Ref, role: String, secret: String, @(JsonProperty @field)("hashed_secret") hashedSecret: String, ts: Long, data: ObjectPrimitive) extends Response
case class Database(ref: Ref, @(JsonProperty @field)("class") classRef: Ref, ts: Long, name: String) extends Response
case class Class(ref: Ref, @(JsonProperty @field)("class") classRef: Ref, ts: Long, @(JsonProperty @field)("history_days") historyDays: Long, name: String) extends Response
case class Index(ref: Ref, @(JsonProperty @field)("class") classRef: Ref, ts: Long, unique: Boolean, active: Boolean, name: String, source: Ref, path: String) extends Response
