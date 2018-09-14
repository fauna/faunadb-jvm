package faunadb.values

object Result {
  def successful[T](t: T, path: FieldPath): Result[T] = VSuccess(t, path)

  def collectErrors(rs: Result[_]*) =
    VFail(rs.toList flatMap {
      case VFail(errs) => errs
      case _ => Nil
    })

  def NotFound[T](path: FieldPath): Result[T] =
    VFail(List(FieldError("Value not found", path)))

  def Unexpected[T](actual: Value, expected: String, path: FieldPath): Result[T] = {
    val tpe = actual.getClass.getSimpleName.dropRight(1)
    VFail(List(FieldError(s"Expected $expected; found value $actual of type $tpe.", path)))
  }

  implicit class ValueResultOps(r: Result[Value]) {
    def apply[T](field: Field[T]): Result[T] =
      r match {
        case VSuccess(v, pre) =>
          field.get(v) match {
            case VSuccess(t, path) => VSuccess(t, pre ++ path)
            case VFail(errs) => VFail(errs map (_ prefixed pre))
          }
        case VFail(errs) => VFail(errs)
      }

    def apply(p: FieldPath, ps: FieldPath*): Result[Value] = apply(Field(p, ps: _*))

    def to[T: Decoder]: Result[T] = apply(Field.to[T])

    def collect[T](inner: Field[T]): Result[Seq[T]] = apply(Field.collect(inner))
  }
}

sealed abstract class Result[+T] {

  def get: T
  def isSuccess: Boolean

  def map[U](f: T => U): Result[U]
  def flatMap[U](f: T => Result[U]): Result[U]

  def isFailure = !isSuccess
  def isDefined = isSuccess
  def isEmpty = isFailure

  def getOrElse[T1 >: T](alt: => T1) = try get catch { case ValueReadException(_) => alt }
  def toEither = try Right(get) catch { case ValueReadException(errs) => Left(errs) }
  def toOpt = try Some(get) catch { case ValueReadException(_) => None }
}

final case class VFail private[values] (errors: List[FieldError]) extends Result[Nothing] {
  def get = throw ValueReadException(errors)
  def isSuccess = false

  def map[U](f: Nothing => U): Result[U] = VFail(errors)
  def flatMap[U](f: Nothing => Result[U]): Result[U] = VFail(errors)
}

final case class VSuccess[+T] private[values] (get: T, path: FieldPath) extends Result[T] {
  def isSuccess = true

  def map[U](f: T => U): Result[U] = VSuccess(f(get), path)
  def flatMap[U](f: T => Result[U]): Result[U] = f(get)
}
