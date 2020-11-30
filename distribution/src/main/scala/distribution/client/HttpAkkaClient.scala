package distribution.client

import java.io.{File, IOException}
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.{Post, addCredentials}
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpCredentials}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart}
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import com.vyulabs.update.distribution.DistributionWebPaths._
import com.vyulabs.update.distribution.client.GraphqlRequest
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class HttpAkkaClient(val distributionUrl: URL)
                    (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext)  {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def makeUrl(path: String*): URL = new URL(path.foldLeft(distributionUrl.toString)((url, path) => url + "/" + path))

  def graphqlRequest[Response](request: GraphqlRequest[Response])
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

  def upload(url: URL, file: File): Future[Unit] = {
    val multipartForm =
      Multipart.FormData(Multipart.FormData.BodyPart(
        "instances-state",
        HttpEntity(ContentTypes.`application/octet-stream`, file.length, FileIO.fromPath(file.toPath)),
        Map("filename" -> file.getName)))
    var post = Post(url.toString, multipartForm)
    getHttpCredentials().foreach(credentials => post = post.addCredentials(credentials))
    for {
      response <- Http(system).singleRequest(post)
      entity <- response.entity.dataBytes.runFold(ByteString())(_ ++ _)
    } yield {
      val response = entity.decodeString("utf8")
      if (response != "Success") {
        throw new IOException(s"Unexpected response from server: ${response}")
      }
    }
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
