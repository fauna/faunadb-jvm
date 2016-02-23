package faunadb.response

import faunadb.values.LazyValue

/**
 * Represents a FaunaDB Page response. This, like other response types, is obtained by coercing a
 * [[faunadb.types.LazyValue]] using its associated conversion method: [[faunadb.types.LazyValue.asPage]] or [[faunadb.types.LazyValue.asPageOpt]].
 *
 */
case class Page(data: Array[LazyValue], before: Option[LazyValue], after: Option[LazyValue])
