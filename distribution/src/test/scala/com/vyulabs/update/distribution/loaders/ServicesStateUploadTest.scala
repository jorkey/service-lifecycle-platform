package com.vyulabs.update.distribution.loaders

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.GraphqlArgument
import com.vyulabs.update.common.info.{DirectoryServiceState, DistributionServiceState, ServiceState}
import com.vyulabs.update.common.version.ClientDistributionVersion
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.client.AkkaHttpClient.AkkaSource
import com.vyulabs.update.distribution.client.HttpClientTestStub
import com.vyulabs.update.distribution.mongo.UploadStatusDocument
import spray.json.DefaultJsonProtocol._
import spray.json.enrichAny

import java.io.IOException
import java.util.Date
import scala.concurrent.{ExecutionContext, Promise}

class ServicesStateUploadTest extends TestEnvironment {
  behavior of "Services State Upload"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val httpClient = new HttpClientTestStub[AkkaSource]()
  val distributionClient = new DistributionClient(httpClient)

  distributionClient.login()
  waitForLogin().success("token123")

  it should "upload service states" in {
    val uploader = new StateUploader("consumer", collections, distributionDir, distributionClient)
    uploader.start()

    val state1 = DistributionServiceState("distribution1", "instance1", DirectoryServiceState("service1", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(1, 1, 0), 0)), None, None, None, None)))
    result(collections.State_ServiceStates.insert(state1))
    waitForSetServiceStates(Seq(state1)).success(true)

    Thread.sleep(500)
    assertResult(UploadStatusDocument("state.serviceStates", Some(1), None))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.serviceStates")).map(_.head)))))

    val state2 = DistributionServiceState("client2", "instance2", DirectoryServiceState("service2", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(1, 1, 1), 0)), None, None, None, None)))
    result(collections.State_ServiceStates.insert(state2))
    waitForSetServiceStates(Seq(state2)).success(true)

    Thread.sleep(500)
    assertResult(UploadStatusDocument("state.serviceStates", Some(2), None))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.serviceStates")).map(_.head)))))

    uploader.stop()
    clear()
  }

  it should "try to upload service states again after failure" in {
    val uploader = new StateUploader("consumer", collections, distributionDir, distributionClient)
    uploader.start()

    val state1 = DistributionServiceState("distribution1", "instance1", DirectoryServiceState("service1", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(1, 1, 0), 0)), None, None, None, None)))
    result(collections.State_ServiceStates.insert(state1))
    waitForSetServiceStates(Seq(state1)).failure(new IOException("upload error"))

    Thread.sleep(500)
    assertResult(UploadStatusDocument("state.serviceStates", None, Some("upload error")))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.serviceStates")).map(_.head)))))

    val state2 = DistributionServiceState("client2", "instance2", DirectoryServiceState("service2", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(1, 1, 1), 0)), None, None, None, None)))
    result(collections.State_ServiceStates.insert(state2))
    waitForSetServiceStates(Seq(state1, state2)).success(true)

    val state3 = DistributionServiceState("client3", "instance3", DirectoryServiceState("service3", "directory",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(1, 1, 1), 0)), None, None, None, None)))
    result(collections.State_ServiceStates.insert(state3))
    waitForSetServiceStates(Seq(state3)).success(true)

    Thread.sleep(500)
    assertResult(UploadStatusDocument("state.serviceStates", Some(3), None))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.serviceStates")).map(_.head)))))

    uploader.stop()
    clear()
  }

  def waitForLogin(): Promise[String] = {
    httpClient.waitForMutation("login", Seq(GraphqlArgument("account" -> "test"), GraphqlArgument("password" -> "test")))
  }

  private def waitForSetServiceStates(states: Seq[DistributionServiceState]): Promise[Boolean] = {
    httpClient.waitForMutation("setServiceStates", Seq(GraphqlArgument("states" -> states.toJson)))
  }

  private def clear(): Unit = {
    result(collections.State_ServiceStates.drop())
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
    result(result(collections.Sequences).drop())
  }
}