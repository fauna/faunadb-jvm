package com.faunadb.client.response

import com.fasterxml.jackson.annotation.JsonProperty

case class Parameter(@JsonProperty("error") error: String, @JsonProperty("reason") reason: String)
case class Error(@JsonProperty("position") position: Seq[String],
                 @JsonProperty("code") code: String,
                 @JsonProperty("reason") reason: String,
                 @JsonProperty("parameters") parameters: Map[String, Parameter])
