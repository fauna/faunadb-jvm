package com.faunadb.oldquery

import com.faunadb.oldquery.Terms._
import org.scalatest.{FlatSpec, Matchers}

class TermSpec extends FlatSpec with Matchers {
  "Match Terms" should "stringify properly" in {
    val term = new Match("indexes/test", "word1")
    term.toQueryString shouldBe "match(indexes/test, \"word1\")"
  }

  "Nested Set Query Terms" should "stringify properly" in {
    val term = new Intersection(new Difference("classes/set1", "classes/set2", "classes/set3"), new Union("classes/set4", new Merge("classes/set5", "classes/set6")), new Events("classes/set7"))
    term.toQueryString shouldBe "intersection(difference(classes/set1,classes/set2,classes/set3),union(classes/set4,merge(classes/set5,classes/set6)),events(classes/set7))"
  }
}