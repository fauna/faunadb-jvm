package com.faunadb.query

import scala.annotation.varargs

object QueryBuilder {
  @varargs def `match`(className: String, jsonPath: String, terms: String*) = {
    Terms.Match(className, jsonPath, terms: _*)
  }
}

case class QueryBuilder(tree: Terms.Term) {
  def build() = {
    tree.toQueryString
  }
}
