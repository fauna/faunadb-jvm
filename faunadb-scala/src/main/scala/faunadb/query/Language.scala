package faunadb.query

import com.fasterxml.jackson.annotation._
import faunadb.types._

import scala.annotation.meta.{field, getter}

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
  sealed abstract class TimeUnit(val value: String)

  /**
    * Enumeration for time units. Used by [[https://faunadb.com/documentation/queries#time_functions]].
    */
  object TimeUnit {
    case object Second extends TimeUnit("second")
    case object Millisecond extends TimeUnit("millisecond")
    case object Microsecond extends TimeUnit("microsecond")
    case object Nanosecond extends TimeUnit("nanosecond")
  }

  /**
    * Enumeration for event action types.
    */
  sealed abstract class Action(val value: String)
  object Action {
    case object Create extends Action("create")
    case object Delete extends Action("delete")
  }

  implicit def stringToObjectPath(str: String) = ObjectPath(str)
  implicit def intToArrayPath(i: Int) = ArrayPath(i)
  implicit def stringToValue(unwrapped: String) = StringV(unwrapped)
  implicit def intToValue(unwrapped: Int) = NumberV(unwrapped)
  implicit def longToValue(unwrapped: Long) = NumberV(unwrapped)
  implicit def boolToValue(unwrapped: Boolean) = BooleanV(unwrapped)
  implicit def arrayToValue(unwrapped: Array[Value]) = ArrayV(unwrapped)
  implicit def mapToValue(unwrapped: collection.Map[String, Value]) = ObjectV(unwrapped)
  implicit def doubleToValue(unwrapped: Double) = DoubleV(unwrapped)
  implicit def pairToValuePair[T](p: (String, T))(implicit convert: T => Value) = {
    (p._1, convert(p._2))
  }

  /**
    * A Let expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#basic_forms]]
    */
  def Let(vars: collection.Map[String, Value], in: Value): Value = {
    ObjectV("let" -> ObjectV(vars), "in" -> in)
  }

  /**
   * A Do expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#basic_forms]]
   */
  def Do(exprs: Iterable[Value]): Value = {
    ObjectV("do" -> ArrayV(exprs.toArray))
  }

  /**
   * An If expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#basic_forms]]
   */
  def If(condition: Value, `then`: Value, `else`: Value): Value = {
    ObjectV("if" -> condition, "then" -> `then`, "else" -> `else`)
  }

  /**
   * A Quote expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#basic_forms]]
   */
  def Quote(quote: Value): Value = {
    ObjectV("quote" -> quote)
  }

  /**
   * A Select expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#basic_forms]]
   */
  def Select(path: Iterable[Path], from: Value): Value = {
    ObjectV("select" -> ArrayV(path.map(_.value).toArray), "from" -> from)
  }

  /**
   * A Lambda expression.
   *
   * '''Reference''': TBD
   */
  def Lambda(argument: String, expr: Value): Value = {
    ObjectV("lambda" -> StringV(argument), "expr" -> expr)
  }

  /**
   * A Map expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
   */
  def Map(lambda: Value, collection: Value): Value = {
    ObjectV("map" -> lambda, "collection" -> collection)
  }

  /**
   * A Foreach expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
   */
  def Foreach(lambda: Value, collection: Value): Value = {
    ObjectV("foreach" -> lambda, "collection" -> collection)
  }

  /**
    * A Filter expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
    */
  def Filter(lambda: Value, collection: Value): Value = {
    ObjectV("filter" -> lambda, "collection" -> collection)
  }

  /**
    * A Take expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
    */
  def Take(num: Value, collection: Value): Value = {
    ObjectV("take" -> num, "collection" -> collection)
  }

  /**
    * A Drop expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
    */
  def Drop(num: Value, collection: Value): Value = {
    ObjectV("drop" -> num, "collection" -> collection)
  }

  /**
    * A Prepend expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
    */
  def Prepend(elems: Value, collection: Value): Value = {
    ObjectV("prepend" -> elems, "collection" -> collection)
  }

  /**
    * An Append expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
    */
  def Append(elems: Value, collection: Value): Value = {
    ObjectV("append" -> elems, "collection" -> collection)
  }

  /**
   * A Match set.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#sets]]
   */
  def Match(term: Value, index: Ref): Value = {
    ObjectV("match" -> term, "index" -> index)
  }

  /**
   * A Union set.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#sets]]
   */
  def Union(sets: Iterable[Value]): Value = {
    ObjectV("union" -> ArrayV(sets.toArray))
  }

  /**
   * An Intersection set.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#sets]]
   */
  def Intersection(sets: Iterable[Value]): Value = {
    ObjectV("intersection" -> ArrayV(sets.toArray))
  }

  /**
   * A Difference set.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#sets]]
   */
  def Difference(sets: Iterable[Value]): Value = {
    ObjectV("difference" -> ArrayV(sets.toArray))
  }

  /**
   * A Join set.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#sets]]
   */
  def Join(source: Value, target: Value): Value = {
    ObjectV("join" -> source, "with" -> target)
  }

  /**
   * A Get expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#read_functions]]
   */
  def Get(resource: Value): Value = {
    ObjectV("get" -> resource)
  }

  /**
   * A Paginate expression.
   *
   * The paginate expression takes optional parameters. These can either be specified by named parameters on the constructor:
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
  def Paginate(resource: Value,
               ts: Option[Long] = None,
               cursor: Option[Cursor] = None,
               size: Option[Long] = None,
               sources: Boolean = false,
               events: Boolean = false): Value = {
    val builder = collection.immutable.Map.newBuilder[String, Value]
    builder += "paginate" -> resource

    ts foreach { builder += "ts" -> _}
    size foreach { builder += "size" -> _ }

    cursor foreach { c =>
      c match {
        case b: Before =>
          builder += "before" -> b.value
        case a: After =>
          builder += "after" -> a.value
      }
    }

    if (events) {
      builder += "events" -> events
    }

    if (sources) {
      builder += "sources" -> sources
    }

    ObjectV(builder.result())
  }

  /**
   * A Count expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#read_functions]]
   */
  def Count(set: Value): Value = {
    ObjectV("count" -> set)
  }

  /**
   * An Exists expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#read_functions]]
   */
  def Exists(ref: Value): Value = {
    ObjectV("exists" -> ref)
  }

  /**
   * A Create expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#write_functions]]
   */
  def Create(ref: Value, params: Value): Value = {
    ObjectV("create" -> ref, "params" -> params)
  }

  /**
   * A Replace expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#write_functions]]
   */
  def Replace(ref: Value, params: Value): Value = {
    ObjectV("replace" -> ref, "params" -> params)
  }

  /**
   * An Update expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#write_functions]]
   */
  def Update(ref: Value, params: Value): Value = {
    ObjectV("update" -> ref, "params" -> params)
  }

  /**
   * A Delete expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#write_functions]]
   */
  def Delete(ref: Value): Value = {
    ObjectV("delete" -> ref)
  }

  /**
    * An Insert expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#write_functions]]
    */
  def Insert(ref: Value, ts: Long, action: Action, params: Value): Value = {
    ObjectV("insert" -> ref, "ts" -> ts, "action" -> action.value, "params" -> params)
  }

  /**
    * A Remove expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#write_functions]]
    */
  def Remove(ref: Value, ts: Long, action: Action): Value = {
    ObjectV("remove" -> ref, "ts" -> ts, "action" -> action.value)
  }

  def Object(value: ObjectV) = {
    ObjectV("object" -> value)
  }

  /**
   * An Add expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Add(terms: Iterable[Value]): Value = {
    ObjectV("add" -> ArrayV(terms.toArray))
  }

  /**
   * An Equals expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Equals(terms: Iterable[Value]): Value = {
    ObjectV("equals" -> ArrayV(terms.toArray))
  }

  /**
   * A Concat expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Concat(terms: Iterable[Value]): Value = {
    ObjectV("concat" -> ArrayV(terms.toArray))
  }

  def Concat(terms: Iterable[Value], separator: Value): Value = {
    ObjectV("concat" -> ArrayV(terms.toArray), "separator" -> separator)
  }

  /**
   * A Contains expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Contains(path: Iterable[Path], in: Value): Value = {
    ObjectV("contains" -> ArrayV(path.map(_.value).toArray), "in" -> in)
  }

  /**
   * A Multiply expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Multiply(terms: Iterable[Value]): Value = {
    ObjectV("multiply" -> ArrayV(terms.toArray))
  }

  /**
   * A Divide expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Divide(terms: Iterable[Value]): Value = {
    ObjectV("divide" -> ArrayV(terms.toArray))
  }

  /**
    * A Modulo expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
    */
  def Modulo(terms: Iterable[Value]): Value = {
    ObjectV("modulo" -> ArrayV(terms.toArray))
  }

  /**
   * A Subtract expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Subtract(terms: Iterable[Value]): Value = {
    ObjectV("subtract" -> ArrayV(terms.toArray))
  }

  /**
    * An And expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
    */
  def And(terms: Iterable[Value]): Value = {
    ObjectV("and" -> ArrayV(terms.toArray))
  }

  /**
    * An Or expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
    */
  def Or(terms: Iterable[Value]): Value = {
    ObjectV("or" -> ArrayV(terms.toArray))
  }

  /**
    * A Not expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
    */
  def Not(term: Value): Value = {
    ObjectV("not" -> term)
  }

  /**
    * A Login expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#auth_functions]]
    */
  def Login(ref: Value, params: Value): Value = {
    ObjectV("login" -> ref, "params" -> params)
  }

  /**
    * A Logout expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#auth_functions]]
    */
  def Logout(invalidateAll: Boolean): Value = {
    ObjectV("logout" -> invalidateAll)
  }

  /**
    * An Identify expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#auth_functions]]
    */
  def Identify(ref: Value, password: Value): Value = {
    ObjectV("identify" -> ref, "password" -> password)
  }

  /**
    * A Time expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#time_functions]]
    */
  def Time(str: Value): Value = {
    ObjectV("time" -> str)
  }

  /**
    * An Epoch expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#time_functions]]
    */
  def Epoch(num: Value, unit: TimeUnit) = {
    ObjectV("epoch" -> num, "unit" -> unit.value)
  }

  def Epoch(num: Value, unit: String) = {
    ObjectV("epoch" -> num, "unit" -> unit)
  }

  /**
    * A Date expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#time_functions]]
    */
  def Date(str: Value) = {
    ObjectV("date" -> str)
  }
}

sealed trait Path {
  def value: Value
}
case class ObjectPath(@(JsonValue @getter) field: String) extends Path {
  def value = StringV(field)
}
case class ArrayPath(@(JsonValue @getter) index: Int) extends Path {
  def value = NumberV(index)
}

