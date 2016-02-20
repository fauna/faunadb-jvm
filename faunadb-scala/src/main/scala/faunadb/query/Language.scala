package faunadb.query

import com.fasterxml.jackson.annotation._
import faunadb.types._
import language.experimental.macros
import scala.annotation.meta.{field, getter}
import scala.reflect.macros._

/**
 * Implicit conversions to FaunaDB value types, as well as helper functions to construct queries.
 *
 * These can be used by adding:
 * {{{
 *   import com.faunadb.client.query.Language._
 * }}}
 *
 */
object Language {

  implicit def boolToValue(unwrapped: Boolean) = BooleanV(unwrapped)
  implicit def stringToValue(unwrapped: String) = StringV(unwrapped)
  implicit def intToValue(unwrapped: Int) = NumberV(unwrapped)
  implicit def longToValue(unwrapped: Long) = NumberV(unwrapped)
  implicit def floatToValue(unwrapped: Float) = DoubleV(unwrapped)
  implicit def doubleToValue(unwrapped: Double) = DoubleV(unwrapped)

  /**
    * Enumeration for time units. Used by [[https://faunadb.com/documentation/queries#time_functions]].
    */
  sealed abstract class TimeUnit(val value: String)
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

  /**
    * Helper for path syntax
    */
  protected class Path(val segments: Value*) { def /(seg: Value) = new Path(segments :+ seg: _*) }
  implicit def strToPath(str: String) = new Path(StringV(str))
  implicit def intToPath(int: Int) = new Path(NumberV(int))

  /**
    * Helper for pagination cursors
    */
  sealed trait Cursor
  case class Before(value: Value) extends Cursor
  case class After(value: Value) extends Cursor
  case object NoCursor extends Cursor

  private def varargs(exprs: Seq[Value]) =
    exprs match {
      case Seq(e) => e
      case es     => ArrayV(es: _*)
    }

  // Values

  /**
    * An Array value.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#values]]
    */
  def Arr(elems: Value*): Value =
    ArrayV(elems: _*)

  /**
    * An Object value.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#values]]
    */
  def Obj(pairs: (String, Value)*): Value =
    ObjectV("object" -> ObjectV(pairs: _*))

  // Basic Forms

  /**
    * A Let expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#basic_forms]]
    */

  def Let(block: => Any): Value = macro LanguageMacros.let

  def Let(bindings: Seq[(String, Value)], in: Value): Value =
    ObjectV("let" -> ObjectV(bindings: _*), "in" -> in)

  /**
    * A Var expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#basic_forms]]
    */
  def Var(name: String): Value =
    ObjectV("var" -> StringV(name))

  /**
   * An If expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#basic_forms]]
   */
  def If(condition: Value, `then`: Value, `else`: Value): Value =
    ObjectV("if" -> condition, "then" -> `then`, "else" -> `else`)

  /**
   * A Do expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#basic_forms]]
   */
  def Do(exprs: Value*): Value =
    ObjectV("do" -> varargs(exprs))

  /**
   * A Lambda expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#basic_forms]]
   */
  def Lambda(fn: Value => Value): Value = macro LanguageMacros.lambda
  def Lambda(fn: (Value, Value) => Value): Value = macro LanguageMacros.lambda
  def Lambda(fn: (Value, Value, Value) => Value): Value = macro LanguageMacros.lambda
  def Lambda(fn: (Value, Value, Value, Value) => Value): Value = macro LanguageMacros.lambda
  def Lambda(fn: (Value, Value, Value, Value, Value) => Value): Value = macro LanguageMacros.lambda
  def Lambda(fn: (Value, Value, Value, Value, Value, Value) => Value): Value = macro LanguageMacros.lambda
  def Lambda(fn: (Value, Value, Value, Value, Value, Value, Value) => Value): Value = macro LanguageMacros.lambda
  def Lambda(fn: (Value, Value, Value, Value, Value, Value, Value, Value) => Value): Value = macro LanguageMacros.lambda
  def Lambda(fn: (Value, Value, Value, Value, Value, Value, Value, Value, Value) => Value): Value = macro LanguageMacros.lambda
  def Lambda(fn: (Value, Value, Value, Value, Value, Value, Value, Value, Value, Value) => Value): Value = macro LanguageMacros.lambda

  def Lambda(lambda: Value, expr: Value) =
    ObjectV("lambda" -> lambda, "expr" -> expr)

  class LanguageMacros(val c: whitebox.Context) {
    import c.universe._

    val T = q"_root_.faunadb.types"
    val M = q"_root_.faunadb.query.Language"

    def let(block: c.Tree): c.Tree = {
      val (vals, expr) = c.untypecheck(block.duplicate) match {
        case Block(stmts, expr) =>
          stmts foreach {
            case v @ ValDef(_, _, _, _) => ()
            case _ => c.abort(c.enclosingPosition, "Let call does not have the structure: Let { val a = ???, ..., val n = ???, <in_expr> }")
          }

          (stmts.asInstanceOf[List[ValDef]], expr)
        case expr => (Nil, expr)
      }

      val varDefs = vals map { v => q"val ${v.name} = $M.Var(${v.name.toString})" }
      val bindings = vals map { v => q"(${v.name.toString}, ${v.rhs}: $T.Value)" }

      val tv = q"$M.Let($bindings, { ..$varDefs; $expr })"

      tv
    }

    def lambda(fn: c.Tree): c.Tree = {
      val (vals, expr) = c.untypecheck(fn.duplicate) match {
        case Function(ps, expr) => (ps, expr)
        case _ => c.abort(c.enclosingPosition, "type mismatch: Argument must be a function literal.")
      }

      val varDefs = vals map { v => q"val ${v.name} = $M.Var(${v.name.toString})" }
      val paramsV = vals map { v => q"$T.StringV(${v.name.toString})" } match {
        case List(p) => p
        case ps => q"$T.ArrayV(..$ps)"
      }

      val tv = q"$M.Lambda($paramsV, { ..$varDefs; $expr })"

      tv
    }
  }

  // Collection Functions

  /**
   * A Map expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
   */
  def Map(lambda: Value, collection: Value): Value =
    ObjectV("map" -> lambda, "collection" -> collection)

  /**
   * A Foreach expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
   */
  def Foreach(lambda: Value, collection: Value): Value =
    ObjectV("foreach" -> lambda, "collection" -> collection)

  /**
    * A Filter expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
    */
  def Filter(lambda: Value, collection: Value): Value =
    ObjectV("filter" -> lambda, "collection" -> collection)

  /**
    * A Prepend expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
    */
  def Prepend(elems: Value, collection: Value): Value =
    ObjectV("prepend" -> elems, "collection" -> collection)

  /**
    * An Append expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
    */
  def Append(elems: Value, collection: Value): Value =
    ObjectV("append" -> elems, "collection" -> collection)

  /**
    * A Take expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
    */
  def Take(num: Value, collection: Value): Value =
    ObjectV("take" -> num, "collection" -> collection)

  /**
    * A Drop expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#collection_functions]]
    */
  def Drop(num: Value, collection: Value): Value =
    ObjectV("drop" -> num, "collection" -> collection)

  // Read Functions

  /**
   * A Get expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#read_functions]]
   */
  def Get(resource: Value): Value =
    ObjectV("get" -> resource)

  def Get(resource: Value, ts: Value): Value =
    ObjectV("get" -> resource, "ts" -> ts)

  /**
   * A Paginate expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#read_functions]]
   */
  def Paginate(
    resource: Value,
    cursor: Cursor = NoCursor,
    ts: Value = NullV,
    size: Value = NullV,
    sources: Value = NullV,
    events: Value = NullV): Value = {

    val call = List.newBuilder[(String, Value)]
    call += "paginate" -> resource

    cursor match {
      case b: Before => call += "before" -> b.value
      case a: After => call += "after" -> a.value
      case _ => ()
    }

    if (ts != NullV) call += "ts" -> ts
    if (size != NullV) call += "size" -> size
    if (events != NullV) call += "events" -> events
    if (sources != NullV) call += "sources" -> sources

    ObjectV(call.result: _*)
  }

  /**
   * An Exists expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#read_functions]]
   */
  def Exists(ref: Value): Value =
    ObjectV("exists" -> ref)

  def Exists(ref: Value, ts: Value): Value =
    ObjectV("exists" -> ref, "ts" -> ts)

  /**
   * A Count expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#read_functions]]
   */
  def Count(set: Value): Value =
    ObjectV("count" -> set)

  // Write Functions

  /**
   * A Create expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#write_functions]]
   */
  def Create(ref: Value, params: Value): Value =
    ObjectV("create" -> ref, "params" -> params)

  /**
   * An Update expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#write_functions]]
   */
  def Update(ref: Value, params: Value): Value =
    ObjectV("update" -> ref, "params" -> params)

  /**
   * A Replace expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#write_functions]]
   */
  def Replace(ref: Value, params: Value): Value =
    ObjectV("replace" -> ref, "params" -> params)

  /**
   * A Delete expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#write_functions]]
   */
  def Delete(ref: Value): Value =
    ObjectV("delete" -> ref)

  /**
    * An Insert expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#write_functions]]
    */
  def Insert(ref: Value, ts: Long, action: Action, params: Value): Value =
    Insert(ref, ts, action.value, params)

  def Insert(ref: Value, ts: Long, action: Value, params: Value): Value =
    ObjectV("insert" -> ref, "ts" -> ts, "action" -> action, "params" -> params)

  /**
    * A Remove expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#write_functions]]
    */
  def Remove(ref: Value, ts: Long, action: Action): Value =
    Remove(ref, ts, action.value)

  def Remove(ref: Value, ts: Long, action: Value): Value =
    ObjectV("remove" -> ref, "ts" -> ts, "action" -> action)

  // Set Constructors

  /**
   * A Match set.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#sets]]
   */
  def Match(index: Value, terms: Value*) =
    ObjectV("match" -> varargs(terms), "index" -> index)

  /**
   * A Union set.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#sets]]
   */
  def Union(sets: Value*): Value =
    ObjectV("union" -> varargs(sets))

  /**
   * An Intersection set.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#sets]]
   */
  def Intersection(sets: Value*): Value =
    ObjectV("intersection" -> varargs(sets))

  /**
   * A Difference set.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#sets]]
   */
  def Difference(sets: Value*): Value =
    ObjectV("difference" -> varargs(sets))

  /**
   * A Distinct set.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#sets]]
   */
  def Distinct(set: Value): Value =
    ObjectV("distinct" -> set)

  /**
   * A Join set.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#sets]]
   */
  def Join(source: Value, `with`: Value): Value =
    ObjectV("join" -> source, "with" -> `with`)

  // Authentication Functions

  /**
    * A Login expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#auth_functions]]
    */
  def Login(ref: Value, params: Value): Value =
    ObjectV("login" -> ref, "params" -> params)

  /**
    * A Logout expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#auth_functions]]
    */
  def Logout(invalidateAll: Value): Value =
    ObjectV("logout" -> invalidateAll)

  /**
    * An Identify expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#auth_functions]]
    */
  def Identify(ref: Value, password: Value): Value =
    ObjectV("identify" -> ref, "password" -> password)

  // String Functions

  /**
   * A Concat expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#string_functions]]
   */
  def Concat(term: Value): Value =
    ObjectV("concat" -> term)

  def Concat(term: Value, separator: Value): Value =
    ObjectV("concat" -> term, "separator" -> separator)

  /**
   * A Casefold expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#string_functions]]
   */
  def Casefold(term: Value): Value =
    ObjectV("casefold" -> term)

  // Time Functions

  /**
    * A Time expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#time_functions]]
    */
  def Time(str: Value): Value =
    ObjectV("time" -> str)

  /**
    * An Epoch expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#time_functions]]
    */
  def Epoch(num: Value, unit: TimeUnit): Value =
    Epoch(num, unit.value)

  def Epoch(num: Value, unit: Value): Value =
    ObjectV("epoch" -> num, "unit" -> unit)

  /**
    * A Date expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#time_functions]]
    */
  def Date(str: Value): Value =
    ObjectV("date" -> str)

  // Misc Functions

  /**
   * An Equals expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Equals(terms: Value*): Value =
    ObjectV("equals" -> varargs(terms))

  /**
   * A Contains expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Contains(path: Path, in: Value): Value =
    Contains(varargs(path.segments), in)

  def Contains(path: Value, in: Value): Value =
    ObjectV("contains" -> path, "in" -> in)

  /**
   * A Select expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Select(path: Path, from: Value): Value =
    Select(varargs(path.segments), from)

  def Select(path: Path, from: Value, default: Value): Value =
    Select(varargs(path.segments), from, default)

  def Select(path: Value, from: Value): Value =
    ObjectV("select" -> path, "from" -> from)

  def Select(path: Value, from: Value, default: Value): Value =
    ObjectV("select" -> path, "from" -> from, "default" -> default)

  /**
   * An Add expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Add(terms: Value*): Value =
    ObjectV("add" -> varargs(terms))

  /**
   * A Multiply expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Multiply(terms: Value*): Value =
    ObjectV("multiply" -> varargs(terms))

  /**
   * A Subtract expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Subtract(terms: Value*): Value =
    ObjectV("subtract" -> varargs(terms))

  /**
   * A Divide expression.
   *
   * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
   */
  def Divide(terms: Value*): Value =
    ObjectV("divide" -> varargs(terms))

  /**
    * A Modulo expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
    */
  def Modulo(terms: Value*): Value =
    ObjectV("modulo" -> varargs(terms))

  /**
    * A LT expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
    */
  def LT(terms: Value*): Value =
    ObjectV("lt" -> varargs(terms))

  /**
    * A LTE expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
    */
  def LTE(terms: Value*): Value =
    ObjectV("lte" -> varargs(terms))

  /**
    * A GT expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
    */
  def GT(terms: Value*): Value =
    ObjectV("gt" -> varargs(terms))

  /**
    * A GTE expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
    */
  def GTE(terms: Value*): Value =
    ObjectV("gte" -> varargs(terms))

  /**
    * An And expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
    */
  def And(terms: Value*): Value =
    ObjectV("and" -> varargs(terms))

  /**
    * An Or expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
    */
  def Or(terms: Value*): Value =
    ObjectV("or" -> varargs(terms))

  /**
    * A Not expression.
    *
    * '''Reference''': [[https://faunadb.com/documentation/queries#misc_functions]]
    */
  def Not(term: Value): Value =
    ObjectV("not" -> term)
}
