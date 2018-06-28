package faunadb

import faunadb.values.Codec
import java.time.{ Instant, LocalDate }

/**
  * This class only exist to test Codec.caseClass macro compilation without importing `faunadb.values._`
  */
class CaseClassSpec {

  case class CaseClass(boolean: Boolean, instant: Instant, localDate: LocalDate, string: String, char: Char, byte: Byte,
                       short: Short, int: Int, long: Long, float: Float, double: Double,
                       array: Array[Int], seq: Seq[Int], list: List[Int], indexedSeq: IndexedSeq[Int],
                       map: Map[String, Long], option: Option[Int], either: Either[Int, String])

  implicit val caseClassCodec: Codec[CaseClass] = Codec.caseClass[CaseClass]
}
