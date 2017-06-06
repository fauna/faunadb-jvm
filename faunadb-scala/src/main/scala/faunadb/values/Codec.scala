package faunadb.values

import org.joda.time.{ Instant, LocalDate }
import scala.collection.generic.CanBuildFrom
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.ClassTag

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

  implicit object CharDecoder extends Decoder[Char] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case LongV(l) => Result.successful(l.toChar, path)
        case v => Result.Unexpected(v, "Char", path)
      }
  }

  implicit object ByteDecoder extends Decoder[Byte] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case LongV(l) => Result.successful(l.toByte, path)
        case v => Result.Unexpected(v, "Byte", path)
      }
  }

  implicit object ShortDecoder extends Decoder[Short] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case LongV(l) => Result.successful(l.toShort, path)
        case v => Result.Unexpected(v, "Short", path)
      }
  }

  implicit object IntDecoder extends Decoder[Int] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case LongV(l) => Result.successful(l.toInt, path)
        case v => Result.Unexpected(v, "Int", path)
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

  implicit object FloatDecoder extends Decoder[Float] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case DoubleV(d) => Result.successful(d.toFloat, path)
        case v => Result.Unexpected(v, "Float", path)
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
        case TimeV(time) => Result.successful(time.toInstant, path)
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

  trait ArrayDecoder[T] extends Decoder[Array[T]]

  implicit def ArrayDecoder[T: Decoder](implicit ev: ClassTag[T]) = new ArrayDecoder[T] {
    override def decode(v: Value, path: FieldPath): Result[Array[T]] =
      Field.collect(Field.to[T]).get(v).map(x => x.toArray[T])
  }

  implicit object ByteArrayDecoder extends ArrayDecoder[Byte] {
    override def decode(v: Value, path: FieldPath): Result[Array[Byte]] =
      v match {
        case BytesV(b) => Result.successful(b, path)
        case v => Result.Unexpected(v, "Bytes", path)
      }
  }

  implicit def CollectionDecoder[T: Decoder, Col[_]](implicit cbf: CanBuildFrom[_, T, Col[T]]): Decoder[Col[T]] = new Decoder[Col[T]] {
    def decode(v: Value, path: FieldPath) =
      Field.collect[T, Col](Field.to[T]).get(v)
  }

  implicit object BytesVDecoder extends Decoder[BytesV] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case b: BytesV => Result.successful(b, path)
        case v => Result.Unexpected(v, "Bytes", path)
      }
  }

  implicit def OptionDecoder[T](implicit decoder: Decoder[T]): Decoder[Option[T]] = new Decoder[Option[T]] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case NullV => Result.successful(None, path)
        case _ => decoder.decode(v, path).map (Some(_))
      }
  }

  implicit def EitherDecoder[A, B](implicit a: Decoder[A], b: Decoder[B], tagA: ClassTag[A], tagB: ClassTag[B]): Decoder[Either[A, B]] = new Decoder[Either[A, B]] {
    def decode(v: Value, path: FieldPath) =
      (a.decode(v, path), b.decode(v, path)) match {
        case (VSuccess(s, _), _: VFail) => Result.successful(Left[A, B](s), path)
        case (_: VFail, VSuccess(s, _)) => Result.successful(Right[A, B](s), path)
        case _ => Result.Unexpected(v, s"Either ${tagA.runtimeClass.getSimpleName} or ${tagB.runtimeClass.getSimpleName}", path)
      }
  }
}

trait Encoder[T] {
  def encode(t: T): Value
}

object Encoder {
  implicit def ValueEncoder[T <: Value]: Encoder[T] = new Encoder[T] {
    def encode(t: T): Value = if (t != null) t else NullV
  }

  implicit def ResultEncoder[T <: Value] = new Encoder[Result[T]] {
    def encode(t: Result[T]): Value = t.get
  }

  implicit object BooleanEncoder extends Encoder[Boolean] {
    def encode(t: Boolean) = BooleanV(t)
  }

  implicit object LocalDateEncoder extends Encoder[LocalDate] {
    def encode(t: LocalDate) = if (t != null) DateV(t) else NullV
  }

  implicit object StringEncoder extends Encoder[String] {
    def encode(t: String) = if (t != null) StringV(t) else NullV
  }

  implicit object CharEncoder extends Encoder[Char] {
    def encode(t: Char) = LongV(t)
  }

  implicit object ByteEncoder extends Encoder[Byte] {
    def encode(t: Byte) = LongV(t)
  }

  implicit object ShortEncoder extends Encoder[Short] {
    def encode(t: Short) = LongV(t)
  }

  implicit object IntEncoder extends Encoder[Int] {
    def encode(t: Int) = LongV(t)
  }

  implicit object LongEncoder extends Encoder[Long] {
    def encode(t: Long) = LongV(t)
  }

  implicit object FloatEncoder extends Encoder[Float] {
    def encode(t: Float) = DoubleV(t)
  }

  implicit object DoubleEncoder extends Encoder[Double] {
    def encode(t: Double) = DoubleV(t)
  }

  trait ArrayEncoder[T] extends Encoder[Array[T]]

  implicit def ArrayEncoder[T: Encoder](implicit ev: ClassTag[T]) = new ArrayEncoder[T] {
    def encode(t: Array[T]): Value = if (t != null) ArrayV(t map { Value(_) } toVector) else NullV
  }

  implicit object ByteArrayEncoder extends ArrayEncoder[Byte] {
    def encode(t: Array[Byte]): Value = if (t != null) BytesV(t) else NullV
  }

  implicit def CollectionEncoder[T: Decoder, Col[E] <: Traversable[E]](implicit ev: Encoder[T]): Encoder[Col[T]] = new Encoder[Col[T]] {
    def encode(col: Col[T]) = if (col != null) ArrayV(col map { Value(_) } toVector) else NullV
  }

  class OptionEncoder[Opt <: Option[T], T: Encoder] extends Encoder[Opt] {
    def encode(t: Opt): Value = t map (Value(_)) getOrElse NullV
  }

  implicit def OptionEncoder[T: Encoder] = new OptionEncoder[Option[T], T]
  implicit def SomeEncoder[T: Encoder] = new OptionEncoder[Some[T], T]
  implicit def NoneEncoder = new OptionEncoder[None.type, Nothing]

  class EitherEncoder[A: Encoder, B: Encoder, Etr <: Either[A, B]] extends Encoder[Etr] {
    def encode(t: Etr): Value = t.fold(Value(_), Value(_))
  }

  implicit def EitherEncoder[A: Encoder, B: Encoder] = new EitherEncoder[A, B, Either[A, B]]
  implicit def LeftEncoder[A: Encoder] = new EitherEncoder[A, Nothing, Left[A, Nothing]]
  implicit def RightEncoder[B: Encoder] = new EitherEncoder[Nothing, B, Right[Nothing, B]]
}

trait Codec[T] extends Decoder[T] with Encoder[T]

object Codec {
  def caseClass[T]: Codec[T] = macro CodecMacro.caseClassImpl[T]
}
