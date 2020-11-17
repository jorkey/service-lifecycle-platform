package com.vyulabs.update.distribution.loaders

import java.io.IOException
import java.util.Date

import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{ClientServiceState, DirectoryServiceState, ServiceState}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import distribution.loaders.{StateUploader}
import distribution.mongo.ServiceStateDocument
import spray.json.{JsValue, enrichAny}

import scala.concurrent.{Future, Promise}

class ServicesStateUploadTest extends TestEnvironment {
  behavior of "Services State Upload"

  case class GraphqlMutationRequest(command: String, arguments: Map[String, JsValue])

  var requestPromise = Promise[GraphqlMutationRequest]
  var result = Future.successful()

  def graphqlMutationRequest(command: String, arguments: Map[String, JsValue]): Future[Unit] = {
    requestPromise.success(GraphqlMutationRequest(command, arguments))
    result
  }

  it should "upload service states" in {
    val uploader = new StateUploader(collections, distributionDir, 1, graphqlMutationRequest,
      (_, _, _, _) => Future.failed(new IOException("Not expected")))
    uploader.start()

    val state1 = ClientServiceState("client1", "instance1", DirectoryServiceState("service1", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 0))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(0, state1))))
    requestPromise = Promise[GraphqlMutationRequest]
    val request1 = result(requestPromise.future)
    assertResult("setServicesState")(request1.command)
    assertResult(Map("state" -> Seq(state1).toJson))(request1.arguments)

    val state2 = ClientServiceState("client2", "instance2", DirectoryServiceState("service2", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(1, state2))))
    requestPromise = Promise[GraphqlMutationRequest]
    val request2 = result(requestPromise.future)
    assertResult("setServicesState")(request2.command)
    assertResult(Map("state" -> Seq(state2).toJson))(request2.arguments)

    uploader.stop()
    result(collections.State_ServiceStates.map(_.dropItems()))
    result(collections.State_UploadStatus.map(_.dropItems()))
  }

  it should "try to upload service states again after failure" in {
    val uploader = new StateUploader(collections, distributionDir, 1, graphqlMutationRequest,
      (_, _, _, _) => Future.failed(new IOException("Not expected")))
    uploader.start()

    requestPromise = Promise[GraphqlMutationRequest]
    val state1 = ClientServiceState("client1", "instance1", DirectoryServiceState("service1", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 0))))), None, None, None, None)))
    result = Future.failed(new IOException("upload error"))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(0, state1))))

    val request1 = result(requestPromise.future)
    assertResult("setServicesState")(request1.command)
    assertResult(Map("state" -> Seq(state1).toJson))(request1.arguments)

    val state2 = ClientServiceState("client2", "instance2", DirectoryServiceState("service2", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(1, state2))))

    requestPromise = Promise[GraphqlMutationRequest]
    val request2 = result(requestPromise.future)
    assertResult("setServicesState")(request2.command)
    assertResult(Map("state" -> Seq(state1, state2).toJson))(request2.arguments)

    result = Future.successful()
    requestPromise = Promise[GraphqlMutationRequest]
    val request3 = result(requestPromise.future)
    assertResult("setServicesState")(request3.command)
    assertResult(Map("state" -> Seq(state1, state2).toJson))(request3.arguments)

    val state3 = ClientServiceState("client3", "instance3", DirectoryServiceState("service3", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(2, state3))))
    requestPromise = Promise[GraphqlMutationRequest]
    val request4 = result(requestPromise.future)
    assertResult("setServicesState")(request4.command)
    assertResult(Map("state" -> Seq(state3).toJson))(request4.arguments)

    uploader.stop()

    result(collections.State_ServiceStates.map(_.dropItems()))
    result(collections.State_UploadStatus.map(_.dropItems()))
  }
}