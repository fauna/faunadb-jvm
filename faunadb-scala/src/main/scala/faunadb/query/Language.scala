package faunadb.query

import java.lang.{Iterable => JIterable}

import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.NullNode

import scala.annotation.meta.{field, getter, param}

sealed trait Expression

/**
 * Implicit conversions to FaunaDB value types.
 *
 * These can be used by adding:
 * {{{
 *   import com.faunadb.client.query.Language._
 * }}}
 *
 */
object Language {
  implicit def stringToObjectPath(str: String) = ObjectPath(str)
  implicit def intToArrayPath(i: Int) = ArrayPath(i)
  implicit def stringToValue(unwrapped: String) = StringV(unwrapped)
  implicit def longToValue(unwrapped: Long) = NumberV(unwrapped)
  implicit def boolToValue(unwrapped: Boolean) = BooleanV(unwrapped)
  implicit def arrayToValue(unwrapped: Array[Value]) = ArrayV(unwrapped)
  implicit def mapToValue(unwrapped: collection.Map[String, Value]) = ObjectV(unwrapped)
  implicit def doubleToValue(unwrapped: Double) = DoubleV(unwrapped)
  implicit def pairToValuePair[T](p: (String, T))(implicit convert: T => Value) = {
    (p._1, convert(p._2))
  }
}

sealed trait Path
case class ObjectPath(@(JsonValue @getter) field: String) extends Path
case class ArrayPath(@(JsonValue @getter) index: Int) extends Path

/**
 * A Let expression.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-basic_forms FaunaDB Basic Forms]]
 */
case class Let(@(JsonProperty @field)("let") vars: collection.Map[String, Expression], in: Expression) extends Expression

/**
 * A Do expression.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-basic_forms FaunaDB Basic Forms]]
 */
case class Do(@(JsonProperty @field @getter)("do") expressions: Iterable[Expression]) extends Expression

/**
 * An If function.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-basic_forms FaunaDB Basic Forms]]
 */
case class If(@(JsonProperty @field)("if") condition: Expression, then: Expression, `else`: Expression) extends Expression

/**
 * A Quote function.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-basic_forms FaunaDB Basic Forms]]
 */
case class Quote(@(JsonProperty @field)("quote") quote: Expression) extends Value

/**
 * A Select function.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-basic_forms FaunaDB Basic Forms]]
 */
case class Select(@(JsonProperty @field)("select") path: Iterable[Path], from: Value) extends Expression

/**
 * A Lambda expression.
 *
 * '''Reference''': TBD
 */
case class Lambda(@(JsonProperty @field)("lambda") argument: String, expr: Expression)

/**
 * A Map function.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-collection_functions FaunaDB Collection Functions]]
 */
case class Map(@(JsonProperty @field)("map") lambda: Lambda, collection: Expression) extends Expression

/**
 * A Foreach function.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-collection_functions FaunaDB Collection Functions]]
 */
case class Foreach(@(JsonProperty @field)("foreach") lambda: Lambda, collection: Expression) extends Expression

sealed trait Set extends Expression

/**
 * A Match set.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-collection_functions FaunaDB Sets]]
 */
case class Match(@(JsonProperty @field)("match") term: Value, @(JsonProperty @field) index: Ref) extends Set {
  def this(term: String, index: Ref) = this(StringV(term), index)
}

/**
 * A Union set.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-collection_functions FaunaDB Sets]]
 */
case class Union(@(JsonProperty @field @getter)("union") sets: Iterable[Set]) extends Set

/**
 * An Intersection set.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-collection_functions FaunaDB Sets]]
 */
case class Intersection(@(JsonProperty @field @getter)("intersection") sets: Iterable[Set]) extends Set

/**
 * A Difference set.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-collection_functions FaunaDB Sets]]
 */
case class Difference(@(JsonProperty @field @getter)("difference") sets: Iterable[Set]) extends Set

/**
 * A Join set.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-collection_functions FaunaDB Sets]]
 */
case class Join(@(JsonProperty @field)("join") source: Set, @(JsonProperty @field)("with") target: Lambda) extends Set

/**
 * A Get function.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-reading-resources FaunaDB Resource Retrieval Functions]]
 */
case class Get(@(JsonProperty @field)("get") resource: Expression) extends Expression

/**
 * A Paginate function.
 *
 * The paginate function takes optional parameters. These can either be specified by named parameters on the constructor:
 *
 * {{{
 *   Paginate(resource, ts, sources=true, cursor=Some(cursor))
 * }}}
 *
 * or through the `with` methods:
 * {{{
 *   val paginate = Paginate(resource, ts).withCursor(cursor).withSize(size)
 * }}}
 */
@JsonSerialize(using = classOf[PaginateSerializer])
case class Paginate(resource: Expression,
                    ts: Option[Long] = None,
                    cursor: Option[Cursor] = None,
                    size: Option[Long] = None,
                    sources: Boolean = false,
                    events: Boolean = false) extends Expression {
  /**
   * Returns a copy of this with the optional timestamp parameter set.
   */
  def withCursor(cursor: Cursor) = copy(cursor = Some(cursor))

  /**
   * Returns a copy of this with the optional cursor parameter set.
   */
  def withSize(size: Long) = copy(size = Some(size))

  /**
   * Returns a copy of this with the optional sources parameter set.
   */
  def withSources(sources: Boolean) = copy(sources = sources)

  /**
   * Returns a copy of this with the optional events parameter set.
   */
  def withEvents(events: Boolean) = copy(events = events)
}

/**
 * A Count function.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-reading-resources FaunaDB Resource Retrieval Functions]]
 */
case class Count(@(JsonProperty @field)("count") set: Set) extends Expression

/**
 * An Exists function.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-read_functions FaunaDB Read Functions]]
 */
case class Exists(@(JsonProperty @field)("exists") ref: Ref) extends Expression

/**
 * A Create function.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-modifying-resources FaunaDB Resource Modification Functions]]
 */
case class Create(@(JsonProperty @field)("create") ref: Expression, @(JsonProperty @field)("params") params: Expression = ObjectV.empty) extends Expression

/**
 * A Replace function.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-modifying-resources FaunaDB Resource Modification Functions]]
 */
case class Replace(@(JsonProperty @field)("replace") ref: Expression, @(JsonProperty @field)("params") params: Expression) extends Expression

/**
 * An Update function.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-modifying-resources FaunaDB Resource Modification Functions]]
 */
case class Update(@(JsonProperty @field)("update") ref: Expression, @(JsonProperty @field)("params") params: Expression) extends Expression

/**
 * A Delete function.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-modifying-resources FaunaDB Resource Modification Functions]]
 */
case class Delete(@(JsonProperty @field)("delete") ref: Expression) extends Expression


sealed trait Value extends Expression

/**
 * A Ref.
 *
 * '''Reference''': [[https://faunadb.com/documentation#queries-values-special_types FaunaDB Special Types]]
 */
case class Ref(@(JsonProperty @field @param)("@ref") value: String) extends Value with Expression {
  def this(parent: Ref, child: String) = this(parent.value + "/" + child)
}

case class Var(@(JsonProperty @field)("var") variable: String) extends Value with Expression

sealed trait Resource

case class Event(@(JsonProperty @field)("resource") resource: Ref,
                 @(JsonProperty @field)("action") action: String,
                 @(JsonProperty @field)("ts") ts: Long) extends Value

case object NullV extends Value {
  @(JsonValue @getter) val value = NullNode.instance;
}

case class StringV(@(JsonValue @getter) value: String) extends Value
case class NumberV(@(JsonValue @getter) value: Long) extends Value
case class DoubleV(@(JsonValue @getter) value: Double) extends Value
case class BooleanV(@(JsonValue @getter) value: Boolean) extends Value

object ArrayV {
  val empty = new ArrayV(Array[Value]())

  def apply(items: Value*) = {
    new ArrayV(Array(items: _*))
  }
}

case class ArrayV(@(JsonValue @getter) values: scala.Array[Value]) extends Value

object ObjectV {
  val empty = new ObjectV(scala.collection.Map.empty[String, Value])
  def apply(pairs: (String, Value)*) = new ObjectV(pairs.toMap)
}

case class ObjectV(@(JsonValue @getter) values: collection.Map[String, Value]) extends Value

case class Object(@(JsonProperty @field)("object") value: ObjectV) extends Value

case class Add(@(JsonProperty @field)("add") terms: Seq[Expression]) extends Value
