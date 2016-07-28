package faunadb.values
package time

import org.joda.time.Instant
import org.joda.time.format.ISODateTimeFormat
import scala.Ordered._

/**
  * Represents the instant from Java's epoch composed by the number of seconds since eposh
  * and the nanoseconds offset.
  */
class HighPrecisionTime private(val secondsSinceEposh: Long, val nanoSecondsOffset: Int) {

  /** Returns a [[org.joda.time.Instant]]. Truncates micro and nanoseconds. */
  def toInstant: Instant =
    new Instant(milliSecondsSinceEpoch)

  /** Milliseconds since Java epoch. Truncates micro and nanoseconds. */
  def milliSecondsSinceEpoch: Long =
    secondsSinceEposh / 1000

  override def equals(obj: Any): Boolean = obj match {
    case other: HighPrecisionTime =>
      secondsSinceEposh == other.secondsSinceEposh &&
        nanoSecondsOffset == other.nanoSecondsOffset

    case _ => false
  }

  override def hashCode(): Int =
    secondsSinceEposh.hashCode() + nanoSecondsOffset.hashCode()

  override def toString =
    "%s%06dZ".format(
      toInstant.toString(ISODateTimeFormat.dateHourMinuteSecondMillis()),
      nanoSecondsOffset
    )
}

object HighPrecisionTime {

  private val NanosInAMicro = 10000
  private val NanosInASecond = 1000000000

  /**
    * Creates an instance of [[faunadb.values.time.HighPrecisionTime]] adding
    * the the micro and nanoseconds informed.
    */
  def apply(initial: Instant, microsToAdd: Long = 0, nanosToAdd: Long = 0): HighPrecisionTime = {
    val nanos = microsToAdd * NanosInAMicro + nanosToAdd
    val overflow = nanos / NanosInASecond
    val remaining = nanos % NanosInASecond

    new HighPrecisionTime(initial.getMillis * 1000 + overflow, remaining.toInt)
  }

  private val Precision = ".*\\.\\d{3}(\\d{3})(\\d{3})?Z".r

  /**
    * Parses an [[String]] into an instance of [[faunadb.values.time.HighPrecisionTime]].
    * String must be in "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZ" date format.
    */
  def parse(value: String): HighPrecisionTime = {
    value match {
      case Precision(micros, null)  => HighPrecisionTime(toInstant(value), microsToAdd = micros.toLong)
      case Precision(micros, nanos) => HighPrecisionTime(toInstant(value), micros.toLong, nanos.toLong)
      case _                        => HighPrecisionTime(toInstant(value))
    }
  }

  private def toInstant(value: String): Instant =
    ISODateTimeFormat.dateTimeParser().parseDateTime(value).toInstant

  implicit object HighPrecisionTimeDecoder extends Decoder[HighPrecisionTime] {
    def decode(value: Value, path: FieldPath) =
      value match {
        case TimeV(time) => Result.successful(time, path)
        case v           => Result.Unexpected(v, "HightPrecisionTime", path)
      }
  }

  implicit val jodaTimeOrdering: Ordering[Instant] =
    Ordering.fromLessThan(_ isBefore _)

  implicit object HighPrecisionTimeOrdering extends Ordering[HighPrecisionTime] {
    def compare(x: HighPrecisionTime, y: HighPrecisionTime): Int =
      (x.secondsSinceEposh, x.nanoSecondsOffset) compare (y.secondsSinceEposh, y.nanoSecondsOffset)
  }
}