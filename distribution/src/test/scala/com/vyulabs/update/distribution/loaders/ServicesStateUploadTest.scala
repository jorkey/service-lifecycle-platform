package com.vyulabs.update.distribution.loaders

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.GraphqlArgument
import com.vyulabs.update.common.info.{DirectoryInstanceState, DistributionInstanceState, AddressedInstanceState, InstanceState}
import com.vyulabs.update.common.utils.Utils
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
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, Utils.logException(log, "Uncatched exception", _))

  val httpClient = new HttpClientTestStub[AkkaSource]()
  val distributionClient = new DistributionClient(httpClient)

  distributionClient.login()
  waitForLogin().success("token123")

  it should "upload service states" in {
    val uploader = new StateUploader("consumer", collections, distributionDir, distributionClient)
    uploader.start()

    collections.State_Instances.setSequence(0)

    val state1 = DistributionInstanceState("distribution1", "instance1", DirectoryInstanceState("service1", "directory",
      InstanceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(1, 1, 0), 0)), None, None, None, None)))
    result(collections.State_Instances.insert(state1))
    waitForSetServiceStates(Seq(state1)).success(true)

    Thread.sleep(500)
    assertResult(UploadStatusDocument("state.instances", Some(1), None))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.instances")).map(_.head)))))

    val state2 = DistributionInstanceState("client2", "instance2", DirectoryInstanceState("service2", "directory",
      InstanceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(1, 1, 1), 0)), None, None, None, None)))
    result(collections.State_Instances.insert(state2))
    waitForSetServiceStates(Seq(state2)).success(true)

    Thread.sleep(500)
    assertResult(UploadStatusDocument("state.instances", Some(2), None))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.instances")).map(_.head)))))

    uploader.stop()
    clear()
  }

  it should "try to upload service states again after failure" in {
    val uploader = new StateUploader("consumer", collections, distributionDir, distributionClient)
    uploader.start()

    val state1 = DistributionInstanceState("distribution1", "instance1", DirectoryInstanceState("service1", "directory",
      InstanceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(1, 1, 0), 0)), None, None, None, None)))
    result(collections.State_Instances.insert(state1))
    waitForSetServiceStates(Seq(state1)).failure(new IOException("upload error"))

    Thread.sleep(500)
    assertResult(UploadStatusDocument("state.instances", None, Some("upload error")))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.instances")).map(_.head)))))

    val state2 = DistributionInstanceState("client2", "instance2", DirectoryInstanceState("service2", "directory",
      InstanceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(1, 1, 1), 0)), None, None, None, None)))
    result(collections.State_Instances.insert(state2))
    waitForSetServiceStates(Seq(state1, state2)).success(true)

    val state3 = DistributionInstanceState("client3", "instance3", DirectoryInstanceState("service3", "directory",
      InstanceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(1, 1, 1), 0)), None, None, None, None)))
    result(collections.State_Instances.insert(state3))
    waitForSetServiceStates(Seq(state3)).success(true)

    Thread.sleep(500)
    assertResult(UploadStatusDocument("state.instances", Some(3), None))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.instances")).map(_.head)))))

    uploader.stop()
    clear()
  }

  def waitForLogin(): Promise[String] = {
    httpClient.waitForMutation("login", Seq(GraphqlArgument("account" -> "test"), GraphqlArgument("password" -> "test")))
  }

  private def waitForSetServiceStates(states: Seq[DistributionInstanceState]): Promise[Boolean] = {
    httpClient.waitForMutation("setInstanceStates", Seq(GraphqlArgument("states" ->
      states.map(s => AddressedInstanceState(s.instance, s.service, s.directory, s.state)).toJson, "[AddressedInstanceStateInput!]")))
  }

  private def clear(): Unit = {
    result(collections.State_Instances.drop())
    collections.State_Instances.setSequence(0)
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
  }
}