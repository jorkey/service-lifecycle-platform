package com.vyulabs.update.distribution.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.{Get, Post}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, HttpCredentials, OAuth2BearerToken}
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.scaladsl.{BroadcastHub, Concat, FileIO, Flow, Framing, Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, OverflowStrategy, QueueOfferResult}
import akka.util.ByteString
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.distribution.client.HttpClient
import com.vyulabs.update.common.distribution.client.graphql.GraphqlRequest
import com.vyulabs.update.distribution.client.AkkaHttpClient.AkkaSource
import com.vyulabs.update.distribution.common.AkkaCallbackSource
import com.vyulabs.update.distribution.graphql._
import org.slf4j.Logger
import spray.json._

import java.io.{File, IOException}
import java.net.URL
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Random, Success}

class AkkaHttpClient(val distributionUrl: String, initAccessToken: Option[String] = None)
                    (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext) extends HttpClient[AkkaSource] {
  accessToken = initAccessToken

  private val poolClientFlow = {
    val url = new URL(distributionUrl)
    val port = url.getPort
    if (url.getProtocol == "https") {
      Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]](url.getHost, if (port != -1) port else 443)
    } else {
      Http().cachedHostConnectionPool[Promise[HttpResponse]](url.getHost, if (port != -1) port else 80)
    }
  }
  private val queue =
    Source.queue[(HttpRequest, Promise[HttpResponse])](10, OverflowStrategy.fail)
      .via(poolClientFlow)
      .to(Sink.foreach({
        case (Success(resp), p) =>
          p.success(resp)
        case (Failure(ex), p) =>
          system.log.error(ex, "Http request failure")
          p.failure(ex)
      }))
      .run()

  private def httpRequest(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued    => responsePromise.future
      case QueueOfferResult.Dropped     => Future.failed(new IOException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed => Future.failed(new IOException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }

  def graphql[Response](request: GraphqlRequest[Response])
                       (implicit reader: JsonReader[Response], log: Logger): Future[Response] = {
    val queryJson = request.encodeRequest()
    log.debug(s"Send graphql query: ${queryJson}")
    var post = Post(distributionUrl + "/" + graphqlPathPrefix,
      HttpEntity(ContentTypes.`application/json`, request.encodeRequest().compactPrint.getBytes()))
    getHttpCredentials().foreach(credentials => post = post.addCredentials(credentials))
    for {
      response <- httpRequest(post)
      entity <- response.entity.dataBytes.runFold(ByteString())(_ ++ _)
    } yield {
      if (response.status.isSuccess()) {
        val responseJson = entity.decodeString("utf8")
        log.debug(s"Receive graphql response: ${responseJson}")
        request.decodeResponse(responseJson.parseJson.asJsObject) match {
          case Left(value) => value
          case Right(value) => throw new IOException(value)
        }
      } else {
        log.error(s"Receive graphql error: ${response}")
        if (response.status == StatusCodes.Unauthorized) {
          accessToken = None
        }
        throw new IOException(entity.decodeString("utf8"))
      }
    }
  }

  override def subscribeSSE[Response](request: GraphqlRequest[Response])
                                     (implicit reader: JsonReader[Response], log: Logger): Future[AkkaSource[Response]] = {
    val queryJson = request.encodeRequest()
    log.debug(s"Send graphql SSE query: ${queryJson}")
    val post = Post(distributionUrl.toString + "/" + graphqlPathPrefix + "/" + websocketPathPrefix,
      HttpEntity(ContentTypes.`application/json`, request.encodeRequest().compactPrint.getBytes()))
    getHttpCredentials().foreach(credentials => post.addCredentials(credentials))
    for {
      response <- httpRequest(post)
    } yield {
      if (response.status.isSuccess()) {
        response.entity.dataBytes
          .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1048576))
          .map(_.decodeString("utf8"))
          .filter(!_.isEmpty)
          .map(line => if (line.startsWith("data:")) line.substring(5) else throw new IOException(s"Error data line: ${line}"))
          .map(line => request.decodeResponse(line.parseJson.asJsObject) match {
            case Left(response) =>
              response
            case Right(error) =>
              throw new IOException(error)
          })
      } else {
        if (response.status == StatusCodes.Unauthorized) {
          accessToken = None
        }
        throw new IOException(response.status.toString())
      }
    }
  }

  override def subscribeWS[Response](request: GraphqlRequest[Response])
                                    (implicit reader: JsonReader[Response], log: Logger): Future[AkkaSource[Response]] = {
    log.debug(s"Send graphql websocket query: ${request}")
    val token = accessToken.getOrElse(throw new IOException("Not authorized"))
    var uri = Uri(distributionUrl + "/" + graphqlPathPrefix + "/" + websocketPathPrefix)
    if (uri.scheme == "http") {
      uri = uri.withScheme("ws")
    } else if (uri.scheme == "https") {
      uri = uri.withScheme("wss")
    }
    val ((publisherCallback, killSwitch), publisherSource) =
      Source.fromGraph(new AkkaCallbackSource[Response]())
        .viaMat(KillSwitches.single)(Keep.both)
        .toMat(BroadcastHub.sink)(Keep.both)
        .run()
    val id = Random.nextInt().toString
    val connectionAcked = Promise[Unit]()

    def processMessage(message: String): Unit = {
      val response = message.parseJson.asJsObject
      val responseType = response.fields.get("type").map(_.asInstanceOf[JsString].value).getOrElse("unknown")
      responseType match {
        case ConnectionAck.`type` =>
          connectionAcked.success()
        case Next.`type` =>
          val subscribe = response.convertTo[Next]
          publisherCallback.invoke(request.decodeResponse(subscribe.payload) match {
            case Left(response) =>
              //                    if (log.isDebugEnabled) log.debug(s"Received websocket message: ${response}")
              response
            case Right(error) =>
              throw new IOException(error)
          })
        case Complete.`type` =>
          val complete = response.convertTo[Complete]
          log.debug(s"Received websocket complete message")
          system.scheduler.scheduleOnce(FiniteDuration(1, TimeUnit.SECONDS))(killSwitch.shutdown())
        case Error.`type` =>
          val error = response.convertTo[Error]
          log.error(s"Received websocket subscription error: ${error.payload.message}")
          system.scheduler.scheduleOnce(FiniteDuration(1, TimeUnit.SECONDS))(killSwitch.shutdown())
        case m =>
          log.error(s"Invalid message: ${m}")
      }
    }

    val handleMessage = Source.fromGraph(new AkkaCallbackSource[Source[String, _]]()).map {
      _.runFold("")(_ + _).andThen {
        case Success(message) =>
          processMessage(message)
        case Failure(ex) =>
          log.error("Receive websocket message error", ex)
      }
    }.toMat(BroadcastHub.sink)(Keep.left).run()

    (for {
      response <- {
        val handlerFlow = Flow.fromSinkAndSourceMat(Sink.foreach[Message](message => {
          message match {
            case TextMessage.Streamed(source) =>
              handleMessage.invoke(source)
            case TextMessage.Strict(message) =>
              handleMessage.invoke(Source.single(message))
            case m =>
              throw new IOException(s"Unknown websocket message ${m}")
          }}), Source.combine(
            Source.single(TextMessage(ConnectionInit(payload =
              ConnectionInitPayload(s"Bearer ${token}")).toJson.compactPrint)),
            Source.future(connectionAcked.future).map(_ => TextMessage(Subscribe(id = id, payload =
              SubscribePayload(query = request.encodeQuery(), Some(request.command), Some(request.encodeVariables()), None)).toJson.compactPrint)),
            Source.future(Promise[Message]().future))(Concat(_))
        )(Keep.left)
        val webSocketRequest = WebSocketRequest(uri,
          getHttpCredentials().map(Authorization(_)).to[collection.immutable.Seq], collection.immutable.Seq("graphql-transport-ws"))
        val (response, closed) = Http(system).singleWebSocketRequest(webSocketRequest, handlerFlow)
        closed.foreach { _ =>
          log.info("Websocket connection is closed")
          system.scheduler.scheduleOnce(FiniteDuration(1, TimeUnit.SECONDS))(killSwitch.shutdown())
        }
        response
      }
    } yield {
      if (!response.response.status.isSuccess()) {
        if (response.response.status == StatusCodes.Unauthorized) {
          accessToken = None
        }
        throw new IOException(response.response.status.toString())
      }
    }).map(_ => publisherSource)
  }

  def upload(path: String, fieldName: String, file: File)
            (implicit log: Logger): Future[Unit] = {
    val multipartForm =
      Multipart.FormData(Multipart.FormData.BodyPart(
        fieldName,
        HttpEntity(ContentTypes.`application/octet-stream`, file.length, FileIO.fromPath(file.toPath)),
        Map("filename" -> file.getName)))
    var post = Post(distributionUrl.toString + "/" + loadPathPrefix + "/" + path, multipartForm)
    getHttpCredentials().foreach(credentials => post = post.addCredentials(credentials))
    for {
      response <- httpRequest(post)
      entity <- response.entity.dataBytes.runFold(ByteString())(_ ++ _)
    } yield {
      if (!response.status.isSuccess()) {
        if (response.status == StatusCodes.Unauthorized) {
          accessToken = None
        }
        throw new IOException(s"Unexpected response from server: ${entity.decodeString("utf8")}")
      }
    }
  }

  def download(path: String, file: File)(implicit log: Logger): Future[Unit] = {
    var get = Get(distributionUrl.toString + "/" + loadPathPrefix + "/" + path)
    getHttpCredentials().foreach(credentials => get = get.addCredentials(credentials))
    for {
      response <- httpRequest(get)
      result <- {
        if (response.status.isSuccess()) {
          response.entity.dataBytes.runWith(FileIO.toPath(file.toPath)).map(_ => ())
        } else {
          if (response.status == StatusCodes.Unauthorized) {
            accessToken = None
          }
          response.entity.dataBytes.runFold(ByteString())(_ ++ _)
            .map(entity => throw new IOException(s"Unexpected response from server: ${entity.decodeString("utf8")}"))
        }
      }
    } yield {
      result
    }
  }

  private def getHttpCredentials(): Option[HttpCredentials] = {
//    if (distributionUrl.getAccountInfo != null) {
//      val accountInfo = distributionUrl.getAccountInfo
//      val index = accountInfo.indexOf(':')
//      if (index != -1) {
//        val account = accountInfo.substring(0, index)
//        val password = accountInfo.substring(index + 1)
//        Some(BasicHttpCredentials(account, password))
//      } else {
//        None
//      }
//    } else {
//      None
//    }
    accessToken.map(OAuth2BearerToken(_))
  }
}

object AkkaHttpClient {
  type AkkaSource[T] = Source[T, Any]
}