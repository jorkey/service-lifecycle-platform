package com.vyulabs.update.distribution.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.{Get, Post}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, HttpCredentials, OAuth2BearerToken}
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.{KillSwitch, KillSwitches, Materializer, UniqueKillSwitch}
import akka.stream.scaladsl.{BroadcastHub, Concat, FileIO, Flow, Framing, Keep, Sink, Source}
import akka.util.ByteString
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.distribution.client.HttpClient
import com.vyulabs.update.common.distribution.client.graphql.GraphqlRequest
import com.vyulabs.update.distribution.client.AkkaHttpClient.AkkaSource
import com.vyulabs.update.distribution.common.AkkaCallbackSource
import com.vyulabs.update.distribution.graphql.{Complete, ConnectionAck, ConnectionInit, ConnectionInitPayload, Next, Subscribe, SubscribePayload}
import org.slf4j.Logger
import spray.json._

import java.io.{File, IOException}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Random

class AkkaHttpClient(val distributionUrl: String)
                    (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext) extends HttpClient[AkkaSource] {
  def graphql[Response](request: GraphqlRequest[Response])
                       (implicit reader: JsonReader[Response], log: Logger): Future[Response] = {
    val queryJson = request.encodeRequest()
    log.debug(s"Send graphql query: ${queryJson}")
    var post = Post(distributionUrl + "/" + graphqlPathPrefix,
      HttpEntity(ContentTypes.`application/json`, request.encodeRequest().compactPrint.getBytes()))
    getHttpCredentials().foreach(credentials => post = post.addCredentials(credentials))
    for {
      response <- Http(system).singleRequest(post)
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
      response <- Http(system).singleRequest(post)
    } yield {
      if (response.status.isSuccess()) {
        response.entity.dataBytes
          .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 10240))
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
    log.debug(s"Send graphql WebSocket query: ${request}")
    val token = accessToken.getOrElse(throw new IOException("Not authorized"))
    val webSocketRequest = WebSocketRequest(Uri(distributionUrl + "/" + graphqlPathPrefix + "/" + websocketPathPrefix),
      getHttpCredentials().map(Authorization(_)).to[collection.immutable.Seq], collection.immutable.Seq("graphql-transport-ws"))
    val ((publisherCallback, killSwitch), publisherSource) =
      Source.fromGraph(new AkkaCallbackSource[Response]())
        .viaMat(KillSwitches.single)(Keep.both)
        .toMat(BroadcastHub.sink)(Keep.both)
        .run()
    val id = Random.nextInt().toString
    (for {
      response <- {
        val handlerFlow = Flow.fromSinkAndSource[Message, Message](Sink.foreach(m => { m match {
          case TextMessage.Strict(m) =>
            val response = m.parseJson.asJsObject
            val responseType = response.fields.get("type").map(_.asInstanceOf[JsString].value).getOrElse("unknown")
            responseType match {
              case ConnectionAck.`type` =>
              case Next.`type` =>
                val subscribe = response.convertTo[Next]
                publisherCallback.invoke(request.decodeResponse(subscribe.payload) match {
                  case Left(response) =>
                    response
                  case Right(error) =>
                    throw new IOException(error)
                })
              case Complete.`type` =>
                val complete = response.convertTo[Complete]
                killSwitch.shutdown()
              case m =>
                log.error(s"Invalid message ${m}")
            }
          case _ =>
        }}), Source.combine(
            Source.fromIterator(() => Seq(
            TextMessage(ConnectionInit(payload =
              ConnectionInitPayload(s"Bearer ${token}")).toJson.compactPrint),
            TextMessage(Subscribe(id = id, payload =
                SubscribePayload(query = request.encodeQuery(), Some(request.command), Some(request.encodeVariables()), None)).toJson.compactPrint),
            ).iterator), Source.future(Promise[Message]().future))(Concat(_))
        )
        Http(system).singleWebSocketRequest(webSocketRequest, handlerFlow)._1
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
      response <- Http(system).singleRequest(post)
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
      response <- Http(system).singleRequest(get)
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