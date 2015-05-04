package com.faunadb.httpclient

import java.net.{ConnectException, URL}

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.ning.http.client._
import com.ning.http.util.Base64
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise
import scala.util.{Failure, Success}

object Connection {
  def DefaultConfig = new AsyncHttpClientConfig.Builder()
    .setConnectionTimeoutInMs(1000) // 1 second connect timeout
    .setRequestTimeoutInMs(5000) // 5 second req. timeout
    .setAllowPoolingConnection(false)
    .setAllowSslConnectionPool(false)
    .build()

  val DefaultRoot = "https://rest.faunadb.com"

  case class Builder(authToken: String = "", faunaRoot: String = "https://rest.faunadb.com", clientConfig: AsyncHttpClientConfig = DefaultConfig, metricRegistry: MetricRegistry = new MetricRegistry, objectMapper: ObjectMapper = new ObjectMapper()) {
    def setAuthToken(tok: String) = copy(authToken = tok)
    def setFaunaRoot(root: String) = copy(faunaRoot = root)
    def setClientConfig(config: AsyncHttpClientConfig) = copy(clientConfig = config)
    def setMetricRegistry(registry: MetricRegistry) = copy(metricRegistry = registry)

    def build() = {
      new Connection(new URL(faunaRoot), authToken, new AsyncHttpClient(clientConfig), metricRegistry)
    }
  }
}

class Connection(faunaRoot: URL, authToken: String, client: AsyncHttpClient, registry: MetricRegistry) {
  private[httpclient] val json =  new ObjectMapper().registerModule(DefaultScalaModule)
  private val log = LoggerFactory.getLogger("fauna")
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
      .setHeader("Connection", "close")
      .setUrl(new URL(faunaRoot, path).toString)

    params foreach { case (k,v) => builder.addQueryParameter(k,v) }

    val req = builder.build()

    performRequest(req)
  }

  def post(path: String, body: JsonNode) = {
    val req = new RequestBuilder("POST")
      .setUrl(new URL(faunaRoot, path).toString)
      .setBody(json.writeValueAsString(body))
      .setHeader("Connection", "close")
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
      .setHeader("Connection", "close")
      .setHeader("Content-Type", "application/json")
      .build()

    performRequest(req)
  }

  def patch(path: String, body: JsonNode) = {
    val req = new RequestBuilder("PATCH")
      .setUrl(new URL(faunaRoot, path).toString)
      .setBody(json.writeValueAsString(body))
      .setHeader("Connection", "close")
      .setHeader("Content-Type", "application/json")
      .build()

    performRequest(req)
  }

  def logRequest(request: Request) = {
  }

  def close(): Unit = {
    client.close()
  }

  def performRequest(request: Request) = {
    logRequest(request)

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

    val f = rv.future

    f.onComplete {
      case Success(resp) =>
        val reqData = Option(request.getStringData).getOrElse("")
        val faunaHost = Option(resp.getHeader("X-FaunaDB-Host")).getOrElse("Unknown")
        val faunaBuild = Option(resp.getHeader("X-FaunaDB-Build")).getOrElse("Unknown")
        val respBody = Option(resp.getResponseBody).getOrElse("")
        log.debug(s"Request: ${request.getMethod} ${request.getURI}: ${reqData}. Response: Status=${resp.getStatusCode}, Fauna Host=${faunaHost}, Fauna Build=${faunaBuild}: ${respBody}}")
      case Failure(ex) =>
        val reqData = Option(request.getStringData).getOrElse("")
        log.info(s"Request: ${request.getMethod} ${request.getURI}: ${reqData}. Failed: ${ex.getMessage}", ex)
    }

    f.recover {
      case ex: ConnectException => throw new UnavailableException(ex.getMessage)
    }
  }
}