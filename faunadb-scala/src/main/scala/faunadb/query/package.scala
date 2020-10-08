package faunadb

import com.fasterxml.jackson.annotation.JsonValue
import faunadb.values._
import scala.annotation.meta.getter
import scala.language.experimental.macros
import scala.language.implicitConversions

/**
  * Functions modeling the FaunaDB Query language.
  *
  * Documents of these Collections can be composed to model a query expression, which can then be passed to
  * [[FaunaClient!.query(expr:faunadb\.query\.Expr)*]] in order to execute the query.
  *
  * ===Examples===
  *
  * {{{
  * val query = Create(Collection("spells"), Obj("data" -> Obj("name" -> "Magic Missile")))
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
    implicit def encode[T: Encoder](obj: T): Expr = new Expr(wrapValue(obj))

    private def wrapValue(value: Value): Value = value match {
      case ObjectV(fields) => ObjectV("object" -> ObjectV(fields.map { case (k, v) => (k, wrapValue(v)) }))
      case ArrayV(values) => ArrayV(values.map(wrapValue))
      case _ => value
    }

    private[query] def apply(value: Value): Expr = new Expr(value)
  }

  /**
    * Enumeration for time units. Used by [[https://app.fauna.com/documentation/reference/queryapi#time-and-date]].
    */
  sealed abstract class TimeUnit(val expr: Expr)
  object TimeUnit {
    case object Day extends TimeUnit("day")
    case object HalfDay extends TimeUnit("half day")
    case object Hour extends TimeUnit("hour")
    case object Minute extends TimeUnit("minute")
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
    def /(sub: Path): Path = Path(segments ++ sub.segments: _*)
  }

  /**
    * Helper for pagination cursors
    */
  sealed trait Cursor
  object Cursor {
    implicit def exprToCursor(expr: Expr): Cursor =
      RawCursor(expr)
  }
  case class Before(expr: Expr) extends Cursor
  case class After(expr: Expr) extends Cursor
  case class RawCursor(expr: Expr) extends Cursor
  case object NoCursor extends Cursor
}

package object query {

  // implicit conversions

  implicit def strToPath(str: String): Path = Path(Expr(StringV(str)))
  implicit def intToPath(int: Int): Path = Path(Expr(LongV(int.toLong)))
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
    * Creates a RefV value. The string "collections/widget/123" will be equivalent to:
    * {{{
    * RefV("123", RefV("widget", Native.Collections))
    * }}}
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#simple-type]]
    */
  def Ref(value: String): Expr =
    Expr(ObjectV("@ref" -> StringV(value)))

  def Ref(collectionRef: Expr, id: Expr): Expr =
    Expr(ObjectV("ref" -> collectionRef.value, "id" -> id.value))

  /** Native reference to all classes */
  @deprecated("use Collections instead", "2.7.0")
  def Classes(scope: Expr = NullV): Expr =
    Expr(ObjectV("classes" -> scope.value))

    /** Native reference to all classes */
  def Collections(scope: Expr = NullV): Expr =
    Expr(ObjectV("collections" -> scope.value))

  /** Native reference to all databases */
  def Databases(scope: Expr = NullV): Expr =
    Expr(ObjectV("databases" -> scope.value))

  /** Native reference to all indexes */
  def Indexes(scope: Expr = NullV): Expr =
    Expr(ObjectV("indexes" -> scope.value))

  /** Native reference to all functions */
  def Functions(scope: Expr = NullV): Expr =
    Expr(ObjectV("functions" -> scope.value))

  /** Native reference to all roles */
  def Roles(scope: Expr = NullV): Expr =
    Expr(ObjectV("roles" -> scope.value))

  /** Native reference to all keys */
  def Keys(scope: Expr = NullV): Expr =
    Expr(ObjectV("keys" -> scope.value))

  /** Native reference to all tokens */
  def Tokens(scope: Expr = NullV): Expr =
    Expr(ObjectV("tokens" -> scope.value))

  /** Native reference to all credentials */
  def Credentials(scope: Expr = NullV): Expr =
    Expr(ObjectV("credentials" -> scope.value))

  /** Returns a set of all documents in the given collection */
  def Documents(collection: Expr): Expr =
    Expr(ObjectV("documents" -> collection.value))

  /**
    * An Array value.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#simple-type]]
    */
  def Arr(elems: Expr*): Expr =
    Expr(ArrayV(unwrap(elems): _*))

  /**
    * An Object value.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#simple-type]]
    */
  def Obj(pairs: (String, Expr)*): Expr =
    Expr(ObjectV("object" -> ObjectV(unwrapPairs(pairs): _*)))

  /**
    * A Null value.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#simple-type]]
    */
  def Null(): Expr =
    Expr(NullV)

  // Basic Forms

  /**
    * A Abort expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#basic-forms]]
    */
  def Abort(msg: Expr): Expr =
    Expr(ObjectV("abort" -> msg.value))

  /**
    * A Call expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#basic-forms]]
    */
  def Call(ref: Expr, arguments: Expr*): Expr =
    Expr(ObjectV("call" -> ref.value, "arguments" -> varargs(arguments)))

  /**
    * A Query expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#basic-forms]]
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

  def Query(lambda: Expr): Expr =
    Expr(ObjectV("query" -> lambda.value))

  /**
    * A At expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#basic-forms]]
    */
  def At(timestamp: Expr, expr: Expr): Expr =
    Expr(ObjectV("at" -> timestamp.value, "expr" -> expr.value))

  /**
    * A Let expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#basic-forms]]
    */

  def Let(block: => Any): Expr = macro QueryMacros.let

  def Let(bindings: Seq[(String, Expr)], in: Expr): Expr =
    Expr(ObjectV("let" -> ArrayV(unwrapPairs(bindings) map { ObjectV(_) }: _*), "in" -> in.value))

  /**
    * A Var expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#basic-forms]]
    */
  def Var(name: String): Expr =
    Expr(ObjectV("var" -> StringV(name)))

  /**
   * An If expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#basic-forms]]
   */
  def If(pred: Expr, `then`: Expr, `else`: Expr): Expr =
    Expr(ObjectV("if" -> pred.value, "then" -> `then`.value, "else" -> `else`.value))

  /**
   * A Do expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#basic-forms]]
   */
  def Do(exprs: Expr*): Expr =
    Expr(ObjectV("do" -> ArrayV(unwrap(exprs): _*)))

  /**
   * A Lambda expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#basic-forms]]
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
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#collections]]
   */
  def Map(collection: Expr, lambda: Expr): Expr =
    Expr(ObjectV("map" -> lambda.value, "collection" -> collection.value))

  /**
   * A Foreach expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#collections]]
   */
  def Foreach(collection: Expr, lambda: Expr): Expr =
    Expr(ObjectV("foreach" -> lambda.value, "collection" -> collection.value))

  /**
    * A Filter expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#collections]]
    */
  def Filter(collection: Expr, lambda: Expr): Expr =
    Expr(ObjectV("filter" -> lambda.value, "collection" -> collection.value))

  /**
    * A Prepend expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#collections]]
    */
  def Prepend(elems: Expr, collection: Expr): Expr =
    Expr(ObjectV("prepend" -> elems.value, "collection" -> collection.value))

  /**
    * An Append expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#collections]]
    */
  def Append(elems: Expr, collection: Expr): Expr =
    Expr(ObjectV("append" -> elems.value, "collection" -> collection.value))

  /**
    * A Take expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#collections]]
    */
  def Take(num: Expr, collection: Expr): Expr =
    Expr(ObjectV("take" -> num.value, "collection" -> collection.value))

  /**
    * A Drop expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#collections]]
    */
  def Drop(num: Expr, collection: Expr): Expr =
    Expr(ObjectV("drop" -> num.value, "collection" -> collection.value))

  /**
    * A IsEmpty expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#collections]]
    */
  def IsEmpty(collection: Expr): Expr =
    Expr(ObjectV("is_empty" -> collection.value))

  /**
    * A IsNonEmpty expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#collections]]
    */
  def IsNonEmpty(collection: Expr): Expr =
    Expr(ObjectV("is_nonempty" -> collection.value))

  // Read Functions

  /**
   * A Get expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#read-functions]]
   */
  def Get(resource: Expr): Expr =
    Expr(ObjectV("get" -> resource.value))

  def Get(resource: Expr, ts: Expr): Expr =
    Expr(ObjectV("get" -> resource.value, "ts" -> ts.value))

  /**
    * A KeyFromSecret expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#read-functions]]
    */
  def KeyFromSecret(secret: Expr): Expr =
    Expr(ObjectV("key_from_secret" -> secret.value))

  /**
    * A Reduce expression.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/reduce]]
    */
  def Reduce(lambda: Expr, initial: Expr, collection: Expr): Expr =
    Expr(ObjectV("reduce" -> lambda.value, "initial" -> initial.value, "collection" -> collection.value))

  /**
    * A Reverse expression.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/reverse]]
    */
  def Reverse(source: Expr): Expr =
    Expr(ObjectV("reverse" -> source.value))

  /**
   * A Paginate expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#read-functions]]
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
      case b: Before    => call += "before" -> b.expr.value
      case a: After     => call += "after" -> a.expr.value
      case r: RawCursor => call += "cursor" -> r.expr.value
      case NoCursor     => ()
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
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#read-functions]]
   */
  def Exists(ref: Expr): Expr =
    Expr(ObjectV("exists" -> ref.value))

  def Exists(ref: Expr, ts: Expr): Expr =
    Expr(ObjectV("exists" -> ref.value, "ts" -> ts.value))

  // Write Functions

  /**
   * A Create expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
   */
  def Create(ref: Expr, params: Expr): Expr =
    Expr(ObjectV("create" -> ref.value, "params" -> params.value))

  /**
   * An Update expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
   */
  def Update(ref: Expr, params: Expr): Expr =
    Expr(ObjectV("update" -> ref.value, "params" -> params.value))

  /**
   * A Replace expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
   */
  def Replace(ref: Expr, params: Expr): Expr =
    Expr(ObjectV("replace" -> ref.value, "params" -> params.value))

  /**
   * A Delete expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
   */
  def Delete(ref: Expr): Expr =
    Expr(ObjectV("delete" -> ref.value))

  /**
    * An Insert expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
    */
  def Insert(ref: Expr, ts: Expr, action: Action, params: Expr): Expr =
    Insert(ref, ts, action.expr, params)

  def Insert(ref: Expr, ts: Expr, action: Expr, params: Expr): Expr =
    Expr(ObjectV("insert" -> ref.value, "ts" -> ts.value, "action" -> action.value, "params" -> params.value))

  /**
    * A Remove expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
    */
  def Remove(ref: Expr, ts: Expr, action: Action): Expr =
    Remove(ref, ts, action.expr)

  def Remove(ref: Expr, ts: Expr, action: Expr): Expr =
    Expr(ObjectV("remove" -> ref.value, "ts" -> ts.value, "action" -> action.value))

  /**
    * A Create Class expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
    */
  @deprecated("use CreateCollection instead", "2.7.0")
  def CreateClass(params: Expr): Expr =
    Expr(ObjectV("create_class" -> params.value))

  /**
    * A Create Collection expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
    */
  def CreateCollection(params: Expr): Expr =
    Expr(ObjectV("create_collection" -> params.value))

  /**
    * A Create Database expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
    */
  def CreateDatabase(params: Expr): Expr =
    Expr(ObjectV("create_database" -> params.value))

  /**
    * A Create Key expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
    */
  def CreateKey(params: Expr): Expr =
    Expr(ObjectV("create_key" -> params.value))

  /**
    * A Create Index expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
    */
  def CreateIndex(params: Expr): Expr =
    Expr(ObjectV("create_index" -> params.value))

  /**
    * A Create Function expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
    */
  def CreateFunction(params: Expr): Expr =
    Expr(ObjectV("create_function" -> params.value))

  /**
    * A Create Role expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
    */
  def CreateRole(params: Expr): Expr =
    Expr(ObjectV("create_role" -> params.value))

  /**
    * A Move Database expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#write-functions]]
    */
  def MoveDatabase(from: Expr, to: Expr): Expr =
    Expr(ObjectV("move_database" -> from.value, "to" -> to.value))

  // Set Constructors

  /**
    * A Singleton set.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#sets]]
    */
  def Singleton(ref: Expr): Expr =
    Expr(ObjectV("singleton" -> ref.value))

  /**
    * A Events set.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#sets]]
    */
  def Events(refSet: Expr): Expr =
    Expr(ObjectV("events" -> refSet.value))

  /**
   * A Match set.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#sets]]
   */
  def Match(index: Expr, terms: Expr*): Expr =
    Expr(ObjectV("match" -> varargs(terms), "index" -> index.value))

  /**
   * A Union set.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#sets]]
   */
  def Union(sets: Expr*): Expr =
    Expr(ObjectV("union" -> varargs(sets)))

  /**
   * An Intersection set.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#sets]]
   */
  def Intersection(sets: Expr*): Expr =
    Expr(ObjectV("intersection" -> varargs(sets)))

  /**
   * A Difference set.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#sets]]
   */
  def Difference(sets: Expr*): Expr =
    Expr(ObjectV("difference" -> varargs(sets)))

  /**
   * A Distinct set.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#sets]]
   */
  def Distinct(set: Expr): Expr =
    Expr(ObjectV("distinct" -> set.value))

  /**
   * A Join set.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#sets]]
   */
  def Join(source: Expr, `with`: Expr): Expr =
    Expr(ObjectV("join" -> source.value, "with" -> `with`.value))

  /**
   * Filter the set based on the lower/upper bounds (inclusive).
   *
   * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/range]]
   *
   * @param set set to be filtered
   * @param from lower bound
   * @param to upper bound
   */
  def Range(set: Expr, from: Expr, to: Expr): Expr =
    Expr(ObjectV("range" -> set.value, "from" -> from.value, "to" -> to.value))

  // Authentication Functions

  /**
    * A Login expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#authentication]]
    */
  def Login(ref: Expr, params: Expr): Expr =
    Expr(ObjectV("login" -> ref.value, "params" -> params.value))

  /**
    * A Logout expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#authentication]]
    */
  def Logout(invalidateAll: Expr): Expr =
    Expr(ObjectV("logout" -> invalidateAll.value))

  /**
    * An Identify expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#authentication]]
    */
  def Identify(ref: Expr, password: Expr): Expr =
    Expr(ObjectV("identify" -> ref.value, "password" -> password.value))

  /**
    * An Identity expression.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/identity]]
    */
  @deprecated("use CurrentIdentity instead", "4.0.0")
  def Identity(): Expr =
    Expr(ObjectV("identity" -> NullV))

  /**
    * A CurrentIdentity expression.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/currentidentity]]
    */
  def CurrentIdentity(): Expr =
    Expr(ObjectV("current_identity" -> NullV))

  /**
    * A HasIdentity expression.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/hasidentity]]
    */
  @deprecated("use HasCurrentIdentity instead", "4.0.0")
  def HasIdentity(): Expr =
    Expr(ObjectV("has_identity" -> NullV))

  /**
    * An CreateAccessProvider expression.
    *
    * @param params An object of parameters used to create a new access provider.
    *  - name: A valid schema name
    *  - issuer: A unique string
    *  - jwks_uri: A valid HTTPS URI
    *  - roles: An array of role/predicate pairs where the predicate returns a boolean.
    *                The array can also contain Role references.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/createaccessprovider]]
    */
  def CreateAccessProvider(params: Expr): Expr =
    Expr(ObjectV("create_access_provider" -> params.value))

  /** Native reference to all access providers */
  def AccessProviders(scope: Expr = NullV): Expr =
    Expr(ObjectV("access_providers" -> scope.value))

  /**
    * An Access Provider expression.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/accessprovider]]
    */
  def AccessProvider(name: Expr): Expr =
     Expr(ObjectV("access_provider" -> name.value))

  /**
    * An Access Provider expression.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/accessprovider]]
    */
  def AccessProvider(name: Expr, scope: Expr): Expr =
    Expr(ObjectV("access_provider" -> name.value, "scope" -> scope.value))


  /**
    * An HasCurrentIdentity expression.
    * A HasCurrentToken expression.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/hascurrenttoken]]
    */
  def HasCurrentToken(): Expr =
    Expr(ObjectV("has_current_token" -> NullV))

  /**
    * A CurrentToken expression.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/currenttoken]]
    */
  def CurrentToken(): Expr =
    Expr(ObjectV("current_token" -> NullV))

  /**
    * A HasCurrentIdentity expression.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/hascurrentidentity]]
    */
  def HasCurrentIdentity(): Expr =
    Expr(ObjectV("has_current_identity" -> NullV))


  // String Functions

  /**
   * A Concat expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def Concat(term: Expr): Expr =
    Expr(ObjectV("concat" -> term.value))

  def Concat(term: Expr, separator: Expr): Expr =
    Expr(ObjectV("concat" -> term.value, "separator" -> separator.value))

  /**
   * A Casefold expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def Casefold(term: Expr): Expr =
    Expr(ObjectV("casefold" -> term.value))

  def Casefold(term: Expr, normalizer: Normalizer): Expr =
    Casefold(term, normalizer.expr)

  def Casefold(term: Expr, normalizer: Expr): Expr =
    Expr(ObjectV("casefold" -> term.value, "normalizer" -> normalizer.value))

  /**
    * A ContainsStr expression
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/containsstr]]
    */
  def ContainsStr(value: Expr, search: Expr): Expr =
    Expr(ObjectV("containsstr" -> value.value, "search" -> search.value))

  /**
    * A ContainsStrRegex expression
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/containsstrregex]]
    */
  def ContainsStrRegex(value: Expr, pattern: Expr): Expr =
    Expr(ObjectV("containsstrregex" -> value.value, "pattern" -> pattern.value))

  /**
    * An EndsWith expression
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/endswith]]
    */
  def EndsWith(value: Expr, search: Expr): Expr =
    Expr(ObjectV("endswith" -> value.value, "search" -> search.value))

  /**
   * A FindStr expression
   *
   *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def FindStr(value: Expr, find: Expr): Expr =
    Expr(ObjectV("findstr" -> value.value, "find" -> find.value))

  def FindStr(value: Expr, find: Expr, start: Expr): Expr =
    Expr(ObjectV("findstr" -> value.value, "find" -> find.value, "start" -> start.value))

  /**
    * A FindStrRegex expression
    *
    *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: Expr, start: Expr): Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> pattern.value, "start" -> start.value))

  /**
    * A FindStrRegex expression
    *
    *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
    */
  def FindStrRegex(value: Expr, pattern: Expr, start: Expr, num_results: Expr): Expr =
    Expr(ObjectV("findstrregex" -> value.value, "pattern" -> pattern.value, "start" -> start.value, "num_results" -> num_results.value))

  /**
   * A Length expression
   *
   *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def Length(value: Expr): Expr =
    Expr(ObjectV("length" -> value.value ))

  /**
   * A LowerCase expression
   *
   *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def LowerCase(value: Expr): Expr =
    Expr(ObjectV("lowercase" -> value.value))

  /**
   * A LTrim expression
   *
   *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def LTrim(value: Expr): Expr =
    Expr(ObjectV("ltrim" -> value.value ))

  /**
    * A NGram expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
    */
  def NGram(terms: Expr, min: Expr = NullV, max: Expr = NullV): Expr = {
    val b = Seq.newBuilder[(String, Value)]

    b += "ngram" -> terms.value
    if (min != Expr(NullV)) b += "min" -> min.value
    if (max != Expr(NullV)) b += "max" -> max.value
    Expr(ObjectV(b.result(): _*))
  }

  /**
    * A SplitStr expression
    *
    *'''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/splitstr]]
    */
  def SplitStr(value: Expr, token: Expr, count: Expr): Expr =
    Expr(ObjectV("split_str" -> value.value, "token" -> token.value, "count" -> count.value))

  /**
    * A SplitStr expression
    *
    *'''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/splitstr]]
    */
  def SplitStr(value: Expr, token: Expr): Expr =
    Expr(ObjectV("split_str" -> value.value, "token" -> token.value))

  /**
    * A SplitStrRegex expression
    *
    *'''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/splitstrregex]]
    */
  def SplitStrRegex(value: Expr, pattern: Expr, count: Expr): Expr =
    Expr(ObjectV("split_str_regex" -> value.value, "pattern" -> pattern.value, "count" -> count.value))

  /**
    * A SplitStrRegex expression
    *
    *'''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/splitstrregex]]
    */
  def SplitStrRegex(value: Expr, pattern: Expr): Expr =
    Expr(ObjectV("split_str_regex" -> value.value, "pattern" -> pattern.value))

  /**
    * A RegexEscape expression
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/regexescape]]
    */
  def RegexEscape(value: Expr): Expr =
    Expr(ObjectV("regexescape" -> value.value))

  /**
   * A Repeat expression
   *
   *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def Repeat(value: Expr): Expr =
    Expr(ObjectV("repeat" -> value.value ))

  /**
    * A Repeat expression
    *
    *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
    */
  def Repeat(value: Expr, number: Expr): Expr =
    Expr(ObjectV("repeat" -> value.value, "number" -> number.value))

  /**
   * A ReplaceStr expression
   *
   *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def ReplaceStr(value: Expr, find: Expr, replace: Expr): Expr =
    Expr(ObjectV("replacestr" -> value.value, "find" -> find.value, "replace" -> replace.value))

  /**
    * A ReplaceStrRegex expression
    *
    *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: Expr, replace: Expr): Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> pattern.value, "replace" -> replace.value))

  /**
    * A ReplaceStrRegex expression
    *
    *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
    */
  def ReplaceStrRegex(value: Expr, pattern: Expr, replace: Expr, first: Expr): Expr =
    Expr(ObjectV("replacestrregex" -> value.value, "pattern" -> pattern.value, "replace" -> replace.value, "first" -> first.value))

  /**
   * A RTrim expression
   *
   *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def RTrim(value: Expr): Expr =
    Expr(ObjectV("rtrim" -> value.value))

  /**
   * A Space expression
   *
   *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def Space(value: Expr): Expr =
    Expr(ObjectV("space" -> value.value ))

  /**
    * A StartsWith expression
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/startswith]]
    */
  def StartsWith(value: Expr, search: Expr): Expr =
    Expr(ObjectV("startswith" -> value.value, "search" -> search.value))

  /**
    * A SubString expression
    *
    *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
    */
  def SubString(value: Expr): Expr =
    Expr(ObjectV("substring" -> value.value))

  /**
   * A SubString expression
   *
   *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def SubString(value: Expr, start: Expr): Expr =
    Expr(ObjectV("substring" -> value.value, "start" -> start.value))

  /**
   * A SubString expression
   *
   *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def SubString(value: Expr, start: Expr, length: Expr): Expr =
    Expr(ObjectV("substring" -> value.value, "start" -> start.value, "length" -> length.value))

  /**
    * A TitleCase expression
    *
    *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
    */
  def TitleCase(value: Expr): Expr =
    Expr(ObjectV("titlecase" -> value.value ))

  /**
   * A Trim expression
   *
   *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def Trim(term: Expr): Expr =
    Expr(ObjectV("trim" -> term.value ))

  /**
   * A UpperCase expression
   *
   *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
   */
  def UpperCase(term: Expr): Expr =
    Expr(ObjectV("uppercase" -> term.value ))

  /**
    * A Format expression
    *
    *'''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#string-functions]]
    */
  def Format(format: Expr, values: Expr*): Expr =
    Expr(ObjectV("format" -> format.value, "values" -> varargs(values)))

  // Time Functions

  /**
    * A Time expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-and-date]]
    */
  def Time(str: Expr): Expr =
    Expr(ObjectV("time" -> str.value))

  /**
    * An Epoch expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-and-date]]
    */
  def Epoch(num: Expr, unit: TimeUnit): Expr =
    Epoch(num, unit.expr)

  def Epoch(num: Expr, unit: Expr): Expr =
    Expr(ObjectV("epoch" -> num.value, "unit" -> unit.value))

  /**
    * A TimeAdd expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-and-date]]
    */
  def TimeAdd(base: Expr, offset: Expr, unit: Expr): Expr =
    Expr(ObjectV("time_add" -> base.value, "offset" -> offset.value, "unit" -> unit.value))

  def TimeAdd(base: Expr, offset: Expr, unit: TimeUnit): Expr =
    TimeAdd(base, offset, unit.expr)

  /**
    * A TimeSubtract expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-and-date]]
    */
  def TimeSubtract(base: Expr, offset: Expr, unit: Expr): Expr =
    Expr(ObjectV("time_subtract" -> base.value, "offset" -> offset.value, "unit" -> unit.value))

  def TimeSubtract(base: Expr, offset: Expr, unit: TimeUnit): Expr =
    TimeSubtract(base, offset, unit.expr)

  /**
    * A TimeDiff expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-and-date]]
    */
  def TimeDiff(start: Expr, finish: Expr, unit: Expr): Expr =
    Expr(ObjectV("time_diff" -> start.value, "other" -> finish.value, "unit" -> unit.value))

  def TimeDiff(start: Expr, finish: Expr, unit: TimeUnit): Expr =
    TimeDiff(start, finish, unit.expr)

  /**
    * A Date expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-and-date]]
    */
  def Date(str: Expr): Expr =
    Expr(ObjectV("date" -> str.value))

  /**
    * Returns the current snapshot time.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/now]]
   */
  def Now(): Expr =
    Expr(ObjectV("now" -> NullV))

  // Misc Functions

  /**
    * A Next Id expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  @deprecated("use NewId instead", "2.7.0")
  def NextId(): Expr =
    Expr(ObjectV("next_id" -> NullV))

  /**
    * A New Id expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  def NewId(): Expr =
    Expr(ObjectV("new_id" -> NullV))

  /**
    * A Class expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  @deprecated("use Collection instead", "2.7.0")
  def Class(name: Expr): Expr =
    Expr(ObjectV("class" -> name.value))

  /**
    * A Class expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  @deprecated("use Collection instead", "2.7.0")
  def Class(name: Expr, scope: Expr): Expr =
    Expr(ObjectV("class" -> name.value, "scope" -> scope.value))
  
  /**
    * A Collection expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  def Collection(name: Expr): Expr =
    Expr(ObjectV("collection" -> name.value))

  /**
    * A Collection expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  def Collection(name: Expr, scope: Expr): Expr =
    Expr(ObjectV("collection" -> name.value, "scope" -> scope.value))  

  /**
    * A Database expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  def Database(name: Expr): Expr =
    Expr(ObjectV("database" -> name.value))

  /**
    * A Database expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  def Database(name: Expr, scope: Expr): Expr =
    Expr(ObjectV("database" -> name.value, "scope" -> scope.value))

  /**
    * An Index expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  def Index(name: Expr): Expr =
    Expr(ObjectV("index" -> name.value))

  /**
    * An Index expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  def Index(name: Expr, scope: Expr): Expr =
    Expr(ObjectV("index" -> name.value, "scope" -> scope.value))

  /**
    * A Function expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  def Function(name: Expr): Expr =
    Expr(ObjectV("function" -> name.value))

  /**
    * A Function expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  def Function(name: Expr, scope: Expr): Expr =
    Expr(ObjectV("function" -> name.value, "scope" -> scope.value))

  /**
    * A Role expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  def Role(name: Expr): Expr =
    Expr(ObjectV("role" -> name.value))

  /**
    * A Role expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
    */
  def Role(name: Expr, scope: Expr): Expr =
    Expr(ObjectV("role" -> name.value, "scope" -> scope.value))

  /**
   * An Equals expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
   */
  def Equals(terms: Expr*): Expr =
    Expr(ObjectV("equals" -> varargs(terms)))

  /**
   * A Contains expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#miscellaneous-functions]]
   */
  @deprecated("use ContainsPath instead", "3.0.0")
  def Contains(path: Expr, in: Expr): Expr =
    Expr(ObjectV("contains" -> path.value, "in" -> in.value))

  /**
   * A ContainsField expression.
   *
   * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/containsfield]]
   */
  def ContainsField(field: Expr, in: Expr): Expr =
    Expr(ObjectV("contains_field" -> field.value, "in" -> in.value))

  /**
   * A ContainsPath expression.
   *
   * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/containspath]]
   */
  def ContainsPath(path: Expr, in: Expr): Expr =
    Expr(ObjectV("contains_path" -> path.value, "in" -> in.value))

  /**
   * A ContainsValue expression.
   *
   * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/containsvalue]]
   */
  def ContainsValue(value: Expr, in: Expr): Expr =
    Expr(ObjectV("contains_value" -> value.value, "in" -> in.value))

  /**
   * A Select expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#read-functions]]
   */
  def Select(path: Expr, from: Expr): Expr =
    Expr(ObjectV("select" -> path.value, "from" -> from.value))

  def Select(path: Expr, from: Expr, default: Expr): Expr =
    Expr(ObjectV("select" -> path.value, "from" -> from.value, "default" -> default.value))

  /**
    * A SelectAll expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#read-functions]]
    */
  @deprecated("use SelectAsIndex instead", "2.10.0")
  def SelectAll(path: Expr, from: Expr): Expr =
    Expr(ObjectV("select_all" -> path.value, "from" -> from.value))

    /**
    * A SelectAsIndex expression.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/selectasindex]]
    */
  def SelectAsIndex(path: Expr, from: Expr): Expr =
    Expr(ObjectV("select_as_index" -> path.value, "from" -> from.value))

  /**
   * An Abs expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
   */
  def Abs(num: Expr): Expr =
    Expr(ObjectV("abs" -> num.value))

  /**
    * An Acos expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Acos(num: Expr): Expr =
    Expr(ObjectV("acos" -> num.value))

  /**
   * An Add expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
   */
  def Add(terms: Expr*): Expr =
    Expr(ObjectV("add" -> varargs(terms)))

  /**
    * An Asin expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Asin(num: Expr): Expr =
    Expr(ObjectV("asin" -> num.value))

  /**
    * An Atan expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Atan(num: Expr): Expr =
    Expr(ObjectV("atan" -> num.value))

  /**
    * An BitAnd expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def BitAnd(terms: Expr*): Expr =
    Expr(ObjectV("bitand" -> varargs(terms)))

  /**
    * A BitNot expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def BitNot(num: Expr): Expr =
    Expr(ObjectV("bitnot" -> num.value))

  /**
    * An BitOr expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def BitOr(terms: Expr*): Expr =
    Expr(ObjectV("bitor" -> varargs(terms)))

  /**
    * An BitXor expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def BitXor(terms: Expr*): Expr =
    Expr(ObjectV("bitxor" -> varargs(terms)))

  /**
    * A Ceil expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Ceil(num: Expr): Expr =
    Expr(ObjectV("ceil" -> num.value))

  /**
    * A Cos expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Cos(num: Expr): Expr =
    Expr(ObjectV("cos" -> num.value))

  /**
    * A Cosh expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Cosh(num: Expr): Expr =
    Expr(ObjectV("cosh" -> num.value))

  /**
    * A Degrees expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Degrees(num: Expr): Expr =
    Expr(ObjectV("degrees" -> num.value))

  /**
    * A Divide expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Divide(terms: Expr*): Expr =
    Expr(ObjectV("divide" -> varargs(terms)))

  /**
    * An Exp expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Exp(num: Expr): Expr =
    Expr(ObjectV("exp" -> num.value))

  /**
    * A Floor expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Floor(num: Expr): Expr =
    Expr(ObjectV("floor" -> num.value))

  /**
    * A Hypot expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Hypot(num: Expr): Expr =
    Expr(ObjectV("hypot" -> num.value))

  /**
    * A Hypot expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Hypot(num: Expr, exp: Expr): Expr =
    Expr(ObjectV("hypot" -> num.value, "b" -> exp.value))

  /**
    * A ln expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Ln(num: Expr): Expr =
    Expr(ObjectV("ln" -> num.value))

  /**
    * A Log expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Log(num: Expr): Expr =
    Expr(ObjectV("log" -> num.value))

  /**
    * A Max expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Max(terms: Expr*): Expr =
    Expr(ObjectV("max" -> varargs(terms)))

  /**
    * A Min expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Min(terms: Expr*): Expr =
    Expr(ObjectV("min" -> varargs(terms)))

  /**
    * A Modulo expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Modulo(terms: Expr*): Expr =
    Expr(ObjectV("modulo" -> varargs(terms)))

  /**
   * A Multiply expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
   */
  def Multiply(terms: Expr*): Expr =
    Expr(ObjectV("multiply" -> varargs(terms)))

  /**
    * A Pow expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Pow(num: Expr): Expr =
    Expr(ObjectV("pow" -> num.value))

  /**
    * A Pow expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Pow(num: Expr, exp: Expr): Expr =
    Expr(ObjectV("pow" -> num.value, "exp" -> exp.value))

  /**
    * A Radians expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Radians(num: Expr): Expr =
    Expr(ObjectV("radians" -> num.value))

  /**
    * A Round expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Round(num: Expr): Expr =
    Expr(ObjectV("round" -> num.value))

  /**
    * A Round expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Round(num: Expr, precision: Expr): Expr =
    Expr(ObjectV("round" -> num.value, "precision" -> precision.value))

  /**
    * A sign expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Sign(num: Expr): Expr =
    Expr(ObjectV("sign" -> num.value))

  /**
    * A sin expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Sin(num: Expr): Expr =
    Expr(ObjectV("sin" -> num.value))

  /**
    * A sinh expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Sinh(num: Expr): Expr =
    Expr(ObjectV("sinh" -> num.value))

  /**
    * A sqrt expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Sqrt(num: Expr): Expr =
    Expr(ObjectV("sqrt" -> num.value))

  /**
   * A Subtract expression.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
   */
  def Subtract(terms: Expr*): Expr =
    Expr(ObjectV("subtract" -> varargs(terms)))

  /**
    * A Tan expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Tan(num: Expr): Expr =
    Expr(ObjectV("tan" -> num.value))

  /**
    * A Tanh expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Tanh(num: Expr): Expr =
    Expr(ObjectV("tanh" -> num.value))

  /**
    * A Trunc expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Trunc(num: Expr): Expr =
    Expr(ObjectV("trunc" -> num.value))

  /**
    * A Trunc expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#mathematical-functions]]
    */
  def Trunc(num: Expr, precision: Expr): Expr =
    Expr(ObjectV("trunc" -> num.value, "precision" -> precision.value))

  /**
    * Count the number of elements in the collection.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/count]]
    */
  def Count(collection: Expr): Expr =
    Expr(ObjectV("count" -> collection.value))

  /**
    * Sum the elements in the collection.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/sum]]
    */
  def Sum(collection: Expr): Expr =
    Expr(ObjectV("sum" -> collection.value))

  /**
    * Returns the mean of all elements in the collection.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/mean]]
    */
  def Mean(collection: Expr): Expr =
    Expr(ObjectV("mean" -> collection.value))

  /**
    * Evaluates to true if all elements of the collection is true.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/all]]
    */
  def All(collection: Expr): Expr =
    Expr(ObjectV("all" -> collection.value))

  /**
    * Evaluates to true if any element of the collection is true.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/any]]
    */
  def Any(collection: Expr): Expr =
    Expr(ObjectV("any" -> collection.value))

  /**
    * A LT expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#logical-functions]]
    */
  def LT(terms: Expr*): Expr =
    Expr(ObjectV("lt" -> varargs(terms)))

  /**
    * A LTE expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#logical-functions]]
    */
  def LTE(terms: Expr*): Expr =
    Expr(ObjectV("lte" -> varargs(terms)))

  /**
    * A GT expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#logical-functions]]
    */
  def GT(terms: Expr*): Expr =
    Expr(ObjectV("gt" -> varargs(terms)))

  /**
    * A GTE expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#logical-functions]]
    */
  def GTE(terms: Expr*): Expr =
    Expr(ObjectV("gte" -> varargs(terms)))

  /**
    * An And expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#logical-functions]]
    */
  def And(terms: Expr*): Expr =
    Expr(ObjectV("and" -> varargs(terms)))

  /**
    * An Or expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#logical-functions]]
    */
  def Or(terms: Expr*): Expr =
    Expr(ObjectV("or" -> varargs(terms)))

  /**
    * A Not expression.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#logical-functions]]
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
    * Casts an expression to a double value, if possible.
    */
  def ToDouble(term: Expr): Expr =
    Expr(ObjectV("to_double" -> term.value))

  /**
    * Casts an expression to an integer value, if possible.
    */
  def ToInteger(term: Expr): Expr =
    Expr(ObjectV("to_integer" -> term.value))

  /**
    * Casts an expression to a time value, if possible.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
    */
  def ToTime(term: Expr): Expr =
    Expr(ObjectV("to_time" -> term.value))

  /**
   * Converts a time expression to seconds since the UNIX epoch.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
   */ 
  def ToSeconds(term: Expr): Expr =
    Expr(ObjectV("to_seconds" -> term.value))

  /**
   * Converts a time expression to milliseconds since the UNIX epoch.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
   */
  def ToMillis(term: Expr): Expr =
    Expr(ObjectV("to_millis" -> term.value))

  /**
   * Converts a time expression to microseconds since the UNIX epoch.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
   */ 
  def ToMicros(term: Expr): Expr =
    Expr(ObjectV("to_micros" -> term.value))

  /**
   * Returns a time expression's day of the month, from 1 to 31.
   *
   * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
   */ 
  def DayOfMonth(term: Expr): Expr =
    Expr(ObjectV("day_of_month" -> term.value))

  /**
    * Returns a time expression's day of the week following ISO-8601 convention, from 1 (Monday) to 7 (Sunday).
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
    */ 
  def DayOfWeek(term: Expr): Expr =
    Expr(ObjectV("day_of_week" -> term.value))

  /**
    * Returns a time expression's day of the year, from 1 to 365, or 366 in a leap year.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
    */
  def DayOfYear(term: Expr): Expr =
    Expr(ObjectV("day_of_year" -> term.value))

  /**
    * Returns the time expression's year, following the ISO-8601 standard.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
    */ 
  def Year(term: Expr): Expr =
    Expr(ObjectV("year" -> term.value))

  /**
    * Returns a time expression's month of the year, from 1 to 12.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
    */ 
  def Month(term: Expr): Expr =
    Expr(ObjectV("month" -> term.value))
  
  /**
    * Returns a time expression's hour of the day, from 0 to 23.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
    */   
  def Hour(term: Expr): Expr =
    Expr(ObjectV("hour" -> term.value))

  /**
    * Returns a time expression's minute of the hour, from 0 to 59.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
    */   
  def Minute(term: Expr): Expr =
    Expr(ObjectV("minute" -> term.value))

  /**
    * Returns a time expression's second of the minute, from 0 to 59.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
    */ 
  def Second(term: Expr): Expr =
    Expr(ObjectV("second" -> term.value))

  /**
    * Casts an expression to a data value, if possible.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#time-functions]]
    */
  def ToDate(term: Expr): Expr =
    Expr(ObjectV("to_date" -> term.value))

  /**
    * Merge two or more objects into a single one.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#object-functions]]
    */
  def Merge(merge: Expr, `with`: Expr): Expr =
    Expr(ObjectV("merge" -> merge.value, "with" -> `with`.value))

  /**
    * Merge two or more objects into a single one. A lambda can be specified to resolve conflicts.
    *
    * '''Reference''': [[https://app.fauna.com/documentation/reference/queryapi#object-functions]]
    */
  def Merge(merge: Expr, `with`: Expr, lambda: Expr): Expr =
    Expr(ObjectV("merge" -> merge.value, "with" -> `with`.value, "lambda" -> lambda.value))

  /**
    * Try to convert an array of (field, value) into an object.
    */
  def ToObject(fields: Expr): Expr =
    Expr(ObjectV("to_object" -> fields.value))

  /**
    * Try to convert an object into an array of (field, value).
    */
  def ToArray(obj: Expr): Expr =
    Expr(ObjectV("to_array" -> obj.value))

  /**
    * Check if the expression is a number.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isnumber]]
    */
  def IsNumber(expr: Expr): Expr =
    Expr(ObjectV("is_number" -> expr.value))

  /**
    * Check if the expression is a double.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isdouble]]
    */
  def IsDouble(expr: Expr): Expr =
    Expr(ObjectV("is_double" -> expr.value))

  /**
    * Check if the expression is an integer.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isinteger]]
    */
  def IsInteger(expr: Expr): Expr =
    Expr(ObjectV("is_integer" -> expr.value))

  /**
    * Check if the expression is a boolean.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isboolean]]
    */
  def IsBoolean(expr: Expr): Expr =
    Expr(ObjectV("is_boolean" -> expr.value))

  /**
    * Check if the expression is null.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isnull]]
    */
  def IsNull(expr: Expr): Expr =
    Expr(ObjectV("is_null" -> expr.value))

  /**
    * Check if the expression is a byte array.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isbytes]]
    */
  def IsBytes(expr: Expr): Expr =
    Expr(ObjectV("is_bytes" -> expr.value))

  /**
    * Check if the expression is a timestamp.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/istimestamp]]
    */
  def IsTimestamp(expr: Expr): Expr =
    Expr(ObjectV("is_timestamp" -> expr.value))

  /**
    * Check if the expression is a date.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isdate]]
    */
  def IsDate(expr: Expr): Expr =
    Expr(ObjectV("is_date" -> expr.value))

  /**
    * Check if the expression is a string.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isstring]]
    */
  def IsString(expr: Expr): Expr =
    Expr(ObjectV("is_string" -> expr.value))

  /**
    * Check if the expression is an array.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isarray]]
    */
  def IsArray(expr: Expr): Expr =
    Expr(ObjectV("is_array" -> expr.value))

  /**
    * Check if the expression is an object.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isobject]]
    */
  def IsObject(expr: Expr): Expr =
    Expr(ObjectV("is_object" -> expr.value))

  /**
    * Check if the expression is a reference.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isref]]
    */
  def IsRef(expr: Expr): Expr =
    Expr(ObjectV("is_ref" -> expr.value))

  /**
    * Check if the expression is a set.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isset]]
    */
  def IsSet(expr: Expr): Expr =
    Expr(ObjectV("is_set" -> expr.value))

  /**
    * Check if the expression is a document (either a reference or an instance).
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isdoc]]
    */
  def IsDoc(expr: Expr): Expr =
    Expr(ObjectV("is_doc" -> expr.value))

  /**
    * Check if the expression is a lambda.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/islambda]]
    */
  def IsLambda(expr: Expr): Expr =
    Expr(ObjectV("is_lambda" -> expr.value))

  /**
    * Check if the expression is a collection.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/iscollection]]
    */
  def IsCollection(expr: Expr): Expr =
    Expr(ObjectV("is_collection" -> expr.value))

  /**
    * Check if the expression is a database.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isdatabase]]
    */
  def IsDatabase(expr: Expr): Expr =
    Expr(ObjectV("is_database" -> expr.value))

  /**
    * Check if the expression is an index.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isindex]]
    */
  def IsIndex(expr: Expr): Expr =
    Expr(ObjectV("is_index" -> expr.value))

  /**
    * Check if the expression is a function.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isfunction]]
    */
  def IsFunction(expr: Expr): Expr =
    Expr(ObjectV("is_function" -> expr.value))

  /**
    * Check if the expression is a key.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/iskey]]
    */
  def IsKey(expr: Expr): Expr =
    Expr(ObjectV("is_key" -> expr.value))

  /**
    * Check if the expression is a token.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/istoken]]
    */
  def IsToken(expr: Expr): Expr =
    Expr(ObjectV("is_token" -> expr.value))

  /**
    * Check if the expression is a credentials.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/iscredentials]]
    */
  def IsCredentials(expr: Expr): Expr =
    Expr(ObjectV("is_credentials" -> expr.value))

  /**
    * Check if the expression is a role.
    *
    * '''Reference''': [[https://docs.fauna.com/fauna/current/api/fql/functions/isrole]]
    */
  def IsRole(expr: Expr): Expr =
    Expr(ObjectV("is_role" -> expr.value))
}
