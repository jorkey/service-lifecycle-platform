package com.vyulabs.update.distribution.loaders

import java.util.Date
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.GraphqlArgument
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.client.HttpClientTestStub
import com.vyulabs.update.distribution.mongo.{ServiceStateDocument, UploadStatus, UploadStatusDocument}
import com.vyulabs.update.common.info.{DirectoryServiceState, DistributionServiceState, ServiceState}
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import spray.json.{JsonReader, enrichAny}
import spray.json.DefaultJsonProtocol._

import java.io.IOException
import scala.concurrent.{ExecutionContext, Future, Promise}

class ServicesStateUploadTest extends TestEnvironment {
  behavior of "Services State Upload"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val httpClient = new HttpClientTestStub()
  val distributionClient = new DistributionClient(distributionName, httpClient)

  it should "upload service states" in {
    val uploader = new StateUploader("distribution", collections, distributionDir, 1, distributionClient)
    uploader.start()

    val state1 = DistributionServiceState("distribution1", "instance1", DirectoryServiceState("service1", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 0))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(0, state1))).flatten)
    waitForSetServiceStates(Seq(state1)).success(true)

    Thread.sleep(100)
    assertResult(UploadStatusDocument("state.serviceStates", UploadStatus(Some(0), None)))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.serviceStates")).map(_.head)))))

    val state2 = DistributionServiceState("client2", "instance2", DirectoryServiceState("service2", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(1, state2))))
    waitForSetServiceStates(Seq(state2)).success(true)

    Thread.sleep(100)
    assertResult(UploadStatusDocument("state.serviceStates", UploadStatus(Some(1), None)))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.serviceStates")).map(_.head)))))

    uploader.stop()
    result(collections.State_ServiceStates.map(_.dropItems()).flatten)
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
  }

  it should "try to upload service states again after failure" in {
    val uploader = new StateUploader("distribution", collections, distributionDir, 2, distributionClient)
    uploader.start()

    val state1 = DistributionServiceState("distribution1", "instance1", DirectoryServiceState("service1", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 0))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(0, state1))).flatten)
    waitForSetServiceStates(Seq(state1)).failure(new IOException("upload error"))

    Thread.sleep(100)
    assertResult(UploadStatusDocument("state.serviceStates", UploadStatus(None, Some("upload error"))))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.serviceStates")).map(_.head)))))

    val state2 = DistributionServiceState("client2", "instance2", DirectoryServiceState("service2", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(1, state2))).flatten)
    waitForSetServiceStates(Seq(state1, state2)).success(true)

    val state3 = DistributionServiceState("client3", "instance3", DirectoryServiceState("service3", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))), None, None, None, None)))
    result(collections.State_ServiceStates.map(_.insert(ServiceStateDocument(2, state3))).flatten)
    waitForSetServiceStates(Seq(state3)).success(true)

    Thread.sleep(100)
    assertResult(UploadStatusDocument("state.serviceStates", UploadStatus(Some(2), None)))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.serviceStates")).map(_.head)))))

    uploader.stop()

    result(collections.State_ServiceStates.map(_.dropItems()).flatten)
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
  }

  def waitForSetServiceStates(states: Seq[DistributionServiceState]): Promise[Boolean] = {
    httpClient.waitForMutation("setServiceStates", Seq(GraphqlArgument("state" -> states.toJson)))
  }
}