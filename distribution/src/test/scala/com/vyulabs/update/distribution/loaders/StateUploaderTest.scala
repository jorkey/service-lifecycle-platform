package com.vyulabs.update.distribution.loaders

import java.io.IOException
import java.util.Date

import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.vyulabs.update.common.Common.{ClientName, InstanceId, ServiceName}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{ClientServiceState, DirectoryServiceState, ServiceState}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import distribution.loaders.{StateUploader, UploadRequests}
import distribution.mongo.ServiceStateDocument
import spray.json.{JsValue, enrichAny}

import scala.concurrent.{Future, Promise}

class StateUploaderTest extends TestEnvironment {
  behavior of "State Uploader"

  case class GraphqlMutationRequest(command: String, arguments: Map[String, JsValue])
  case class FileUploadRequest(path: String, fileName: String, length: Long, source: Source[ByteString, Future[IOResult]])

  class UploadRequestsHook extends UploadRequests {
    var graphqlRequestPromise = Promise[GraphqlMutationRequest]
    var fileUploadRequestPromise = Promise[FileUploadRequest]
    var result = Future.successful()

    def reset(): Unit = {
      graphqlRequestPromise = Promise[GraphqlMutationRequest]
      fileUploadRequestPromise = Promise[FileUploadRequest]
    }

    def setResult(result: Future[Unit]): Unit = {
      this.result = result
    }

    override def graphqlMutationRequest(command: String, arguments: Map[String, JsValue]): Future[Unit] = {
      graphqlRequestPromise.success(GraphqlMutationRequest(command, arguments))
      result
    }

    override def fileUploadRequest(path: String, fileName: String, length: Long, source: Source[ByteString, Future[IOResult]]): Future[Unit] = {
      fileUploadRequestPromise.success(FileUploadRequest(path, fileName, length, source))
      result
    }
  }

  it should "upload service states" in {
    val hook = new UploadRequestsHook()
    val uploader = new StateUploader(collections, distributionDir, 1, hook)
    uploader.start()

    val state1 = ClientServiceState("client1", "instance1", DirectoryServiceState("service1", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(0, state1))))
    hook.reset()
    val request1 = result(hook.graphqlRequestPromise.future)
    assertResult("setServicesState")(request1.command)
    assertResult(Map("state" -> Seq(state1).toJson))(request1.arguments)

    val state2 = ClientServiceState("client2", "instance2", DirectoryServiceState("service2", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(1, state1))))
    hook.reset()
    val request2 = result(hook.graphqlRequestPromise.future)
    assertResult("setServicesState")(request2.command)
    assertResult(Map("state" -> Seq(state1).toJson))(request2.arguments)

    uploader.stop()

    result(collections.State_ServiceStates.map(_.dropItems()))
    result(collections.State_UploadStatus.map(_.dropItems()))
  }

  it should "try to upload service states again after failure" in {
    val hook = new UploadRequestsHook()
    val uploader = new StateUploader(collections, distributionDir, 1, hook)
    uploader.start()

    val state1 = ClientServiceState("client1", "instance1", DirectoryServiceState("service1", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 0))))), None, None, None, None)))
    hook.setResult(Future.failed(new IOException("upload error")))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(0, state1))))

    val request1 = result(hook.graphqlRequestPromise.future)
    assertResult("setServicesState")(request1.command)
    assertResult(Map("state" -> Seq(state1).toJson))(request1.arguments)

    val state2 = ClientServiceState("client2", "instance2", DirectoryServiceState("service2", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(1, state2))))

    hook.reset()
    val request2 = result(hook.graphqlRequestPromise.future)
    assertResult("setServicesState")(request2.command)
    assertResult(Map("state" -> Seq(state1, state2).toJson))(request2.arguments)

    hook.setResult(Future.successful())
    hook.reset()
    val request3 = result(hook.graphqlRequestPromise.future)
    assertResult("setServicesState")(request3.command)
    assertResult(Map("state" -> Seq(state1, state2).toJson))(request3.arguments)

    val state3 = ClientServiceState("client3", "instance3", DirectoryServiceState("service3", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(2, state3))))
    hook.reset()
    val request4 = result(hook.graphqlRequestPromise.future)
    assertResult("setServicesState")(request4.command)
    assertResult(Map("state" -> Seq(state3).toJson))(request4.arguments)

    uploader.stop()

    result(collections.State_ServiceStates.map(_.dropItems()))
    result(collections.State_UploadStatus.map(_.dropItems()))
  }
}