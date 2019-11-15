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
            readRef(in, ctx)
          case "@set" =>
            in.nextToken()
            val rv = SetRefV(deserialize(in, ctx))
            rv
          case "@ts" =>
            in.nextToken()
            val rv = TimeV(in.getText)
            in.nextToken()
            rv
          case "@date" =>
            in.nextToken()
            val rv = DateV(in.getText)
            in.nextToken()
            rv
          case "@bytes" =>
            in.nextToken()
            val rv = BytesV(in.getText)
            in.nextToken()
            rv
          case "@query" =>
            in.nextToken()
            val rv = QueryV(in.readValueAs(classOf[ObjectV]))
            rv
          case "@obj" =>
            readObject(in, ctx)
          case _ =>
            readObjectBody(in, ctx)
        }
      case END_OBJECT => ObjectV.empty
      case t => throw new JsonMappingException(s"Unexpected token $t")
    }
  }

  private[this] def readRef(in: JsonParser, ctx: DeserializationContext): RefV = {
    in.nextToken() match {
      case START_OBJECT =>
        in.nextToken()
        val rv = readRefBody(in, ctx)
        in.nextToken()
        rv
      case t => throw new JsonMappingException(s"Unexpected token $t")
    }
  }

  private[this] def readRefBody(in: JsonParser, ctx: DeserializationContext): RefV = {
    var id: Option[String] = None
    var cls: Option[RefV] = None
    var db: Option[RefV] = None

    while (in.getCurrentToken != END_OBJECT) {
      in.getCurrentToken match {
        case FIELD_NAME =>
          in.getText match {
            case "id" =>
              in.nextToken()
              id = Some(in.getText)
              in.nextToken()
            case "collection" =>
              in.nextToken()
              cls = deserialize(in, ctx) match {
                case r: RefV => Some(r)
                case t => throw new JsonMappingException(s"Unexpected value in class field of @ref: $t")
              }
            case "database" =>
              in.nextToken()
              db = deserialize(in, ctx) match {
                case d: RefV => Some(d)
                case t => throw new JsonMappingException(s"Unexpected value in database field of @ref: $t")
              }
            case t => throw new JsonMappingException(s"Unexpected field in @ref: $t")
          }
        case t => throw new JsonMappingException(s"Unexpected token $t")
      }
    }

    (id, cls, db) match {
      case (Some(id), None, None) => Native.fromName(id)
      case (Some(id), _, _)       => RefV(id, cls, db)
      case (None, _, _)           => throw new JsonMappingException(s"Malformed reference type")
    }
  }

  private[this] def readObject(in: JsonParser, ctx: DeserializationContext): ObjectV = {
    in.nextToken() match {
      case START_OBJECT =>
        in.nextToken()
        val rv = readObjectBody(in, ctx)
        in.nextToken()
        rv
      case t => throw new JsonMappingException(s"Unexpected token $t")
    }
  }

  private[this] def readObjectBody(in: JsonParser, ctx: DeserializationContext): ObjectV = {
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
