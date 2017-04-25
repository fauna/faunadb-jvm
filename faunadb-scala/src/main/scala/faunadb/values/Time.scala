package faunadb.values
package time

import org.joda.time.Instant
import org.joda.time.format.ISODateTimeFormat
import scala.Ordered._

/**
  * Represents the instant from Java's epoch composed by the number of seconds since eposh
  * and the nanoseconds offset.
  */
case class HighPrecisionTime private(secondsSinceEpoch: Long, nanoSecondsOffset: Int) {
  import HighPrecisionTime._

  /** Returns a [[org.joda.time.Instant]]. Truncates micro and nanoseconds. */
  def toInstant: Instant =
    new Instant(millisecondsSinceEpoch)

  /** Milliseconds since Java epoch. Truncates micro and nanoseconds. */
  def millisecondsSinceEpoch: Long =
    secondsSinceEpoch * MillisInASecond + nanoSecondsOffset / NanosInAMilli

  override def toString =
    "%s.%09dZ".format(
      toInstant.toString(ISODateTimeFormat.dateHourMinuteSecond()),
      nanoSecondsOffset
    )
}

object HighPrecisionTime {

  private val MillisInASecond = 1000
  private val NanosInAMicro = 1000
  private val NanosInAMilli = 1000000
  private val NanosInASecond = 1000000000

  /**
    * Creates an instance of [[faunadb.values.time.HighPrecisionTime]].
    * Calculate overflow of nanoseconds to seconds.
    */
  def apply(secondsSinceEpoch: Long, nanoSecondsOffset: Long): HighPrecisionTime =
    new HighPrecisionTime(
      secondsSinceEpoch + nanoSecondsOffset / NanosInASecond,
      (nanoSecondsOffset % NanosInASecond).toInt
    )

  /**
    * Creates an instance of [[faunadb.values.time.HighPrecisionTime]] adding
    * the the micro and nanoseconds informed.
    */
  def apply(initial: Instant, microsToAdd: Long = 0, nanosToAdd: Long = 0): HighPrecisionTime = {
    val nanos =
      initial.getMillis % MillisInASecond * NanosInAMilli +
      microsToAdd * NanosInAMicro +
      nanosToAdd

    HighPrecisionTime(initial.getMillis / MillisInASecond, nanos)
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
    ISODateTimeFormat.dateTimeParser.parseDateTime(value).toInstant

  implicit object HighPrecisionTimeDecoder extends Decoder[HighPrecisionTime] {
    def decode(value: Value, path: FieldPath) =
      value match {
        case TimeV(time) => Result.successful(time, path)
        case v           => Result.Unexpected(v, "HighPrecisionTime", path)
      }
  }

  implicit object HighPrecisionEncoder extends Encoder[HighPrecisionTime] {
    def encode(t: HighPrecisionTime) = if (t != null) TimeV(t) else NullV
  }

  implicit object HighPrecisionTimeOrdering extends Ordering[HighPrecisionTime] {
    def compare(x: HighPrecisionTime, y: HighPrecisionTime): Int =
      x.secondsSinceEpoch compare y.secondsSinceEpoch match {
        case 0   => x.nanoSecondsOffset compare y.nanoSecondsOffset
        case cmp => cmp
      }
  }
}