package faunadb.values

import scala.reflect.macros.blackbox

class CodecMacro(val c: blackbox.Context) {
  import c.universe._

  def caseClassImpl[T: c.WeakTypeTag]: c.Tree = {
    val tag = weakTypeOf[T]

    q""" new Codec[$tag] {
      override def decode(value: Value, path: FieldPath): Result[$tag] =
        ${getDecodedObject(tag)}

      override def encode(value: $tag): Value =
        ${getEncodedObject(tag)}
    } """
  }

  def getEncodedObject(tag: c.Type): c.Tree = {
    val fields = getFields(tag).map { field =>
      val variable = q"value.${varName(field).toTermName}"

      if (isOption(field._2))
        q""" ${varName(field).toString} -> ($variable.map(v => v: Value).getOrElse(NullV)) """
      else
        q""" ${varName(field).toString} -> ($variable: Value) """
    }

    q"ObjectV.apply(..$fields)"
  }

  def getDecodedObject(tag: c.Type): c.Tree = {
    val fields = getFields(tag)

    val fieldsFragments = fields.map { field =>
      val variable = q"value(${varName(field).toString})"

      if (isOption(field._2))
        fq"${varName(field)} <- VSuccess($variable.to[${field._2.typeArgs.head}].toOpt, ${varName(field).toString})"
      else
        fq"${varName(field)} <- $variable.to[${field._2}]"
    }

    q"for (..$fieldsFragments) yield new $tag(..${fields.map(varName)})"
  }

  def isOption(tpe: c.Type) =
    tpe.typeConstructor =:= weakTypeOf[Option[_]].typeConstructor

  def varName(field: (c.Symbol, c.Type)): c.Name =
    field._1.name.decodedName

  def getFields(tag: c.Type): List[(c.Symbol, c.Type)] = {
    if (tag.typeSymbol.isClass && !tag.typeSymbol.asClass.isCaseClass) {
      throw new IllegalArgumentException(s"type $tag is not a case class")
    }

    val params = tag.decl(termNames.CONSTRUCTOR).asMethod.paramLists.flatten
    val genericType = tag.typeConstructor.typeParams.map { _.asType.toType }

    params.map { field =>
      val index = genericType.indexOf(field.typeSignature)
      val fieldType = if (index >= 0) tag.typeArgs(index) else field.typeSignature

      (field, fieldType)
    }
  }
}

