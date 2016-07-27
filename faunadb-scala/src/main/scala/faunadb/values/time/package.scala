package faunadb.values

import org.joda.time.Instant
import org.joda.time.format.ISODateTimeFormat
import scala.Ordered._

package time {

  /**
    * Represents an instance of [[org.joda.time.Instant]] with nanosecond precision.
    * It calculates the overflow from nano to milliseconds and adds to the initial instant.
    */
  class HighPrecisionTime private(initialInstant: Instant, nanosToAdd: Int) {

    /** [[org.joda.time.Instant]] representation. Truncates micro and nanoseconds. */
    val instant = initialInstant.plus(nanosToAdd / 1000000)

    /** Nanoseconds added to the initial time */
    val nanos = nanosToAdd % 1000000

    /** Microseconds added to the initial time. Truncates nanoseconds. */
    val micros = nanos / 1000

    /** Milliseconds since Java epoch. Truncates micro and nanoseconds. */
    val millis = instant.getMillis

    override def equals(obj: Any): Boolean = obj match {
      case other: HighPrecisionTime => instant == other.instant && nanos == other.nanos
      case _                        => false
    }

    override def hashCode(): Int =
      instant.hashCode() + nanos.hashCode()

    override def toString =
      "%s%06dZ".format(
        instant.toString(ISODateTimeFormat.dateHourMinuteSecondMillis()),
        nanos
      )
  }

  object HighPrecisionTime {

    /**
      * Creates an instance of [[faunadb.values.time.HighPrecisionTime]] adding
      * the the micro and nanoseconds informed.
      */
    def apply(initial: Instant, microsToAdd: Int = 0, nanosToAdd: Int = 0): HighPrecisionTime =
      new HighPrecisionTime(initial, microsToAdd * 1000 + nanosToAdd)

    private val Precision = ".*\\.\\d{3}(\\d{3})(\\d{3})?Z".r

    /**
      * Parses an [[String]] into an instance of [[faunadb.values.time.HighPrecisionTime]].
      * String must be in "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZ" date format.
      */
    def parse(value: String): HighPrecisionTime = {
      value match {
        case Precision(micros, null)  => HighPrecisionTime(toInstant(value), microsToAdd = micros.toInt)
        case Precision(micros, nanos) => HighPrecisionTime(toInstant(value), micros.toInt, nanos.toInt)
        case _                        => HighPrecisionTime(toInstant(value))
      }
    }

    private def toInstant(value: String): Instant =
      ISODateTimeFormat.dateTimeParser().parseDateTime(value).toInstant
  }

}

package object time {

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
      (x.instant, x.nanos) compare (y.instant, y.nanos)
  }

}
