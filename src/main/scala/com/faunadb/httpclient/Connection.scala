package com.faunadb.httpclient

import java.net.URL

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.ning.http.client._
import com.ning.http.util.Base64

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise

object Connection {
  def DefaultConfig = new AsyncHttpClientConfig.Builder()
    .setConnectionTimeoutInMs(1000) // 1 second connect timeout
    .setRequestTimeoutInMs(5000) // 5 second req. timeout
    .setMaximumConnectionsPerHost(20)
    .setMaximumConnectionsTotal(20) // 20 max connections
    .build()

  val DefaultRoot = "https://rest1.fauna.org"

  case class Builder(authToken: String = "", faunaRoot: String = "https://rest1.fauna.org", clientConfig: AsyncHttpClientConfig = DefaultConfig, metricRegistry: MetricRegistry = new MetricRegistry, objectMapper: ObjectMapper = new ObjectMapper()) {
    def setAuthToken(tok: String) = copy(authToken = tok)
    def setFaunaRoot(root: String) = copy(faunaRoot = root)
    def setClientConfig(config: AsyncHttpClientConfig) = copy(clientConfig = config)
    def setMetricRegistry(registry: MetricRegistry) = copy(metricRegistry = registry)
    def setJson(mapper: ObjectMapper) = copy(objectMapper = mapper)

    def build() = {
      new Connection(new URL(faunaRoot), authToken, new AsyncHttpClient(clientConfig), metricRegistry, objectMapper)
    }
  }
}

class Connection(faunaRoot: URL, authToken: String, client: AsyncHttpClient, registry: MetricRegistry, private[httpclient] val json: ObjectMapper) {
  private val authHeader = "Basic " + Base64.encode((authToken + ":").getBytes("ASCII"))

  def get(path: String) = {
    val req = new RequestBuilder("GET")
      .setUrl(new URL(faunaRoot, path).toString)
      .build()

    performRequest(req)
  }

  def get(path: String, params: (String, String)*) = {
    val qParams = new FluentStringsMap()
    val builder = new RequestBuilder("GET")
      .setUrl(new URL(faunaRoot, path).toString)

    params foreach { case (k,v) => builder.addQueryParameter(k,v) }

    val req = builder.build()

    performRequest(req)
  }

  def post(path: String, body: JsonNode) = {
    val req = new RequestBuilder("POST")
      .setUrl(new URL(faunaRoot, path).toString)
      .setBody(json.writeValueAsString(body))
      .setHeader("Content-Type", "application/json")
      .build()

    performRequest(req)
  }

  def put(path: String) = {
    val req = new RequestBuilder("PUT")
      .setUrl(new URL(faunaRoot, path).toString)
      .build()

    performRequest(req)
  }

  def put(path: String, body: JsonNode) = {
    val req = new RequestBuilder("PUT")
      .setUrl(new URL(faunaRoot, path).toString)
      .setBody(json.writeValueAsString(body))
      .setHeader("Content-Type", "application/json")
      .build()

    performRequest(req)
  }

  def patch(path: String, body: JsonNode) = {
    val req = new RequestBuilder("PATCH")
      .setUrl(new URL(faunaRoot, path).toString)
      .setBody(json.writeValueAsString(body))
      .setHeader("Content-Type", "application/json")
      .build()

    performRequest(req)

  }

  def performRequest(request: Request) = {
    val rv = Promise[Response]()
    val ctx = registry.timer("fauna-request").time()
    client.prepareRequest(request).addHeader("Authorization", authHeader).execute(new AsyncCompletionHandler[Response] {
      override def onThrowable(t: Throwable): Unit = {
        ctx.stop()
        rv.failure(t)
      }

      override def onCompleted(response: Response): Response = {
        ctx.stop()
        rv.success(response)
        response
      }
    })

    rv.future.map(parseResponseAndHandleErrors(_))
  }

  private def parseResponseAndHandleErrors(response: Response): ResourceResponse =  {
    response.getStatusCode match {
      case 204 =>
        ResourceResponse(204, json.createObjectNode(), json.createObjectNode())
      case x if x >= 200 && x < 300 =>
        val parsed = parseResponseBody(response)
        val refNode = parsed.path("references")
        val references = if (refNode.isMissingNode) {
          json.createObjectNode() // empty
        } else {
          refNode.asInstanceOf[ObjectNode]
        }

        val resources = parsed.path("resource")
        ResourceResponse(response.getStatusCode, resources, references)
      case code@_ =>
        val parsed = parseResponseBody(response)
        val message = parsed.path("error").asText()
        throw FaunaException.wrapResponse(ErrorResponse(code, message))
    }
  }

  private def parseResponseBody(response: Response) = {
    val body = response.getResponseBody("UTF-8")
    json.readTree(body)
  }
}