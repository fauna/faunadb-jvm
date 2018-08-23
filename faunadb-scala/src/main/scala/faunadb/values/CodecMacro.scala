package faunadb.values

import scala.reflect.macros.blackbox

class CodecMacro(val c: blackbox.Context) {
  import c.universe._

  private val M = q"_root_.faunadb.values"

  def caseClassImpl[T: WeakTypeTag]: Tree = {
    val tpe = weakTypeOf[T]

    q"""new $M.Codec[$tpe] {
      def decode(value: $M.Value, path: $M.FieldPath): $M.Result[$tpe] =
        ${getDecodedObject(tpe)}

      def encode(value: $tpe): $M.Value =
        ${getEncodedObject(tpe)}
    }: $M.Codec[$tpe]"""
  }

  private def getEncodedObject(tpe: Type): Tree = {
    val fields = getFields(tpe) map { field =>
      val variable = q"value.${varName(field).toTermName}"

      if (isOption(field._2)) {
        q"(${varName(field).toString}, ($variable.map(v => v: $M.Value).getOrElse($M.NullV)))"
      } else {
        q"(${varName(field).toString}, ($variable: $M.Value))"
      }
    }

    q"$M.ObjectV(..$fields)"
  }

  private def getDecodedObject(tpe: Type): Tree = {
    val fields = getFields(tpe)

    val fieldsFragments = fields map { field =>
      val variable = q"value(${varName(field).toString})"
      val decoder = q"implicitly[$M.Decoder[${field._2}]]"
      val path = q"path ++ ${varName(field).toString}"

      if (isOption(field._2)) {
        fq"${varName(field)} <- $decoder.decode($variable.getOrElse($M.NullV), $path)"
      } else {
        fq"${varName(field)} <- $decoder.decode($variable, $path)"
      }
    }

    q"for (..$fieldsFragments) yield new $tpe(..${fields.map(varName)})"
  }

  private def isOption(tpe: Type) =
    tpe.typeConstructor =:= weakTypeOf[Option[_]].typeConstructor

  private def varName(field: (Symbol, Type)): Name =
    field._1.name.decodedName

  private def getFields(tpe: Type): List[(Symbol, Type)] = {
    if (tpe.typeSymbol.isClass && !tpe.typeSymbol.asClass.isCaseClass) {
      c.abort(c.enclosingPosition, s"type `$tpe` is not a case class")
    }

    val params = tpe.decl(termNames.CONSTRUCTOR).asMethod.paramLists.flatten
    val genericType = tpe.typeConstructor.typeParams.map { _.asType.toType }

    params.map { field =>
      val index = genericType.indexOf(field.typeSignature)
      val fieldType = if (index >= 0) tpe.typeArgs(index) else field.typeSignature

      (field, fieldType)
    }
  }
}
