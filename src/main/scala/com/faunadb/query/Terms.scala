package com.faunadb.query

object Terms {
  implicit def stringRefToTerm(str: String) = new ClassRef(str)

  sealed abstract class Term {
    def toQueryString: String
  }

  case class ClassRef(classRef: String) extends Term {
    def toQueryString = classRef
  }

  case class Events(term: Term) extends Term {
    def toQueryString = "events(" + term.toQueryString +")"
  }

  case class Match(indexName: String, queryTerm: String) extends Term {
    def toQueryString = "match(" + indexName + ", \"" + queryTerm +"\")"
  }

  abstract class SetTerm(opName: String, first: Term, rest: Seq[Term]) extends Term {
    def toQueryString = opName + "(" + first.toQueryString + "," + rest.map { _.toQueryString }.mkString(",") + ")"

  }

  case class Union(first: Term, rest: Term*) extends SetTerm("union", first, rest)
  case class Intersection(first: Term, rest: Term*) extends SetTerm("intersection", first, rest)
  case class Difference(first: Term, rest: Term*) extends SetTerm("difference", first, rest)
  case class Merge(first: Term, rest: Term*) extends SetTerm("merge", first, rest)
}
