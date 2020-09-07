package faunadb.query

import scala.reflect.macros._

class QueryMacros(val c: whitebox.Context) {
  import c.universe._

  private val T = q"_root_.faunadb.types"
  private val M = q"_root_.faunadb.query"

  private val Expr = c.typecheck(tq"$M.Expr", c.TYPEmode, silent = false).tpe

  def let(block: Tree): Tree = {
    val (bindings, expr) = c.untypecheck(block.duplicate) match {
      case Block(stmts, expr) =>
        stmts foreach {
          case ValDef(_, _, _, _) => ()
          case _ => c.abort(c.enclosingPosition, "Let call does not have the structure: Let { val a = ???, ..., val n = ???, <in_expr> }")
        }

        (stmts.asInstanceOf[List[ValDef]], expr)
      case expr => (Nil, expr)
    }

    val types = block match {
      case Block(stmts, _) => stmts.asInstanceOf[List[ValDef]].iterator.map { v => v.name.toString -> v.tpt.tpe }.toMap
      case _ => scala.collection.immutable.Map.empty[String, Type]
    }

    val labels = bindings map { _ => TermName(c.freshName("binding")) }

    val defs = labels zip bindings flatMap {
      case (b, v) => List(q"val $b: $M.Expr = ${v.rhs}", q"val ${v.name} = $M.Var(${v.name.toString})")
    }

    val barg = labels zip bindings map { case (b, v) => q"(${v.name.toString}, $b)" }

    (new Transformer {
      override def transform(t: Tree): Tree =
        t match {
          case q"query.this.Expr.encode[$xt]($x)($_)" if types.get(x.toString) contains xt.tpe => x
          case _ => super.transform(t)
        }
    }).transform(q"{ ..$defs; $M.Let($barg, $expr) }")
  }

  def lambda(fn: Tree): Tree = {
    val (vals, expr) = c.untypecheck(fn.duplicate) match {
      case Function(ps, expr) => (ps, expr)
      case _ => c.abort(c.enclosingPosition, "type mismatch: Argument must be a function literal.")
    }

    val used = vals.iterator.filter { v =>
      expr exists { case Ident(name) => v.name == name; case _ => false }
    }.toSet

    val varDefs = used map { v => q"val ${v.name} = $M.Var(${v.name.toString})" }
    val paramsV = vals map { v => if (used contains v) v.name.toString else "_" } match {
      case List(p) => q"$p"
      case ps      => q"$M.Arr(..$ps)"
    }

    val tv = q"$M.Lambda($paramsV, { ..$varDefs; $expr })"

    tv
  }

  def query(fn: Tree): Tree =
    q"$M.Query(${lambda(fn)})"
}
