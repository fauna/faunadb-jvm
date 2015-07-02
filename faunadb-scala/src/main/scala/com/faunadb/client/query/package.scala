package com.faunadb.client

/**
 * Classes modeling the FaunaDB Query language.
 *
 * Instances of these classes can be composed to model a query expression, which can then be passed to
 * [[FaunaClient.query]] in order to execute the query.
 *
 * [[query.Language]] contains implicit functions to aid in type conversions when creating a query expression.
 *
 * ===Example===
 * {{{
 * val query = Create(Ref("classes/spells"), Quote(ObjectV("data" -> Object("name" -> "Magic Missile"))))
 * }}}
 *
 */
package object query {

}
