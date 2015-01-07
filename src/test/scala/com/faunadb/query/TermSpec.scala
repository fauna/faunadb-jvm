package com.faunadb.query

import org.scalatest.{Matchers, FlatSpec}
import com.faunadb.query.Terms._

class TermSpec extends FlatSpec with Matchers {
  "Match Terms" should "stringify properly" in {
    val term = new Match("classes/test", "constraints.something", "word1", "word2", "word3")
    term.toQueryString shouldBe "match(classes/test,constraints.something,\"word1\",\"word2\",\"word3\")"
  }

  "Nested Set Query Terms" should "stringify properly" in {
    val term = new Intersection(new Difference("classes/set1", "classes/set2", "classes/set3"), new Union("classes/set4", new Merge("classes/set5", "classes/set6")), new Events("classes/set7"))
    term.toQueryString shouldBe "intersection(difference(classes/set1,classes/set2,classes/set3),union(classes/set4,merge(classes/set5,classes/set6)),events(classes/set7))"
  }
}