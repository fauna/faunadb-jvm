package com.faunadb.binding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.faunadb.FaunaInstance
import com.faunadb.annotation._
import org.apache.commons.beanutils.PropertyUtils

trait FaunaSerializer {
  def serialize[T](obj: T): FaunaInstance
  def deserialize[T](clazz: Class[T], obj: FaunaInstance): T
}

class BeanSerializer extends FaunaSerializer {
  private val json = new ObjectMapper()

  def serialize[T](obj: T): FaunaInstance = {
    val clazz = obj.getClass
    val descriptors = PropertyUtils.getPropertyDescriptors(clazz)

    val faunaClassRef = "classes/" + Option(clazz.getAnnotation(classOf[FaunaClass])).map { _.className }.getOrElse(clazz.getSimpleName)
    val constraints = json.createObjectNode()
    val data = json.createObjectNode()

    var instanceRef: Option[String] = None

    descriptors foreach { descriptor =>
      val baseField = clazz.getDeclaredField(descriptor.getName)
      val readMethod = descriptor.getReadMethod

      Option(baseField.getDeclaredAnnotation(classOf[FaunaInstanceRef])) match {
        case Some(_) => instanceRef = Some(readMethod.invoke(obj).asInstanceOf[String])
        case None =>
          Option(baseField.getDeclaredAnnotation(classOf[FaunaIgnore])) match {
            case Some(_) =>
            case None =>
              val isConstraint = baseField.getDeclaredAnnotation(classOf[FaunaConstraint]) != null
              val declaredName = if (isConstraint) {
                baseField.getDeclaredAnnotation(classOf[FaunaConstraint]).fieldName()
              } else {
                Option(baseField.getDeclaredAnnotation(classOf[FaunaData])).map(_.fieldName).getOrElse("")
              }

              val name = if (declaredName.isEmpty) {
                descriptor.getName
              } else declaredName

              if (isConstraint)
                constraints.set(name, json.valueToTree[ObjectNode](readMethod.invoke(obj)))
              else
                data.set(name, json.valueToTree[ObjectNode](readMethod.invoke(obj)))
          }
      }
    }
   FaunaInstance(
     ref = instanceRef.getOrElse(""),
     classRef = faunaClassRef,
     ts = 0,
     data = data,
     constraints = constraints,
     references = json.createObjectNode())
  }

  def deserialize[T](clazz: Class[T], obj: FaunaInstance): T = {
    val rv = clazz.newInstance()
    val descriptors = PropertyUtils.getPropertyDescriptors(clazz)

    descriptors foreach { descriptor =>
      val baseField = clazz.getDeclaredField(descriptor.getName)
      val readMethod = descriptor.getReadMethod
      val writeMethod = descriptor.getWriteMethod
      val propType = descriptor.getPropertyType

      Option(baseField.getDeclaredAnnotation(classOf[FaunaInstanceRef])) match {
        case Some(_) => writeMethod.invoke(rv, obj.ref)
        case None =>
          Option(baseField.getDeclaredAnnotation(classOf[FaunaIgnore])) match {
            case Some(_) =>
            case None =>
              val isConstraint = baseField.getDeclaredAnnotation(classOf[FaunaConstraint]) != null
              val declaredName = if (isConstraint) {
                baseField.getDeclaredAnnotation(classOf[FaunaConstraint]).fieldName()
              } else {
                Option(baseField.getDeclaredAnnotation(classOf[FaunaData])).map(_.fieldName).getOrElse("")
              }

              val name = if (declaredName.isEmpty) {
                descriptor.getName
              } else declaredName

              val value = if (isConstraint)
                obj.constraints.get(name)
              else
                obj.data.get(name)

              Option(value) foreach { value =>
                writeMethod.invoke(rv, json.treeToValue(value, propType).asInstanceOf[Object])
              }
          }
      }
    }

    rv
  }
}
