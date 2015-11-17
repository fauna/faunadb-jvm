package faunadb

/**
 * Provides representations of the FaunaDB response format.
 *
 * Most calls to [[FaunaClient]] will return a [[com.faunadb.client.response.ResponseNode]]. This represents an abstract
 * node in the FaunaDB response tree. These nodes can be converted into basic FaunaDB types, as well as various structured
 * response forms.
 *
 * ==Forms==
 *
 * [[com.faunadb.client.response.Class]] - [[https://faunadb.com/documentation/objects#classes]]
 *
 * [[com.faunadb.client.response.Database]] - [[https://faunadb.com/documentation/objects#databases]]
 *
 * [[com.faunadb.client.response.Index]] - [[https://faunadb.com/documentation/objects#indexes]]
 *
 * [[com.faunadb.client.response.Instance]] - [[https://faunadb.com/documentation/objects]]
 *
 * [[com.faunadb.client.response.Key]] - [[https://faunadb.com/documentation/objects#keys]]
 *
 */
package object response {

}
