package com.vyulabs.update.distribution.client

import com.vyulabs.update.common.distribution.client.HttpClient
import com.vyulabs.update.common.distribution.client.graphql.{GraphqlArgument, GraphqlRequest}
import org.scalatest.Matchers
import spray.json.JsonReader

import java.io.{File, IOException}
import org.slf4j.Logger

import java.net.URL
import scala.collection.immutable.Queue
import scala.concurrent.{Future, Promise}

class HttpClientTestStub[Stream[_]] extends HttpClient[Stream] with Matchers {
  trait ClientRequest[Response] { val promise = Promise[Response]() }

  case class GraphqlClientRequest[Response](request: GraphqlRequest[Response]) extends ClientRequest[Response]
  case class UploadClientRequest(path: String, fieldName: String, file: File) extends ClientRequest[Unit]
  case class DownloadClientRequest(path: String, file: File) extends ClientRequest[Unit]
  case class ExistsClientRequest(path: String) extends ClientRequest[Unit]

  val distributionUrl: URL = new URL("http://test:test@test")
  var requests = Queue.empty[ClientRequest[_]]

  def waitForQuery[Response](command: String, arguments: Seq[GraphqlArgument] = Seq.empty, subSelection: String = ""): Promise[Response] = {
    waitForGraphql[Response]("query", command, arguments, subSelection)
  }

  def waitForMutation(command: String, arguments: Seq[GraphqlArgument] = Seq.empty, subSelection: String = ""): Promise[Boolean] = {
    waitForGraphql[Boolean]("mutation", command, arguments, subSelection)
  }

  def waitForGraphql[Response](req: String, command: String,
                               arguments: Seq[GraphqlArgument] = Seq.empty, subSelection: String = ""): Promise[Response] = {
    val request = waitForRequest[GraphqlClientRequest[Response]]()
    assertResult(req)(request.request.request)
    assertResult(command)(request.request.command)
    assertResult(arguments)(request.request.arguments)
    assertResult(subSelection)(request.request.subSelection)
    request.promise
  }

  def waitForUpload(path: String, fieldName: String): Promise[Unit] = {
    val request = waitForRequest[UploadClientRequest]()
    assertResult(path)(request.path)
    assertResult(fieldName)(request.fieldName)
    request.promise
  }

  def waitForDownload(path: String): Promise[Unit] = {
    val request = waitForRequest[DownloadClientRequest]()
    assertResult(path)(request.path)
    request.promise
  }

  def waitForExists(path: String): Promise[Unit] = {
    val request = waitForRequest[ExistsClientRequest]()
    assertResult(path)(request.path)
    request.promise
  }

  private def waitForRequest[T](): T = {
    synchronized {
      while (requests.isEmpty) {
        wait(15000)
      }
      if (requests.isEmpty) {
        throw new IOException("Waiting for request timeout")
      }
      val (request, newRequests) = requests.dequeue
      requests = newRequests
      request.asInstanceOf[T]
    }
  }

  override def graphql[Response](req: GraphqlRequest[Response])
                                (implicit reader: JsonReader[Response], log: Logger): Future[Response] = {
    val request = GraphqlClientRequest(req)
    synchronized { requests = requests.enqueue(request); notify() }
    request.promise.future
  }

  override def graphqlSub[Response](request: GraphqlRequest[Response])
                                   (implicit reader: JsonReader[Response], log: Logger): Future[Stream[Response]] = {
    throw new UnsupportedOperationException()
  }

  override def upload(path: String, fieldName: String, file: File)(implicit log: Logger): Future[Unit] = {
    val request = UploadClientRequest(path, fieldName, file)
    synchronized { requests = requests.enqueue(request); notify() }
    request.promise.future
  }

  override def download(path: String, file: File)(implicit log: Logger): Future[Unit] = {
    val request = DownloadClientRequest(path, file)
    synchronized { requests = requests.enqueue(request); notify() }
    request.promise.future
  }
}
