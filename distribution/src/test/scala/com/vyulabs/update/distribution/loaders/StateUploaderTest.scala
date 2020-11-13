package com.vyulabs.update.distribution.loaders

import java.util.Date

import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{ClientServiceState, DirectoryServiceState, ServiceState}
import com.vyulabs.update.version.BuildVersion
import distribution.loaders.{StateUploader, UploadRequests}
import distribution.mongo.ServiceStateDocument
import spray.json.JsValue

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}

class StateUploaderTest extends TestEnvironment {
  behavior of "State Uploader"

  case class GraphqlMutationRequest(command: String, arguments: Map[String, JsValue])
  case class FileUploadRequest(path: String, fileName: String, length: Long, source: Source[ByteString, Future[IOResult]])

  class UploadRequestsHook extends UploadRequests {
    var graphqlRequestPromise = Promise[GraphqlMutationRequest]
    var fileUploadRequestPromise = Promise[FileUploadRequest]

    override def graphqlMutationRequest(command: String, arguments: Map[String, JsValue]): Future[Unit] = {
      graphqlRequestPromise.success(GraphqlMutationRequest(command, arguments)).future.map(_ => Unit)
    }

    override def fileUploadRequest(path: String, fileName: String, length: Long, source: Source[ByteString, Future[IOResult]]): Future[Unit] = {
      fileUploadRequestPromise.success(FileUploadRequest(path, fileName, length, source)).future.map(_ => Unit)
    }
  }

  override def beforeAll() = {
  }

  it should "upload service states" in {
    val hook = new UploadRequestsHook()
    val uploader = new StateUploader(collections, distributionDir, 1, hook)
    uploader.start()
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(0,
      ClientServiceState("client1", "instance1", DirectoryServiceState("service1", "directory1",
        ServiceState(date = new Date(), None, None, version = Some(BuildVersion(1, 1, 0)), None, None, None, None)))))))
    result(hook.graphqlRequestPromise.future)
    uploader.stop()
  }
}
