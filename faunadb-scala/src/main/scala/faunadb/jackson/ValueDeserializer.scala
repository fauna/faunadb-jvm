package faunadb.jackson

import com.fasterxml.jackson.core.JsonToken._
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import faunadb.values._

private[faunadb] class ValueDeserializer extends JsonDeserializer[Value] {
  override def deserialize(in: JsonParser, ctx: DeserializationContext): Value = {
    val rv = in.getCurrentToken match {
      case START_OBJECT       => readSpecial(in, ctx)
      case START_ARRAY        => readArray(in, ctx)
      case VALUE_STRING       => StringV(in.getText)
      case VALUE_NUMBER_FLOAT => DoubleV(in.getValueAsDouble(0))
      case VALUE_NUMBER_INT   => LongV(in.getValueAsLong)
      case VALUE_TRUE         => BooleanV(true)
      case VALUE_FALSE        => BooleanV(false)
      case VALUE_NULL         => NullV
      case t                  => throw new JsonMappingException(s"Unexpected token $t")
    }

    in.nextToken()
    rv
  }

  private[this] def readSpecial(in: JsonParser, ctx: DeserializationContext): Value = {
    in.nextToken()

    in.getCurrentToken match {
      case FIELD_NAME =>
        in.getText match {
          case "@ref" =>
            in.nextToken()
            val rv = Ref(in.getText)
            in.nextToken()
            rv
          case "@set" =>
            in.nextToken()
            val rv = SetRef(deserialize(in, ctx))
            in.nextToken()
            rv
          case "@ts" =>
            in.nextToken()
            val rv = Timestamp(in.getText)
            in.nextToken()
            rv
          case "@date" =>
            in.nextToken()
            val rv = Date(in.getText)
            in.nextToken()
            rv
          case "@obj" =>
            in.nextToken() match {
              case START_OBJECT =>
                in.nextToken()
                val rv = readObject(in, ctx)
                in.nextToken()
                rv
              case t => throw new JsonMappingException(s"Unexpected token $t")
            }
          case _ =>
            readObject(in, ctx)
        }
      case t => throw new JsonMappingException(s"Unexpected token $t")
    }
  }

  private[this] def readObject(in: JsonParser, ctx: DeserializationContext): Value = {
    val b = Map.newBuilder[String, Value]

    while (in.getCurrentToken != END_OBJECT) {
      in.getCurrentToken match {
        case FIELD_NAME =>
          val name = in.getText
          in.nextToken()
          b += (name -> deserialize(in, ctx))
        case t => throw new JsonMappingException(s"Unexpected token $t")
      }
    }

    new ObjectV(b.result)
  }

  private[this] def readArray(in: JsonParser, ctx: DeserializationContext): Value = {
    in.nextToken()

    val b = Vector.newBuilder[Value]

    while (in.getCurrentToken != END_ARRAY) { b += deserialize(in, ctx) }

    new ArrayV(b.result)
  }
}
