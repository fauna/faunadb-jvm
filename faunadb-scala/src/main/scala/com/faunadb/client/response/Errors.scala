package com.faunadb.client.response

import com.fasterxml.jackson.annotation.JsonProperty

case class Error(@JsonProperty("position") position: Seq[String],
                 @JsonProperty("code") code: String,
                 @JsonProperty("reason") reason: String)

