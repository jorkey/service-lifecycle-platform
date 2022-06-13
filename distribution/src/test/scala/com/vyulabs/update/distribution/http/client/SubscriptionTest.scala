package com.vyulabs.update.distribution.http.client

import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder._
import com.vyulabs.update.common.distribution.client.graphql.DeveloperGraphqlCoder.developerSubscriptions
import com.vyulabs.update.common.distribution.client.graphql.UpdaterGraphqlCoder._
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient}
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.version._
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.client.AkkaHttpClient
import spray.json.DefaultJsonProtocol._

import java.util.Date
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.11.20.
  * Copyright FanDate, Inc.
  */
class SubscriptionTest extends TestEnvironment(true) with ScalatestRouteTest {
  behavior of "Subscription requests"

  val route = distribution.route

  var server = Http().newServerAt("0.0.0.0", 8081).adaptSettings(s => s.withTransparentHeadRequests(true))
  server.bind(route)

  val stateDate = new Date()

  override def dbName = super.dbName + "-client"

  override def beforeAll() = {
    result(collections.State_Instances.insert(
      DistributionInstanceState(distributionName, "instance1", DirectoryServiceState("consumer", "directory1",
        InstanceState(time = stateDate, None, None, version =
          Some(ClientDistributionVersion(distributionName, Seq(1, 2, 3), 0)), None, None, None, None)))))
  }

  def subRequests(): Unit = {
    val developerClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl("http://developer:developer@localhost:8081")), FiniteDuration(15, TimeUnit.SECONDS))

    it should "execute SSE subscription request" in {
      val source = developerClient.graphqlRequestSSE(developerSubscriptions.testSubscription())
      var line = Option.empty[String]
      val spaces = new String((0 to 100).map(_ => ' ').toArray)
      for (i <- 0 to 1000) {
        val line = spaces + i.toString
        assertResult(line)(source.get.next().get)
      }
      assert(source.get.next().isEmpty)
    }
  }

  def akkaSubRequests(): Unit = {
    val adminClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient("http://developer:developer@localhost:8081")), FiniteDuration(15, TimeUnit.SECONDS))

    def testLines(source: AkkaHttpClient.AkkaSource[String]): Unit = {
      val lines = result(source.runFold(Seq.empty[String])(_ :+ _))
      val spaces = new String((0 to 100).map(_ => ' ').toArray)
      for (i <- 0 to 1000) {
        val line = spaces + i.toString
        assert(lines(i) == line)
      }
    }

    it should "execute SSE subscription request" in {
      val source = adminClient.graphqlRequestSSE(developerSubscriptions.testSubscription()).get
      testLines(source)
    }

    it should "execute WS subscription request" in {
      val source = adminClient.graphqlRequestWS(developerSubscriptions.testSubscription()).get
      testLines(source)
    }
  }

  "Http subscription requests" should behave like subRequests()
  "Akka http subscription requests" should behave like akkaSubRequests()
}