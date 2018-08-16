package faunadb

import com.fasterxml.jackson.annotation.JsonValue
import faunadb.values._
import scala.annotation.meta.getter
import scala.language.experimental.macros
import scala.language.implicitConversions

/**
  * Functions modeling the FaunaDB Query language.
  *
  * Instances of these classes can be composed to model a query expression, which can then be passed to
  * [[FaunaClient!.query(expr:faunadb\.query\.Expr)*]] in order to execute the query.
  *
  * ===Examples===
  *
  * {{{
  * val query = Create(Class("spells"), Obj("data" -> Obj("name" -> "Magic Missile")))
  * }}}
  *
  * {{{
  * val query = Map(Paginate(Match(Index("spells_by_name"), "Magic Missile")), Lambda(r => Get(r)))
  * }}}
  */
package query {

  /**
    * A query language expression.
    */
  case class Expr private[query] (@(JsonValue @getter) value: Value) extends AnyVal

  object Expr {
    implicit def encode[T: Encoder](obj: T): Expr = Expr(wrapValue(obj))

    private def wrapValue(value: Value): Value = value match {
      case ObjectV(fields) => ObjectV("object" -> ObjectV(fields.map { case (k, v) => (k, wrapValue(v)) }))
      case ArrayV(values) => ArrayV(values.map(wrapValue))
      case _ => value
    }
  }

  /**
    * Enumeration for time units. Used by [[https://fauna.com/documentation/queries#time_functions]].
    */
  sealed abstract class TimeUnit(val expr: Expr)
  object TimeUnit {
    case object Second extends TimeUnit("second")
    case object Millisecond extends TimeUnit("millisecond")
    case object Microsecond extends TimeUnit("microsecond")
    case object Nanosecond extends TimeUnit("nanosecond")
  }

  /**
    * Enumeration for event action types.
    */
  sealed abstract class Action(val expr: Expr)
  object Action {
    case object Create extends Action("create")
    case object Delete extends Action("delete")
  }

  /**
    * Enumeration for casefold operation.
    */
  sealed abstract class Normalizer(val expr: Expr)
  object Normalizer {
    case object NFD extends Normalizer("NFD")
    case object NFC extends Normalizer("NFC")
    case object NFKD extends Normalizer("NFKD")
    case object NFKC extends Normalizer("NFKC")
    case object NFKCCaseFold extends Normalizer("NFKCCaseFold")
  }

  /**
    * Helper for path syntax
    */
  case class Path private (segments: Expr*) extends AnyVal {
    def /(sub: Path) = Path(segments ++ sub.segments: _*)
  }

  /**
    * Helper for pagination cursors
    */
  sealed trait Cursor
  case class Before(expr: Expr) extends Cursor
  case class After(expr: Expr) extends Cursor
  case object NoCursor extends Cursor
}

package object query {

  // implicit conversions

  implicit def strToPath(str: String): Path = Path(Expr(StringV(str)))
  implicit def intToPath(int: Int): Path = Path(Expr(LongV(int)))
  implicit def pathToExpr(path: Path): Expr = Expr(varargs(path.segments))

  // Helpers

  private def varargs(exprs: Seq[Expr]) =
    exprs match {
      case Seq(e) => e.value
      case es     => ArrayV(es map (_.value): _*)
    }

  private def unwrap(exprs: Seq[Expr]) =
    exprs map { _.value }

  private def unwrapPairs(exprs: Seq[(String, Expr)]) =
    exprs map { t => (t._1, t._2.value) }

  // Values

  /**
    * Creates a RefV value. The string "classes/widget/123" will be equivalent to:
    * {{{
    * RefV("123", RefV("widget", Native.Classes))
    * }}}
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#values]]
    */
  def Ref(value: String): Expr =
    Expr(ObjectV("@ref" -> StringV(value)))

  def Ref(classRef: Expr, id: Expr): Expr =
    Expr(ObjectV("ref" -> classRef.value, "id" -> id.value))

  /** Native reference to all classes */
  def Classes(scope: Expr = NullV): Expr =
    Expr(ObjectV("classes" -> scope.value))

  /** Native reference to all databases */
  def Databases(scope: Expr = NullV): Expr =
    Expr(ObjectV("databases" -> scope.value))

  /** Native reference to all indexes */
  def Indexes(scope: Expr = NullV): Expr =
    Expr(ObjectV("indexes" -> scope.value))

  /** Native reference to all functions */
  def Functions(scope: Expr = NullV): Expr =
    Expr(ObjectV("functions" -> scope.value))

  /** Native reference to all keys */
  def Keys(scope: Expr = NullV): Expr =
    Expr(ObjectV("keys" -> scope.value))

  /** Native reference to all tokens */
  def Tokens(scope: Expr = NullV): Expr =
    Expr(ObjectV("tokens" -> scope.value))

  /** Native reference to all credentials */
  def Credentials(scope: Expr = NullV): Expr =
    Expr(ObjectV("credentials" -> scope.value))

  /**
    * An Array value.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#values]]
    */
  def Arr(elems: Expr*): Expr =
    Expr(ArrayV(unwrap(elems): _*))

  /**
    * An Object value.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#values]]
    */
  def Obj(pairs: (String, Expr)*): Expr =
    Expr(ObjectV("object" -> ObjectV(unwrapPairs(pairs): _*)))

  /**
    * A Null value.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#values]]
    */
  def Null(): Expr =
    Expr(NullV)

  // Basic Forms

  /**
    * A Abort expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#basic-forms]]
    */
  def Abort(msg: Expr): Expr =
    Expr(ObjectV("abort" -> msg.value))

  /**
    * A Call expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#basic-forms]]
    */
  def Call(ref: Expr, arguments: Expr*) =
    Expr(ObjectV("call" -> ref.value, "arguments" -> varargs(arguments)))

  /**
    * A Query expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#basic-forms]]
    */
  def Query(fn: Expr => Expr): Expr = macro QueryMacros.query
  def Query(fn: (Expr, Expr) => Expr): Expr = macro QueryMacros.query
  def Query(fn: (Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.query
  def Query(fn: (Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.query
  def Query(fn: (Expr, Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.query
  def Query(fn: (Expr, Expr, Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.query
  def Query(fn: (Expr, Expr, Expr, Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.query
  def Query(fn: (Expr, Expr, Expr, Expr, Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.query
  def Query(fn: (Expr, Expr, Expr, Expr, Expr, Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.query
  def Query(fn: (Expr, Expr, Expr, Expr, Expr, Expr, Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.query

  def Query(lambda: Expr) =
    Expr(ObjectV("query" -> lambda.value))

  /**
    * A At expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#basic-forms]]
    */
  def At(timestamp: Expr, expr: Expr): Expr =
    Expr(ObjectV("at" -> timestamp.value, "expr" -> expr.value))

  /**
    * A Let expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#basic-forms]]
    */

  def Let(block: => Any): Expr = macro QueryMacros.let

  def Let(bindings: Seq[(String, Expr)], in: Expr): Expr =
    Expr(ObjectV("let" -> ArrayV(unwrapPairs(bindings) map { ObjectV(_) }: _*), "in" -> in.value))

  /**
    * A Var expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#basic-forms]]
    */
  def Var(name: String): Expr =
    Expr(ObjectV("var" -> StringV(name)))

  /**
   * An If expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#basic-forms]]
   */
  def If(pred: Expr, `then`: Expr, `else`: Expr): Expr =
    Expr(ObjectV("if" -> pred.value, "then" -> `then`.value, "else" -> `else`.value))

  /**
   * A Do expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#basic-forms]]
   */
  def Do(exprs: Expr*): Expr =
    Expr(ObjectV("do" -> ArrayV(unwrap(exprs): _*)))

  /**
   * A Lambda expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#basic-forms]]
   */
  def Lambda(fn: Expr => Expr): Expr = macro QueryMacros.lambda
  def Lambda(fn: (Expr, Expr) => Expr): Expr = macro QueryMacros.lambda
  def Lambda(fn: (Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.lambda
  def Lambda(fn: (Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.lambda
  def Lambda(fn: (Expr, Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.lambda
  def Lambda(fn: (Expr, Expr, Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.lambda
  def Lambda(fn: (Expr, Expr, Expr, Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.lambda
  def Lambda(fn: (Expr, Expr, Expr, Expr, Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.lambda
  def Lambda(fn: (Expr, Expr, Expr, Expr, Expr, Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.lambda
  def Lambda(fn: (Expr, Expr, Expr, Expr, Expr, Expr, Expr, Expr, Expr, Expr) => Expr): Expr = macro QueryMacros.lambda

  def Lambda(lambda: Expr, expr: Expr): Expr =
    Expr(ObjectV("lambda" -> lambda.value, "expr" -> expr.value))

  // Collection Functions

  /**
   * A Map expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#collections]]
   */
  def Map(collection: Expr, lambda: Expr): Expr =
    Expr(ObjectV("map" -> lambda.value, "collection" -> collection.value))

  /**
   * A Foreach expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#collections]]
   */
  def Foreach(collection: Expr, lambda: Expr): Expr =
    Expr(ObjectV("foreach" -> lambda.value, "collection" -> collection.value))

  /**
    * A Filter expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#collections]]
    */
  def Filter(collection: Expr, lambda: Expr): Expr =
    Expr(ObjectV("filter" -> lambda.value, "collection" -> collection.value))

  /**
    * A Prepend expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#collections]]
    */
  def Prepend(elems: Expr, collection: Expr): Expr =
    Expr(ObjectV("prepend" -> elems.value, "collection" -> collection.value))

  /**
    * An Append expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#collections]]
    */
  def Append(elems: Expr, collection: Expr): Expr =
    Expr(ObjectV("append" -> elems.value, "collection" -> collection.value))

  /**
    * A Take expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#collections]]
    */
  def Take(num: Expr, collection: Expr): Expr =
    Expr(ObjectV("take" -> num.value, "collection" -> collection.value))

  /**
    * A Drop expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#collections]]
    */
  def Drop(num: Expr, collection: Expr): Expr =
    Expr(ObjectV("drop" -> num.value, "collection" -> collection.value))

  /**
    * A IsEmpty expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#collections]]
    */
  def IsEmpty(collection: Expr): Expr =
    Expr(ObjectV("is_empty" -> collection.value))

  /**
    * A IsNonEmpty expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#collections]]
    */
  def IsNonEmpty(collection: Expr): Expr =
    Expr(ObjectV("is_nonempty" -> collection.value))

  // Read Functions

  /**
   * A Get expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#read-functions]]
   */
  def Get(resource: Expr): Expr =
    Expr(ObjectV("get" -> resource.value))

  def Get(resource: Expr, ts: Expr): Expr =
    Expr(ObjectV("get" -> resource.value, "ts" -> ts.value))

  /**
    * A KeyFromSecret expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#read-functions]]
    */
  def KeyFromSecret(secret: Expr): Expr =
    Expr(ObjectV("key_from_secret" -> secret.value))

  /**
   * A Paginate expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#read-functions]]
   */
  def Paginate(
    resource: Expr,
    cursor: Cursor = NoCursor,
    ts: Expr = Expr(NullV),
    size: Expr = Expr(NullV),
    sources: Expr = Expr(NullV),
    events: Expr = Expr(NullV)): Expr = {

    val call = List.newBuilder[(String, Value)]
    call += "paginate" -> resource.value

    cursor match {
      case b: Before => call += "before" -> b.expr.value
      case a: After => call += "after" -> a.expr.value
      case _ => ()
    }

    val nullExpr = Expr(NullV)

    if (ts != nullExpr) call += "ts" -> ts.value
    if (size != nullExpr) call += "size" -> size.value
    if (events != nullExpr) call += "events" -> events.value
    if (sources != nullExpr) call += "sources" -> sources.value

    Expr(ObjectV(call.result: _*))
  }

  /**
   * An Exists expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#read-functions]]
   */
  def Exists(ref: Expr): Expr =
    Expr(ObjectV("exists" -> ref.value))

  def Exists(ref: Expr, ts: Expr): Expr =
    Expr(ObjectV("exists" -> ref.value, "ts" -> ts.value))

  // Write Functions

  /**
   * A Create expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#write-functions]]
   */
  def Create(ref: Expr, params: Expr): Expr =
    Expr(ObjectV("create" -> ref.value, "params" -> params.value))

  /**
   * An Update expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#write-functions]]
   */
  def Update(ref: Expr, params: Expr): Expr =
    Expr(ObjectV("update" -> ref.value, "params" -> params.value))

  /**
   * A Replace expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#write-functions]]
   */
  def Replace(ref: Expr, params: Expr): Expr =
    Expr(ObjectV("replace" -> ref.value, "params" -> params.value))

  /**
   * A Delete expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#write-functions]]
   */
  def Delete(ref: Expr): Expr =
    Expr(ObjectV("delete" -> ref.value))

  /**
    * An Insert expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#write-functions]]
    */
  def Insert(ref: Expr, ts: Expr, action: Action, params: Expr): Expr =
    Insert(ref, ts, action.expr, params)

  def Insert(ref: Expr, ts: Expr, action: Expr, params: Expr): Expr =
    Expr(ObjectV("insert" -> ref.value, "ts" -> ts.value, "action" -> action.value, "params" -> params.value))

  /**
    * A Remove expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#write-functions]]
    */
  def Remove(ref: Expr, ts: Expr, action: Action): Expr =
    Remove(ref, ts, action.expr)

  def Remove(ref: Expr, ts: Expr, action: Expr): Expr =
    Expr(ObjectV("remove" -> ref.value, "ts" -> ts.value, "action" -> action.value))

  /**
    * A Create Class expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#write-functions]]
    */
  def CreateClass(params: Expr): Expr =
    Expr(ObjectV("create_class" -> params.value))

  /**
    * A Create Database expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#write-functions]]
    */
  def CreateDatabase(params: Expr): Expr =
    Expr(ObjectV("create_database" -> params.value))

  /**
    * A Create Key expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#write-functions]]
    */
  def CreateKey(params: Expr): Expr =
    Expr(ObjectV("create_key" -> params.value))

  /**
    * A Create Index expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#write-functions]]
    */
  def CreateIndex(params: Expr): Expr =
    Expr(ObjectV("create_index" -> params.value))

  /**
    * A Create Function expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#write-functions]]
    */
  def CreateFunction(params: Expr): Expr =
    Expr(ObjectV("create_function" -> params.value))

  // Set Constructors

  /**
    * A Singleton set.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#sets]]
    */
  def Singleton(ref: Expr): Expr =
    Expr(ObjectV("singleton" -> ref.value))

  /**
    * A Events set.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#sets]]
    */
  def Events(refSet: Expr): Expr =
    Expr(ObjectV("events" -> refSet.value))

  /**
   * A Match set.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#sets]]
   */
  def Match(index: Expr, terms: Expr*): Expr =
    Expr(ObjectV("match" -> varargs(terms), "index" -> index.value))

  /**
   * A Union set.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#sets]]
   */
  def Union(sets: Expr*): Expr =
    Expr(ObjectV("union" -> varargs(sets)))

  /**
   * An Intersection set.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#sets]]
   */
  def Intersection(sets: Expr*): Expr =
    Expr(ObjectV("intersection" -> varargs(sets)))

  /**
   * A Difference set.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#sets]]
   */
  def Difference(sets: Expr*): Expr =
    Expr(ObjectV("difference" -> varargs(sets)))

  /**
   * A Distinct set.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#sets]]
   */
  def Distinct(set: Expr): Expr =
    Expr(ObjectV("distinct" -> set.value))

  /**
   * A Join set.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#sets]]
   */
  def Join(source: Expr, `with`: Expr): Expr =
    Expr(ObjectV("join" -> source.value, "with" -> `with`.value))

  // Authentication Functions

  /**
    * A Login expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#authentication]]
    */
  def Login(ref: Expr, params: Expr): Expr =
    Expr(ObjectV("login" -> ref.value, "params" -> params.value))

  /**
    * A Logout expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#authentication]]
    */
  def Logout(invalidateAll: Expr): Expr =
    Expr(ObjectV("logout" -> invalidateAll.value))

  /**
    * An Identify expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#authentication]]
    */
  def Identify(ref: Expr, password: Expr): Expr =
    Expr(ObjectV("identify" -> ref.value, "password" -> password.value))

  /**
    * An Identity expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#authentication]]
    */
  def Identity(): Expr =
    Expr(ObjectV("identity" -> NullV))

  /**
    * An HasIdentity expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#authentication]]
    */
  def HasIdentity(): Expr =
    Expr(ObjectV("has_identity" -> NullV))

  // String Functions

  /**
   * A Concat expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def Concat(term: Expr): Expr =
    Expr(ObjectV("concat" -> term.value))

  def Concat(term: Expr, separator: Expr): Expr =
    Expr(ObjectV("concat" -> term.value, "separator" -> separator.value))

  /**
   * A Casefold expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def Casefold(term: Expr): Expr =
    Expr(ObjectV("casefold" -> term.value))

  def Casefold(term: Expr, normalizer: Normalizer): Expr =
    Casefold(term, normalizer.expr)

  def Casefold(term: Expr, normalizer: Expr): Expr =
    Expr(ObjectV("casefold" -> term.value, "normalizer" -> normalizer.value))

  /**
   * A FindStr expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def FindStr(value: Expr, find: Expr) : Expr =
    Expr(ObjectV("findstr" -> value.value, "find" -> find.value))

  /**
    * A FindStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStr(value: String, find: Expr) : Expr =
    Expr(ObjectV("findstr" -> StringV(value), "find" -> find.value))

  /**
    * A FindStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStr(value: Expr, find: String) : Expr =
    Expr(ObjectV("findstr" -> value.value, "find" -> StringV(find) ))

  /**
    * A FindStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStr(value: String, find: String) : Expr =
    Expr(ObjectV("findstr" -> StringV(value), "find" -> StringV(find)))

  /**
    * A FindStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStr(value: Expr, find: Expr, start: Expr) : Expr =
    Expr(ObjectV("findstr" -> value.value, "find" -> find.value, "start" -> start.value))

  /**
    * A FindStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStr(value: String, find: Expr, start: Expr) : Expr =
    Expr(ObjectV("findstr" -> StringV(value), "find" -> find.value, "start" -> start.value))

  /**
    * A FindStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStr(value: Expr, find: String, start: Expr) : Expr =
    Expr(ObjectV("findstr" -> value.value, "find" -> StringV(find), "start" -> start.value))

  /**
    * A FindStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStr(value: String, find: String, start: Expr) : Expr =
    Expr(ObjectV("findstr" -> StringV(value), "find" -> StringV(find), "start" -> start.value))

  /**
    * A FindStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStr(value: Expr, find: Expr, start: Long) : Expr =
    Expr(ObjectV("findstr" -> value.value, "find" -> find.value, "start" -> LongV(start)))

  /**
    * A FindStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStr(value: String, find: Expr, start: Long) : Expr =
    Expr(ObjectV("findstr" -> StringV(value), "find" -> find.value, "start" -> LongV(start)))

  /**
    * A FindStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStr(value: Expr, find: String, start: Long) : Expr =
    Expr(ObjectV("findstr" -> value.value, "find" -> StringV(find), "start" -> LongV(start)))

  /**
    * A FindStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStr(value: String, find: String, start: Long) : Expr =
    Expr(ObjectV("findstr" -> StringV(value), "find" -> StringV(find), "start" -> LongV(start)))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: Expr, start: Expr) : Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> pattern.value, "start" -> start.value))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: String, pattern: Expr, start: Expr) : Expr =
    Expr(ObjectV("findstrregex" -> StringV(value), "pattern" -> pattern.value, "start" -> start.value))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: String, start: Expr) : Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> StringV(pattern), "start" -> start.value))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: String, pattern: String, start: Expr) : Expr =
    Expr(ObjectV("findstrregex" -> StringV(value), "pattern" -> StringV(pattern), "start" -> start.value))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: Expr, start: Long) : Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> pattern.value, "start" -> LongV(start)))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: String, pattern: Expr, start: Long) : Expr =
    Expr(ObjectV("findstrregex" -> StringV(value), "pattern" -> pattern.value, "start" -> LongV(start)))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: String, start: Long) : Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> StringV(pattern), "start" -> LongV(start)))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: String, pattern: String, start: Long) : Expr =
    Expr(ObjectV("findstrregex" -> StringV(value), "pattern" -> StringV(pattern), "start" -> LongV(start)))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: Expr, start: Expr, num_results: Expr) : Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> pattern.value, "start" -> start.value, "num_results" -> num_results.value))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: String, pattern: Expr, start: Expr, num_results: Expr) : Expr =
    Expr(ObjectV("findstrregex" -> StringV(value), "pattern" -> pattern.value, "start" -> start.value, "num_results" -> num_results.value))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: String, start: Expr, num_results: Expr) : Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> StringV(pattern), "start" -> start.value, "num_results" -> num_results.value))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: String, pattern: String, start: Expr, num_results: Expr) : Expr =
    Expr(ObjectV("findstrregex" -> StringV(value), "pattern" -> StringV(pattern), "start" -> start.value, "num_results" -> num_results.value))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: Expr, start: Long, num_results: Expr) : Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> pattern.value, "start" -> LongV(start), "num_results" -> num_results.value))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: String, pattern: Expr, start: Long, num_results: Expr) : Expr =
    Expr(ObjectV("findstrregex" -> StringV(value), "pattern" -> pattern.value, "start" -> LongV(start), "num_results" -> num_results.value))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: String, start: Long, num_results: Expr) : Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> StringV(pattern), "start" -> LongV(start), "num_results" -> num_results.value))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: String, pattern: String, start: Long, num_results: Expr) : Expr =
    Expr(ObjectV("findstrregex" -> StringV(value), "pattern" -> StringV(pattern), "start" -> LongV(start), "num_results" -> num_results.value))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: Expr, start: Expr, num_results: Long) : Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> pattern.value, "start" -> start.value, "num_results" -> LongV(num_results)))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: String, pattern: Expr, start: Expr, num_results: Long) : Expr =
    Expr(ObjectV("findstrregex" -> StringV(value), "pattern" -> pattern.value, "start" -> start.value, "num_results" -> LongV(num_results)))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: String, start: Expr, num_results: Long) : Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> StringV(pattern), "start" -> start.value, "num_results" -> LongV(num_results)))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: String, pattern: String, start: Expr, num_results: Long) : Expr =
    Expr(ObjectV("findstrregex" -> StringV(value), "pattern" -> StringV(pattern), "start" -> start.value, "num_results" -> LongV(num_results)))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: Expr, start: Long, num_results: Long) : Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> pattern.value, "start" -> LongV(start), "num_results" -> LongV(num_results)))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: String, pattern: Expr, start: Long, num_results: Long) : Expr =
    Expr(ObjectV("findstrregex" -> StringV(value), "pattern" -> pattern.value, "start" -> LongV(start), "num_results" -> LongV(num_results)))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: String, start: Long, num_results: Long) : Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> StringV(pattern), "start" -> LongV(start), "num_results" -> LongV(num_results)))

  /**
    * A FindStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def FindStrRegex(value: String, pattern: String, start: Long, num_results: Long) : Expr =
    Expr(ObjectV("findstrregex" -> StringV(value), "pattern" -> StringV(pattern), "start" -> LongV(start), "num_results" -> LongV(num_results)))

  /**
   * A Length expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def Length(value: Expr) : Expr =
    Expr(ObjectV("length" -> value.value ))

  /**
    * A Length expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def Length(value: String) : Expr =
    Expr(ObjectV("length" -> StringV(value)))

  /**
   * A LowerCase expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def LowerCase(value: Expr) : Expr =
    Expr(ObjectV("lowercase" -> value.value))

  /**
    * A LowerCase expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def LowerCase(value: String) : Expr =
    Expr(ObjectV("lowercase" -> StringV(value)))

  /**
   * A LTrim expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def LTrim(value: Expr) : Expr =
    Expr(ObjectV("ltrim" -> value.value ))

  /**
    * A LTrim expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def LTrim(value: String) : Expr =
    Expr(ObjectV("ltrim" -> StringV(value)))

  /**
    * A NGram expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def NGram(terms: Expr, min: Expr = NullV, max: Expr = NullV): Expr = {
    val b = Seq.newBuilder[(String, Value)]

    b += "ngram" -> terms.value
    if (min != Expr(NullV)) b += "min" -> min.value
    if (max != Expr(NullV)) b += "max" -> max.value
    Expr(ObjectV(b.result(): _*))
  }

  /**
   * A Repeat expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def Repeat(value: Expr) : Expr =
    Expr(ObjectV("repeat" -> value.value ))

  /**
    * A Repeat expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def Repeat(value: String) : Expr =
    Expr(ObjectV("repeat" -> StringV(value)))

  /**
    * A Repeat expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def Repeat(value: Expr, number: Expr) : Expr =
    Expr(ObjectV("repeat" -> value.value, "number" -> number.value))

  /**
    * A Repeat expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def Repeat(value: String, number: Long) : Expr =
    Expr(ObjectV("repeat" -> StringV(value), "number" -> LongV(number)))

  /**
   * A ReplaceStr expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def ReplaceStr(value: Expr, source: Expr, replace: Expr) : Expr =
    Expr(ObjectV("replacestr" -> value.value, "source" -> source.value, "replace" -> replace.value))

  /**
    * A ReplaceStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStr(value: String, source: Expr, replace: Expr) : Expr =
    Expr(ObjectV("replacestr" -> StringV(value), "source" -> source.value, "replace" -> replace.value))

  /**
    * A ReplaceStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStr(value: Expr, source: String, replace: Expr) : Expr =
    Expr(ObjectV("replacestr" -> value.value, "source" -> StringV(source), "replace" -> replace.value))

  /**
    * A ReplaceStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStr(value: String, source: String, replace: Expr) : Expr =
    Expr(ObjectV("replacestr" -> StringV(value), "source" -> StringV(source), "replace" -> replace.value))

  /**
    * A ReplaceStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStr(value: Expr, source: Expr, replace: String) : Expr =
    Expr(ObjectV("replacestr" -> value.value, "source" -> source.value, "replace" -> StringV(replace)))

  /**
    * A ReplaceStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStr(value: String, source: Expr, replace: String) : Expr =
    Expr(ObjectV("replacestr" -> StringV(value), "source" -> source.value, "replace" -> StringV(replace)))

  /**
    * A ReplaceStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStr(value: Expr, source: String, replace: String) : Expr =
    Expr(ObjectV("replacestr" -> value.value, "source" -> StringV(source), "replace" -> StringV(replace)))

  /**
    * A ReplaceStr expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStr(value: String, source: String, replace: String) : Expr =
    Expr(ObjectV("replacestr" -> StringV(value), "find" -> StringV(source), "replace" -> StringV(replace)))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: Expr, replace: Expr) : Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> pattern.value, "replace" -> replace.value))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: String, pattern: Expr, replace: Expr) : Expr =
    Expr(ObjectV("replacestrregex" -> StringV(value), "pattern" -> pattern.value, "replace" -> replace.value))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: String, replace: Expr) : Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> StringV(pattern), "replace" -> replace.value))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: String, pattern: String, replace: Expr) : Expr =
    Expr(ObjectV("replacestrregex" -> StringV(value), "pattern" -> StringV(pattern), "replace" -> replace.value))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: Expr, replace: String) : Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> pattern.value, "replace" -> StringV(replace)))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: String, pattern: Expr, replace: String) : Expr =
    Expr(ObjectV("replacestrregex" -> StringV(value), "pattern" -> pattern.value, "replace" -> StringV(replace)))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: String, replace: String) : Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> StringV(pattern), "replace" -> StringV(replace)))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: String, pattern: String, replace: String) : Expr =
    Expr(ObjectV("replacestrregex" -> StringV(value), "pattern" -> StringV(pattern), "replace" -> StringV(replace)))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: Expr, replace: Expr, first: Expr) : Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> pattern.value, "replace" -> replace.value, "first" -> first.value))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: String, pattern: Expr, replace: Expr, first: Expr) : Expr =
    Expr(ObjectV("replacestrregex" -> StringV(value), "pattern" -> pattern.value, "replace" -> replace.value, "first" -> first.value))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: String, replace: Expr, first: Expr) : Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> StringV(pattern), "replace" -> replace.value, "first" -> first.value))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: String, pattern: String, replace: Expr, first: Expr) : Expr =
    Expr(ObjectV("replacestrregex" -> StringV(value), "pattern" -> StringV(pattern), "replace" -> replace.value, "first" -> first.value))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: Expr, replace: String, first: Expr) : Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> pattern.value, "replace" -> StringV(replace), "first" -> first.value))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: String, pattern: Expr, replace: String, first: Expr) : Expr =
    Expr(ObjectV("replacestrregex" -> StringV(value), "pattern" -> pattern.value, "replace" -> StringV(replace), "first" -> first.value))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: String, replace: String, first: Expr) : Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> StringV(pattern), "replace" -> StringV(replace), "first" -> first.value))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: String, pattern: String, replace: String, first: Expr) : Expr =
    Expr(ObjectV("replacestrregex" -> StringV(value), "pattern" -> StringV(pattern), "replace" -> StringV(replace), "first" -> first.value))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: Expr, replace: Expr, first: Boolean) : Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> pattern.value, "replace" -> replace.value, "first" -> BooleanV(first)))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: String, pattern: Expr, replace: Expr, first: Boolean) : Expr =
    Expr(ObjectV("replacestrregex" -> StringV(value), "pattern" -> pattern.value, "replace" -> replace.value, "first" -> BooleanV(first)))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: String, replace: Expr, first: Boolean) : Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> StringV(pattern), "replace" -> replace.value, "first" -> BooleanV(first)))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: String, pattern: String, replace: Expr, first: Boolean) : Expr =
    Expr(ObjectV("replacestrregex" -> StringV(value), "pattern" -> StringV(pattern), "replace" -> replace.value, "first" -> BooleanV(first)))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: Expr, replace: String, first: Boolean) : Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> pattern.value, "replace" -> StringV(replace), "first" -> BooleanV(first)))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: String, pattern: Expr, replace: String, first: Boolean) : Expr =
    Expr(ObjectV("replacestrregex" -> StringV(value), "pattern" -> pattern.value, "replace" -> StringV(replace), "first" -> BooleanV(first)))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: String, replace: String, first: Boolean) : Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> StringV(pattern), "replace" -> StringV(replace), "first" -> BooleanV(first)))

  /**
    * A ReplaceStrRegex expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def ReplaceStrRegex(value: String, pattern: String, replace: String, first: Boolean) : Expr =
    Expr(ObjectV("replacestrregex" -> StringV(value), "pattern" -> StringV(pattern), "replace" -> StringV(replace), "first" -> BooleanV(first)))

  /**
    * A RegexSubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def RegexSubString(value: Expr, pattern: Expr) : Expr =
    Expr(ObjectV("segex_substring" -> value.value, "pattern" -> pattern.value))

  /**
    * A RegexSubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def RegexSubString(value: String, pattern: Expr) : Expr =
    Expr(ObjectV("segex_substring" -> StringV(value), "pattern" -> pattern.value))

  /**
    * A RegexSubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def RegexSubString(value: Expr, pattern: String) : Expr =
    Expr(ObjectV("segex_substring" -> value.value, "pattern" -> StringV(pattern)))

  /**
    * A RegexSubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def RegexSubString(value: String, pattern: String) : Expr =
    Expr(ObjectV("segex_substring" -> StringV(value), "pattern" -> StringV(pattern)))


  /**
   * A RTrim expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def RTrim(value: Expr) : Expr =
    Expr(ObjectV("rtrim" -> value.value))

  /**
    * A RTrim expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def RTrim(value: String) : Expr =
    Expr(ObjectV("rtrim" -> StringV(value)))

  /**
   * A Space expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def Space(value: Expr) : Expr =
    Expr(ObjectV("space" -> value.value ))

  /**
    * A Space expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def Space(value: String) : Expr =
    Expr(ObjectV("space" -> StringV(value)))

  /**
    * A SubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def SubString(value: Expr) : Expr =
    Expr(ObjectV("substring" -> value.value))

  /**
    * A SubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def SubString(value: String) : Expr =
    Expr(ObjectV("substring" -> StringV(value)))

  /**
   * A SubString expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def SubString(value: Expr, start: Expr) : Expr =
    Expr(ObjectV("substring" -> value.value, "start" -> start.value))

  /**
    * A SubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def SubString(value: String, start: Expr) : Expr =
    Expr(ObjectV("substring" -> StringV(value), "start" -> start.value))

  /**
    * A SubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def SubString(value: Expr, start: Long) : Expr =
    Expr(ObjectV("substring" -> value.value, "start" -> LongV(start)))

  /**
    * A SubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def SubString(value: String, start: Long) : Expr =
    Expr(ObjectV("substring" -> StringV(value), "start" -> LongV(start)))

  /**
   * A SubString expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def SubString(value: Expr, start: Expr, length: Expr) : Expr =
    Expr(ObjectV("substring" -> value.value, "start" -> start.value, "length" -> length.value))

  /**
    * A SubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def SubString(value: String, start: Expr, length: Expr) : Expr =
    Expr(ObjectV("substring" -> StringV(value), "start" -> start.value, "length" -> length.value))

  /**
    * A SubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def SubString(value: Expr, start: String, length: Expr) : Expr =
    Expr(ObjectV("substring" -> value.value, "start" -> StringV(start), "length" -> length.value))

  /**
    * A SubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def SubString(value: String, start: String, length: Expr) : Expr =
    Expr(ObjectV("substring" -> StringV(value), "start" -> StringV(start), "length" -> length.value))

  /**
    * A SubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def SubString(value: Expr, start: Expr, length: Long) : Expr =
    Expr(ObjectV("substring" -> value.value, "start" -> start.value, "length" -> LongV(length)))

  /**
    * A SubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def SubString(value: String, start: Expr, length: Long) : Expr =
    Expr(ObjectV("substring" -> StringV(value), "start" -> start.value, "length" -> LongV(length)))

  /**
    * A SubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def SubString(value: Expr, start: String, length: Long) : Expr =
    Expr(ObjectV("substring" -> value.value, "start" -> StringV(start), "length" -> LongV(length)))

  /**
    * A SubString expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def SubString(value: String, start: String, length: Long) : Expr =
    Expr(ObjectV("substring" -> StringV(value), "start" -> StringV(start), "length" -> LongV(length)))

  /**
    * A TitleCase expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def TitleCase(value: Expr) : Expr =
    Expr(ObjectV("titlecase" -> value.value ))

  /**
    * A TitleCase expression
    *
    *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
    */
  def TitleCase(value: String) : Expr =
    Expr(ObjectV("titlecase" -> StringV(value)))

  /**
   * A Trim expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def Trim(term: Expr) : Expr =
    Expr(ObjectV("trim" -> term.value ))

  /**
   * A Trim expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def Trim(term: String) : Expr =
    Expr(ObjectV("trim" -> StringV(term)))

  /**
   * A UpperCase expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def UpperCase(term: Expr) : Expr =
    Expr(ObjectV("uppercase" -> term.value ))

  /**
   * A Upper expression
   *
   *''Reference''': [[https://fauna.com/documentation/queries#string-functions]]
   */
  def UpperCase(term: String) : Expr =
    Expr(ObjectV("uppercase" -> StringV(term)))

  // Time Functions

  /**
    * A Time expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#time-and-date]]
    */
  def Time(str: Expr): Expr =
    Expr(ObjectV("time" -> str.value))

  /**
    * An Epoch expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#time-and-date]]
    */
  def Epoch(num: Expr, unit: TimeUnit): Expr =
    Epoch(num, unit.expr)

  def Epoch(num: Expr, unit: Expr): Expr =
    Expr(ObjectV("epoch" -> num.value, "unit" -> unit.value))

  /**
    * A Date expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#time-and-date]]
    */
  def Date(str: Expr): Expr =
    Expr(ObjectV("date" -> str.value))

  // Misc Functions

  /**
    * A Next Id expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#miscellaneous]]
    */
  @deprecated("use NewId instead")
  def NextId(): Expr =
    Expr(ObjectV("next_id" -> NullV))

  /**
    * A New Id expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#miscellaneous]]
    */
  def NewId(): Expr =
    Expr(ObjectV("new_id" -> NullV))

  /**
    * A Class expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#helper-functions]]
    */
  def Class(name: Expr): Expr =
    Expr(ObjectV("class" -> name.value))

  /**
    * A Class expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#helper-functions]]
    */
  def Class(name: Expr, scope: Expr): Expr =
    Expr(ObjectV("class" -> name.value, "scope" -> scope.value))

  /**
    * A Database expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#helper-functions]]
    */
  def Database(name: Expr): Expr =
    Expr(ObjectV("database" -> name.value))

  /**
    * A Database expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#helper-functions]]
    */
  def Database(name: Expr, scope: Expr): Expr =
    Expr(ObjectV("database" -> name.value, "scope" -> scope.value))

  /**
    * An Index expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#helper-functions]]
    */
  def Index(name: Expr): Expr =
    Expr(ObjectV("index" -> name.value))

  /**
    * An Index expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#helper-functions]]
    */
  def Index(name: Expr, scope: Expr): Expr =
    Expr(ObjectV("index" -> name.value, "scope" -> scope.value))

  /**
    * A Function expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#helper-functions]]
    */
  def Function(name: Expr): Expr =
    Expr(ObjectV("function" -> name.value))

  /**
    * A Function expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#helper-functions]]
    */
  def Function(name: Expr, scope: Expr): Expr =
    Expr(ObjectV("function" -> name.value, "scope" -> scope.value))

  /**
   * An Equals expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#miscellaneous]]
   */
  def Equals(terms: Expr*): Expr =
    Expr(ObjectV("equals" -> varargs(terms)))

  /**
   * A Contains expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#miscellaneous]]
   */
  def Contains(path: Expr, in: Expr): Expr =
    Expr(ObjectV("contains" -> path.value, "in" -> in.value))

  /**
   * A Select expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#read-functions]]
   */
  def Select(path: Expr, from: Expr): Expr =
    Expr(ObjectV("select" -> path.value, "from" -> from.value))

  def Select(path: Expr, from: Expr, default: Expr): Expr =
    Expr(ObjectV("select" -> path.value, "from" -> from.value, "default" -> default.value))

  /**
    * A SelectAll expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#read-functions]]
    */
  def SelectAll(path: Expr, from: Expr): Expr =
    Expr(ObjectV("select_all" -> path.value, "from" -> from.value))

  def SelectAll(path: Expr, from: Expr, default: Expr): Expr =
    Expr(ObjectV("select_all" -> path.value, "from" -> from.value, "default" -> default.value))

  /**
   * An Abs expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
   */
  def Abs(num: Expr): Expr =
    Expr(ObjectV("abs" -> num.value))

  /**
    * An Acos expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Acos(num: Expr): Expr =
    Expr(ObjectV("acos" -> num.value))

  /**
   * An Add expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
   */
  def Add(terms: Expr*): Expr =
    Expr(ObjectV("add" -> varargs(terms)))

  /**
    * An Asin expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Asin(num: Expr): Expr =
    Expr(ObjectV("asin" -> num.value))

  /**
    * An Atan expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Atan(num: Expr): Expr =
    Expr(ObjectV("atan" -> num.value))

  /**
    * An BitAnd expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def BitAnd(terms: Expr*): Expr =
    Expr(ObjectV("bitand" -> varargs(terms)))

  /**
    * A BitNot expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def BitNot(num: Expr): Expr =
    Expr(ObjectV("bitnot" -> num.value))

  /**
    * An BitOr expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def BitOr(terms: Expr*): Expr =
    Expr(ObjectV("bitor" -> varargs(terms)))

  /**
    * An BitXor expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def BitXor(terms: Expr*): Expr =
    Expr(ObjectV("bitxor" -> varargs(terms)))

  /**
    * A Ceil expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Ceil(num: Expr): Expr =
    Expr(ObjectV("ceil" -> num.value))

  /**
    * A Cos expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Cos(num: Expr): Expr =
    Expr(ObjectV("cos" -> num.value))

  /**
    * A Cosh expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Cosh(num: Expr): Expr =
    Expr(ObjectV("cosh" -> num.value))

  /**
    * A Degrees expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Degrees(num: Expr): Expr =
    Expr(ObjectV("degrees" -> num.value))

  /**
    * A Divide expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Divide(terms: Expr*): Expr =
    Expr(ObjectV("divide" -> varargs(terms)))

  /**
    * An Exp expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Exp(num: Expr): Expr =
    Expr(ObjectV("exp" -> num.value))

  /**
    * A Floor expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Floor(num: Expr): Expr =
    Expr(ObjectV("floor" -> num.value))

  /**
    * A Hypot expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Hypot(num: Expr): Expr =
    Expr(ObjectV("hypot" -> num.value))

  /**
    * A Hypot expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Hypot(num: Expr, exp: Expr): Expr =
    Expr(ObjectV("hypot" -> num.value, "b" -> exp.value))

  /**
    * A ln expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Ln(num: Expr): Expr =
    Expr(ObjectV("ln" -> num.value))

  /**
    * A Log expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Log(num: Expr): Expr =
    Expr(ObjectV("log" -> num.value))

  /**
    * A Max expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Max(terms: Expr*): Expr =
    Expr(ObjectV("max" -> varargs(terms)))

  /**
    * A Min expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Min(terms: Expr*): Expr =
    Expr(ObjectV("min" -> varargs(terms)))

  /**
    * A Modulo expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Modulo(terms: Expr*): Expr =
    Expr(ObjectV("modulo" -> varargs(terms)))

  /**
   * A Multiply expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
   */
  def Multiply(terms: Expr*): Expr =
    Expr(ObjectV("multiply" -> varargs(terms)))

  /**
    * A Pow expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Pow(num: Expr): Expr =
    Expr(ObjectV("pow" -> num.value))

  /**
    * A Pow expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Pow(num: Expr, exp: Expr): Expr =
    Expr(ObjectV("pow" -> num.value, "exp" -> exp.value))

  /**
    * A Radians expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Radians(num: Expr): Expr =
    Expr(ObjectV("radians" -> num.value))

  /**
    * A Round expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Round(num: Expr): Expr =
    Expr(ObjectV("round" -> num.value))

  /**
    * A Round expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Round(num: Expr, precision: Expr): Expr =
    Expr(ObjectV("round" -> num.value, "precision" -> precision.value))

  /**
    * A sign expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Sign(num: Expr): Expr =
    Expr(ObjectV("sign" -> num.value))

  /**
    * A sin expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Sin(num: Expr): Expr =
    Expr(ObjectV("sin" -> num.value))

  /**
    * A sinh expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Sinh(num: Expr): Expr =
    Expr(ObjectV("sinh" -> num.value))

  /**
    * A sqrt expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Sqrt(num: Expr): Expr =
    Expr(ObjectV("sqrt" -> num.value))

  /**
   * A Subtract expression.
   *
   * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
   */
  def Subtract(terms: Expr*): Expr =
    Expr(ObjectV("subtract" -> varargs(terms)))

  /**
    * A Tan expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Tan(num: Expr): Expr =
    Expr(ObjectV("tan" -> num.value))

  /**
    * A Tanh expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Tanh(num: Expr): Expr =
    Expr(ObjectV("tanh" -> num.value))

  /**
    * A Trunc expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Trunc(num: Expr): Expr =
    Expr(ObjectV("trunc" -> num.value))

  /**
    * A Trunc expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#mathematical-functions]]
    */
  def Trunc(num: Expr, precision: Expr): Expr =
    Expr(ObjectV("trunc" -> num.value, "precision" -> precision.value))

  /**
    * A LT expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#logical-functions]]
    */
  def LT(terms: Expr*): Expr =
    Expr(ObjectV("lt" -> varargs(terms)))

  /**
    * A LTE expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#logical-functions]]
    */
  def LTE(terms: Expr*): Expr =
    Expr(ObjectV("lte" -> varargs(terms)))

  /**
    * A GT expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#logical-functions]]
    */
  def GT(terms: Expr*): Expr =
    Expr(ObjectV("gt" -> varargs(terms)))

  /**
    * A GTE expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#logical-functions]]
    */
  def GTE(terms: Expr*): Expr =
    Expr(ObjectV("gte" -> varargs(terms)))

  /**
    * An And expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#logical-functions]]
    */
  def And(terms: Expr*): Expr =
    Expr(ObjectV("and" -> varargs(terms)))

  /**
    * An Or expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#logical-functions]]
    */
  def Or(terms: Expr*): Expr =
    Expr(ObjectV("or" -> varargs(terms)))

  /**
    * A Not expression.
    *
    * '''Reference''': [[https://fauna.com/documentation/queries#logical-functions]]
    */
  def Not(term: Expr): Expr =
    Expr(ObjectV("not" -> term.value))

  /**
    * Casts an expression to a string value, if possible.
    */
  def ToString(term: Expr): Expr =
    Expr(ObjectV("to_string" -> term.value))

  /**
    * Casts an expression to a numeric value, if possible.
    */
  def ToNumber(term: Expr): Expr =
    Expr(ObjectV("to_number" -> term.value))

  /**
    * Casts an expression to a time value, if possible.
    */
  def ToTime(term: Expr): Expr =
    Expr(ObjectV("to_time" -> term.value))

  /**
    * Casts an expression to a data value, if possible.
    */
  def ToDate(term: Expr): Expr =
    Expr(ObjectV("to_date" -> term.value))
}
