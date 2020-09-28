package faunadb.values

import java.time.{ Instant, LocalDate }
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
        case TimeV(time) => Result.successful(time, path)
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
    def decode(v: Value, path: FieldPath): Result[Array[T]] =
      Field.collect(Field.to[T]).get(v).map(x => x.toArray[T])
  }

  implicit object ByteArrayDecoder extends ArrayDecoder[Byte] {
    def decode(v: Value, path: FieldPath): Result[Array[Byte]] =
      v match {
        case BytesV(b) => Result.successful(b, path)
        case v => Result.Unexpected(v, "Bytes", path)
      }
  }

  implicit def CollectionDecoder[T: Decoder, Col[_]](implicit cbf: CanBuildFrom[_, T, Col[T]]): Decoder[Col[T]] = new Decoder[Col[T]] {
    def decode(v: Value, path: FieldPath) =
      Field.collect[T, Col](Field.to[T]).get(v)
  }

  implicit def MapDecoder[T](implicit decoder: Decoder[T]): Decoder[Map[String, T]] = new Decoder[Map[String, T]] {
    def decode(v: Value, path: FieldPath) =
      v match {
        case ObjectV(fields) => {
          val successBuilder = Map.newBuilder[String, T]
          val failureBuilder = List.newBuilder[FieldError]

          fields.foreach { kv =>
            decoder.decode(kv._2, path ++ kv._1) match {
              case VSuccess(v, _) => successBuilder += kv._1 -> v
              case VFail(errs)    => failureBuilder ++= errs
            }
          }

          val failures = failureBuilder.result()

          if (failures.nonEmpty)
            VFail(failures)
          else
            VSuccess(successBuilder.result(), path)
        }
        case _ => Result.Unexpected(v, "Map", path)
      }
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

  implicit def Tuple2Decoder[A: Decoder, B: Decoder] = new Decoder[(A, B)] {
    def decode(value: Value, path: FieldPath): Result[(A, B)] = {
      for {
        v0 <- value(Field(0).to[A])
        v1 <- value(Field(1).to[B])
      } yield (v0, v1)
    }
  }

  implicit def Tuple3Decoder[A: Decoder, B: Decoder, C: Decoder] = new Decoder[(A, B, C)] {
    def decode(value: Value, path: FieldPath): Result[(A, B, C)] = {
      for {
        v0 <- value(Field(0).to[A])
        v1 <- value(Field(1).to[B])
        v2 <- value(Field(2).to[C])
      } yield (v0, v1, v2)
    }
  }

  implicit def Tuple4Decoder[A: Decoder, B: Decoder, C: Decoder, D: Decoder] = new Decoder[(A, B, C, D)] {
    def decode(value: Value, path: FieldPath): Result[(A, B, C, D)] = {
      for {
        v0 <- value(Field(0).to[A])
        v1 <- value(Field(1).to[B])
        v2 <- value(Field(2).to[C])
        v3 <- value(Field(3).to[D])
      } yield (v0, v1, v2, v3)
    }
  }

  implicit def Tuple5Decoder[A: Decoder, B: Decoder, C: Decoder, D: Decoder, E: Decoder] = new Decoder[(A, B, C, D, E)] {
    def decode(value: Value, path: FieldPath): Result[(A, B, C, D, E)] = {
      for {
        v0 <- value(Field(0).to[A])
        v1 <- value(Field(1).to[B])
        v2 <- value(Field(2).to[C])
        v3 <- value(Field(3).to[D])
        v4 <- value(Field(4).to[E])
      } yield (v0, v1, v2, v3, v4)
    }
  }

  implicit def Tuple6Decoder[A: Decoder, B: Decoder, C: Decoder, D: Decoder, E: Decoder, F: Decoder] = new Decoder[(A, B, C, D, E, F)] {
    def decode(value: Value, path: FieldPath): Result[(A, B, C, D, E, F)] = {
      for {
        v0 <- value(Field(0).to[A])
        v1 <- value(Field(1).to[B])
        v2 <- value(Field(2).to[C])
        v3 <- value(Field(3).to[D])
        v4 <- value(Field(4).to[E])
        v5 <- value(Field(5).to[F])
      } yield (v0, v1, v2, v3, v4, v5)
    }
  }

  implicit def Tuple7Decoder[A: Decoder, B: Decoder, C: Decoder, D: Decoder, E: Decoder, F: Decoder, G: Decoder] = new Decoder[(A, B, C, D, E, F, G)] {
    def decode(value: Value, path: FieldPath): Result[(A, B, C, D, E, F, G)] = {
      for {
        v0 <- value(Field(0).to[A])
        v1 <- value(Field(1).to[B])
        v2 <- value(Field(2).to[C])
        v3 <- value(Field(3).to[D])
        v4 <- value(Field(4).to[E])
        v5 <- value(Field(5).to[F])
        v6 <- value(Field(6).to[G])
      } yield (v0, v1, v2, v3, v4, v5, v6)
    }
  }

  implicit def Tuple8Decoder[A: Decoder, B: Decoder, C: Decoder, D: Decoder, E: Decoder, F: Decoder, G: Decoder, H: Decoder] = new Decoder[(A, B, C, D, E, F, G, H)] {
    def decode(value: Value, path: FieldPath): Result[(A, B, C, D, E, F, G, H)] = {
      for {
        v0 <- value(Field(0).to[A])
        v1 <- value(Field(1).to[B])
        v2 <- value(Field(2).to[C])
        v3 <- value(Field(3).to[D])
        v4 <- value(Field(4).to[E])
        v5 <- value(Field(5).to[F])
        v6 <- value(Field(6).to[G])
        v7 <- value(Field(7).to[H])
      } yield (v0, v1, v2, v3, v4, v5, v6, v7)
    }
  }

  implicit def Tuple9Decoder[A: Decoder, B: Decoder, C: Decoder, D: Decoder, E: Decoder, F: Decoder, G: Decoder, H: Decoder, I: Decoder] = new Decoder[(A, B, C, D, E, F, G, H, I)] {
    def decode(value: Value, path: FieldPath): Result[(A, B, C, D, E, F, G, H, I)] = {
      for {
        v0 <- value(Field(0).to[A])
        v1 <- value(Field(1).to[B])
        v2 <- value(Field(2).to[C])
        v3 <- value(Field(3).to[D])
        v4 <- value(Field(4).to[E])
        v5 <- value(Field(5).to[F])
        v6 <- value(Field(6).to[G])
        v7 <- value(Field(7).to[H])
        v8 <- value(Field(8).to[I])
      } yield (v0, v1, v2, v3, v4, v5, v6, v7, v8)
    }
  }

  implicit def Tuple10Decoder[A: Decoder, B: Decoder, C: Decoder, D: Decoder, E: Decoder, F: Decoder, G: Decoder, H: Decoder, I: Decoder, J: Decoder] = new Decoder[(A, B, C, D, E, F, G, H, I, J)] {
    def decode(value: Value, path: FieldPath): Result[(A, B, C, D, E, F, G, H, I, J)] = {
      for {
        v0 <- value(Field(0).to[A])
        v1 <- value(Field(1).to[B])
        v2 <- value(Field(2).to[C])
        v3 <- value(Field(3).to[D])
        v4 <- value(Field(4).to[E])
        v5 <- value(Field(5).to[F])
        v6 <- value(Field(6).to[G])
        v7 <- value(Field(7).to[H])
        v8 <- value(Field(8).to[I])
        v9 <- value(Field(9).to[J])
      } yield (v0, v1, v2, v3, v4, v5, v6, v7, v8, v9)
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

  implicit object InstantEncoder extends Encoder[Instant] {
    def encode(t: Instant): Value = if (t != null) TimeV(t) else NullV
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
    def encode(t: Array[T]): Value = if (t != null) ArrayV(t.iterator.map(Value(_)).toVector) else NullV
  }

  implicit object ByteArrayEncoder extends ArrayEncoder[Byte] {
    def encode(t: Array[Byte]): Value = if (t != null) BytesV(t) else NullV
  }

  implicit def CollectionEncoder[T: Decoder, Col[E] <: Traversable[E]](implicit ev: Encoder[T]): Encoder[Col[T]] = new Encoder[Col[T]] {
    def encode(col: Col[T]) = if (col != null) ArrayV(col.toIterator.map(Value(_)).toVector) else NullV
  }

  implicit def MapEncoder[T: Encoder](implicit encoder: Encoder[T]) = new Encoder[Map[String, T]] {
    def encode(t: Map[String, T]): Value = ObjectV(t map { case (k, v) => k -> encoder.encode(v) })
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

  // hack around our need for local contravariance for tagged union encoding
  implicit def UnionVariantEncoder[T](implicit enc: UnionCodec[_ >: T]): Encoder[T] =
    enc.asInstanceOf[Encoder[T]]

  implicit def Tuple2Encoder[A: Encoder, B: Encoder] = new Encoder[(A, B)] {
    def encode(value: (A, B)): Value = ArrayV(value._1, value._2)
  }

  implicit def Tuple3Encoder[A: Encoder, B: Encoder, C: Encoder] = new Encoder[(A, B, C)] {
    def encode(value: (A, B, C)): Value = ArrayV(value._1, value._2, value._3)
  }

  implicit def Tuple4Encoder[A: Encoder, B: Encoder, C: Encoder, D: Encoder] = new Encoder[(A, B, C, D)] {
    def encode(value: (A, B, C, D)): Value = ArrayV(value._1, value._2, value._3, value._4)
  }

  implicit def Tuple5Encoder[A: Encoder, B: Encoder, C: Encoder, D: Encoder, E: Encoder] = new Encoder[(A, B, C, D, E)] {
    def encode(value: (A, B, C, D, E)): Value = ArrayV(value._1, value._2, value._3, value._4, value._5)
  }

  implicit def Tuple6Encoder[A: Encoder, B: Encoder, C: Encoder, D: Encoder, E: Encoder, F: Encoder] = new Encoder[(A, B, C, D, E, F)] {
    def encode(value: (A, B, C, D, E, F)): Value = ArrayV(value._1, value._2, value._3, value._4, value._5, value._6)
  }

  implicit def Tuple7Encoder[A: Encoder, B: Encoder, C: Encoder, D: Encoder, E: Encoder, F: Encoder, G: Encoder] = new Encoder[(A, B, C, D, E, F, G)] {
    def encode(value: (A, B, C, D, E, F, G)): Value = ArrayV(value._1, value._2, value._3, value._4, value._5, value._6, value._7)
  }

  implicit def Tuple8Encoder[A: Encoder, B: Encoder, C: Encoder, D: Encoder, E: Encoder, F: Encoder, G: Encoder, H: Encoder] = new Encoder[(A, B, C, D, E, F, G, H)] {
    def encode(value: (A, B, C, D, E, F, G, H)): Value = ArrayV(value._1, value._2, value._3, value._4, value._5, value._6, value._7, value._8)
  }

  implicit def Tuple9Encoder[A: Encoder, B: Encoder, C: Encoder, D: Encoder, E: Encoder, F: Encoder, G: Encoder, H: Encoder, I: Encoder] = new Encoder[(A, B, C, D, E, F, G, H, I)] {
    def encode(value: (A, B, C, D, E, F, G, H, I)): Value = ArrayV(value._1, value._2, value._3, value._4, value._5, value._6, value._7, value._8, value._9)
  }

  implicit def Tuple10Encoder[A: Encoder, B: Encoder, C: Encoder, D: Encoder, E: Encoder, F: Encoder, G: Encoder, H: Encoder, I: Encoder, J: Encoder] = new Encoder[(A, B, C, D, E, F, G, H, I, J)] {
    def encode(value: (A, B, C, D, E, F, G, H, I, J)): Value = ArrayV(value._1, value._2, value._3, value._4, value._5, value._6, value._7, value._8, value._9, value._10)
  }
}

trait Codec[T] extends Decoder[T] with Encoder[T]

// base class for macro-generated record style case-class codecs
trait RecordCodec[T] extends Codec[T]

// base class for macro-generated union codecs
trait UnionCodec[T] extends Codec[T]

object Codec {
  def Alias[T, S](to: T => S, from: S => T)(implicit enc: Encoder[S], dec: Decoder[S]): Codec[T] = new Codec[T] {
    def encode(t: T) = enc.encode(to(t))
    def decode(v: Value, path: FieldPath) = dec.decode(v, path) map from
  }

  @deprecated("Use Codec.Record[T] instead", "2.6.0")
  def caseClass[T]: RecordCodec[T] = macro CodecMacro.recordImpl[T]

  def Record[T]: RecordCodec[T] = macro CodecMacro.recordImpl[T]

  def Record[T](t: T): RecordCodec[T] = new RecordCodec[T] {
    def encode(t: T) = ObjectV.empty
    def decode(v: Value, path: FieldPath) =
      v match {
        case ObjectV(_) => VSuccess(t, path)
        case _          => Result.Unexpected(v, "Map", path)
      }
  }

  def Union[T](tagField: String)(variants: (Any, Codec[_ <: T])*): UnionCodec[T] = macro CodecMacro.unionImpl[T]
}
