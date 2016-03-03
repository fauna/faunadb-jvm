package faunadb.query

import scala.reflect.macros._

class QueryMacros(val c: whitebox.Context) {
  import c.universe._

  val T = q"_root_.faunadb.types"
  val M = q"_root_.faunadb.query"

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
    val bindings = vals map { v => q"(${v.name.toString}, ${v.rhs}: $M.Expr)" }

    val tv = q"$M.Let($bindings, { ..$varDefs; $expr })"

    tv
  }

  def lambda(fn: c.Tree): c.Tree = {
    val (vals, expr) = c.untypecheck(fn.duplicate) match {
      case Function(ps, expr) => (ps, expr)
      case _ => c.abort(c.enclosingPosition, "type mismatch: Argument must be a function literal.")
    }

    val used = vals filter { v =>
      expr exists { case Ident(name) => v.name == name; case _ => false }
    } toSet

    val varDefs = used map { v => q"val ${v.name} = $M.Var(${v.name.toString})" }
    val paramsV = vals map { v => if (used contains v) v.name.toString else "_" } match {
      case List(p) => q"$p"
      case ps      => q"$M.Arr(..$ps)"
    }

    val tv = q"$M.Lambda($paramsV, { ..$varDefs; $expr })"

    tv
  }
}
