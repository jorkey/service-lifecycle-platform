package com.vyulabs.update.distribution.loaders

import java.io.IOException
import java.util.Date

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.mongodb.client.model.Filters
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{DirectoryServiceState, DistributionServiceState, ServiceState}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import distribution.loaders.StateUploader
import distribution.mongo.{ServiceStateDocument, UploadStatus, UploadStatusDocument}
import spray.json.{JsValue, enrichAny}

import scala.concurrent.{ExecutionContext, Future, Promise}

class ServicesStateUploadTest extends TestEnvironment {
  behavior of "Services State Upload"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  case class GraphqlMutationRequest(command: String, arguments: Map[String, JsValue])

  var promise = Promise[GraphqlMutationRequest]
  var result = Future.successful()

  def graphqlMutationRequest(command: String, arguments: Map[String, JsValue]): Future[Unit] = {
    synchronized {
      promise.success(GraphqlMutationRequest(command, arguments))
      promise = Promise[GraphqlMutationRequest]
      result
    }
  }

  it should "upload service states" in {
    val uploader = new StateUploader("distribution", collections, distributionDir, 1, graphqlMutationRequest,
      (_, _) => Future.failed(new IOException("Not expected")))
    uploader.start()

    val state1 = DistributionServiceState("distribution1", "instance1", DirectoryServiceState("service1", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 0))))), None, None, None, None)))
    testAction(() => result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(0, state1))).flatten),  Seq(state1))

    Thread.sleep(100)
    assertResult(UploadStatusDocument("state.serviceStates", UploadStatus(Some(0), None)))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.serviceStates")).map(_.head)))))

    val state2 = DistributionServiceState("client2", "instance2", DirectoryServiceState("service2", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    testAction(() => result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(1, state2)))), Seq(state2))

    Thread.sleep(100)
    assertResult(UploadStatusDocument("state.serviceStates", UploadStatus(Some(1), None)))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.serviceStates")).map(_.head)))))

    uploader.stop()
    result(collections.State_ServiceStates.map(_.dropItems()).flatten)
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
  }

  it should "try to upload service states again after failure" in {
    val uploader = new StateUploader("distribution", collections, distributionDir, 2, graphqlMutationRequest,
      (_, _) => Future.failed(new IOException("Not expected")))
    promise = Promise[GraphqlMutationRequest]
    result = Future.failed(new IOException("upload error"))
    uploader.start()

    val state1 = DistributionServiceState("distribution1", "instance1", DirectoryServiceState("service1", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 0))))), None, None, None, None)))
    testAction(() => result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(0, state1))).flatten), Seq(state1))

    Thread.sleep(100)
    assertResult(UploadStatusDocument("state.serviceStates", UploadStatus(None, Some("upload error"))))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.serviceStates")).map(_.head)))))

    val state2 = DistributionServiceState("client2", "instance2", DirectoryServiceState("service2", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    testAction(() => result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(1, state2))).flatten), Seq(state1, state2))

    testAction(() => synchronized { result = Future.successful() }, Seq(state1, state2))

    val state3 = DistributionServiceState("client3", "instance3", DirectoryServiceState("service3", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    testAction(() => result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(2, state3))).flatten), Seq(state3))

    Thread.sleep(100)
    assertResult(UploadStatusDocument("state.serviceStates", UploadStatus(Some(2), None)))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.serviceStates")).map(_.head)))))

    uploader.stop()

    result(collections.State_ServiceStates.map(_.dropItems()).flatten)
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
  }

  def testAction(action: () => Unit, states: Seq[DistributionServiceState]): Unit = {
    val promise = this.promise
    action()
    val request = result(promise.future)
    assertResult("setServicesState")(request.command)
    assertResult(Map("state" -> states.toJson))(request.arguments)
  }
}