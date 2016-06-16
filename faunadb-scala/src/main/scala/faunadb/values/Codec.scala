package faunadb.values

import java.time.{ LocalDate, Instant }

trait Decoder[T] {
  def decode(v: Value, path: FieldPath): Result[T]
}

object Decoder {
  implicit object ValueDecoder extends Decoder[Value] {
    def decode(v: Value, path: FieldPath) = Result.successful(v, path)
  }

  implicit object ScalarDecoder extends Decoder[ScalarValue] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case s: ScalarValue => Result.successful(s, path)
        case v => Result.Unexpected(v, "Scalar", path)
      }
  }

  implicit object StringDecoder extends Decoder[String] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case StringV(s) => Result.successful(s, path)
        case v => Result.Unexpected(v, "String", path)
      }
  }

  implicit object BooleanDecoder extends Decoder[Boolean] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case BooleanV(b) => Result.successful(b, path)
        case v => Result.Unexpected(v, "Boolean", path)
      }
  }

  implicit object IntDecoder extends Decoder[Int] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case LongV(l) => Result.successful(l.toInt, path)
        case v => Result.Unexpected(v, "Long", path)
      }
  }

  implicit object LongDecoder extends Decoder[Long] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case LongV(l) => Result.successful(l, path)
        case v => Result.Unexpected(v, "Long", path)
      }
  }

  implicit object DoubleDecoder extends Decoder[Double] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case DoubleV(d) => Result.successful(d, path)
        case v => Result.Unexpected(v, "Double", path)
      }
  }

  implicit object RefDecoder extends Decoder[RefV] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case r: RefV => Result.successful(r, path)
        case v => Result.Unexpected(v, "Ref", path)
      }
  }

  implicit object SetRefDecoder extends Decoder[SetRefV] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case r: SetRefV => Result.successful(r, path)
        case v => Result.Unexpected(v, "Set Ref", path)
      }
  }

  implicit object TimestampDecoder extends Decoder[TimeV] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case ts: TimeV => Result.successful(ts, path)
        case v => Result.Unexpected(v, "Timestamp", path)
      }
  }

  implicit object InstantDecoder extends Decoder[Instant] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case ts: TimeV => Result.successful(ts.instant, path)
        case v => Result.Unexpected(v, "Timestamp", path)
      }
  }

  implicit object DateDecoder extends Decoder[DateV] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case ts: DateV => Result.successful(ts, path)
        case v => Result.Unexpected(v, "Date", path)
      }
  }

  implicit object LocalDateDecoder extends Decoder[LocalDate] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case ts: DateV => Result.successful(ts.localDate, path)
        case v => Result.Unexpected(v, "Date", path)
      }
  }

  implicit object NullVDecoder extends Decoder[NullV] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case NullV => Result.successful(NullV, path)
        case v => Result.Unexpected(v, "Null", path)
      }
  }

  implicit object ArrayVDecoder extends Decoder[ArrayV] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case a: ArrayV => Result.successful(a, path)
        case v => Result.Unexpected(v, "Array", path)
      }
  }

  implicit object ObjectVDecoder extends Decoder[ObjectV] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case a: ObjectV => Result.successful(a, path)
        case v => Result.Unexpected(v, "Object", path)
      }
  }

  class SeqDecoder[T: Decoder] extends Decoder[Seq[T]] {
    def decode(v: Value, path: FieldPath) =
      Field.collect(Field.to[T]).get(v)
  }
  implicit def SeqDecoder[T: Decoder]: Decoder[Seq[T]] = new SeqDecoder[T]
}

trait Encoder[T] {
  def encode(t: T): Value
}

trait Codec[T] extends Decoder[T] with Encoder[T]
