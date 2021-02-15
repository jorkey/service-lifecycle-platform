package com.vyulabs.update.distribution.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.{Get, Post}
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpCredentials}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart, StatusCodes}
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.distribution.client.HttpClient
import com.vyulabs.update.common.distribution.client.graphql.GraphqlRequest
import com.vyulabs.update.distribution.client.AkkaHttpClient.AkkaSource
import org.slf4j.LoggerFactory
import spray.json._

import java.io.{File, IOException}
import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpClient(distributionUrl: URL)
                    (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext) extends HttpClient[AkkaSource] {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def graphql[Response](request: GraphqlRequest[Response])
                       (implicit reader: JsonReader[Response]): Future[Response] = {
    val queryJson = request.encodeRequest()
    log.debug(s"Send graphql query: ${queryJson}")
    var post = Post(distributionUrl.toString + "/" + graphqlPathPrefix,
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
        throw new IOException(entity.decodeString("utf8"))
      }
    }
  }

  override def graphqlSub[Response](request: GraphqlRequest[Response])
                                   (implicit reader: JsonReader[Response]): Future[AkkaSource[Response]] = {
    val queryJson = request.encodeRequest()
    log.debug(s"Send graphql query: ${queryJson}")
    var post = Post(distributionUrl.toString + "/" + graphqlPathPrefix,
      HttpEntity(ContentTypes.`application/json`, request.encodeRequest().compactPrint.getBytes()))
    getHttpCredentials().foreach(credentials => post = post.addCredentials(credentials))
    for {
      response <- Http(system).singleRequest(post)
    } yield {
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
    }
  }

  def upload(path: String, fieldName: String, file: File): Future[Unit] = {
    val multipartForm =
      Multipart.FormData(Multipart.FormData.BodyPart(
        fieldName,
        HttpEntity(ContentTypes.`application/octet-stream`, file.length, FileIO.fromPath(file.toPath)),
        Map("filename" -> file.getName)))
    var post = Post(distributionUrl.toString + "/" + imagePathPrefix + "/" + path, multipartForm)
    getHttpCredentials().foreach(credentials => post = post.addCredentials(credentials))
    for {
      response <- Http(system).singleRequest(post)
      entity <- response.entity.dataBytes.runFold(ByteString())(_ ++ _)
    } yield {
      if (response.status != StatusCodes.OK) {
        throw new IOException(s"Unexpected response from server: ${entity.decodeString("utf8")}")
      }
    }
  }

  def download(path: String, file: File): Future[Unit] = {
    var get = Get(distributionUrl.toString + "/" + imagePathPrefix + "/" + path)
    getHttpCredentials().foreach(credentials => get = get.addCredentials(credentials))
    for {
      response <- Http(system).singleRequest(get)
      result <- response.entity.dataBytes.runWith(FileIO.toPath(file.toPath)).map(_ => ())
    } yield result
  }

  override def exists(path: String): Future[Unit] = {
    throw new NotImplementedError()
  }

  private def getHttpCredentials(): Option[HttpCredentials] = {
    if (distributionUrl.getUserInfo != null) {
      val userInfo = distributionUrl.getUserInfo
      val index = userInfo.indexOf(':')
      if (index != -1) {
        val user = userInfo.substring(0, index)
        val password = userInfo.substring(index + 1)
        Some(BasicHttpCredentials(user, password))
      } else {
        None
      }
    } else {
      None
    }
  }
}

object AkkaHttpClient {
  type AkkaSource[T] = Source[T, Any]
}