package faunadb.response

import faunadb.types.LazyValue

/**
 * Represents a FaunaDB Page response. This, like other response types, is obtained by coercing a
 * [[LazyValue]] using its associated conversion method: [[LazyValue.asPage]] or [[LazyValue.asPageOpt]].
 *
 */
case class Page(data: Array[LazyValue], before: Option[LazyValue], after: Option[LazyValue])

