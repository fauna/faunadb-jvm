package faunadb

import scala.util.Random

trait RandomGenerator {
  def aRandomString: String = aRandomString(size = 10)
  def aRandomString(size: Int): String = Random.alphanumeric.take(size).mkString
}

object RandomGenerator extends RandomGenerator
