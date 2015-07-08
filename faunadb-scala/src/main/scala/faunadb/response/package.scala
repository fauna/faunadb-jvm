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
 * [[com.faunadb.client.response.Class]] - [[https://faunadb.com/documentation#guide-resource_types-classes Reference]]
 *
 * [[com.faunadb.client.response.Database]] - [[https://faunadb.com/documentation#guide-resource_types-database Reference]]
 *
 * [[com.faunadb.client.response.Index]] - [[https://faunadb.com/documentation#guide-resource_types-indexes Reference]]
 *
 * [[com.faunadb.client.response.Instance]] - [[https://faunadb.com/documentation#guide-resource_types-instances Reference]]
 *
 * [[com.faunadb.client.response.Key]] - [[https://faunadb.com/documentation#guide-resource_types-keys Reference]]
 *
 */
package object response {

}
