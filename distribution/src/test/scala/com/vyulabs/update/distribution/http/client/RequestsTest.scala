package com.vyulabs.update.distribution.http.client

import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.common.config.{GitConfig, SourceConfig}
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder._
import com.vyulabs.update.common.distribution.client.graphql.BuilderGraphqlCoder.builderMutations
import com.vyulabs.update.common.distribution.client.graphql.DeveloperGraphqlCoder.developerSubscriptions
import com.vyulabs.update.common.distribution.client.graphql.DistributionGraphqlCoder.{distributionMutations, distributionQueries}
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
class RequestsTest extends TestEnvironment(true) with ScalatestRouteTest {
  behavior of "Own distribution client graphql requests"

  val route = distribution.route

  var server = Http().newServerAt("0.0.0.0", 8081).adaptSettings(s => s.withTransparentHeadRequests(true))
  server.bind(route)

  val stateDate = new Date()

  override def dbName = super.dbName + "-client"

  override def beforeAll() = {
    val serviceStatesCollection = collections.State_ServiceStates

    result(serviceStatesCollection.insert(
      DistributionServiceState(distributionName, "instance1", DirectoryServiceState("distribution", "directory1",
        ServiceState(time = stateDate, None, None, version =
          Some(ClientDistributionVersion(distributionName, Seq(1, 2, 3), 0)), None, None, None, None)))))
  }

  def httpRequests(): Unit = {
    val adminClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl("http://admin:admin@localhost:8081")), FiniteDuration(15, TimeUnit.SECONDS))
    val updaterClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl("http://updater:updater@localhost:8081")), FiniteDuration(15, TimeUnit.SECONDS))
    val builderClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl("http://builder:builder@localhost:8081")), FiniteDuration(15, TimeUnit.SECONDS))
    val distribClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl("http://distribution:distribution@localhost:8081")), FiniteDuration(15, TimeUnit.SECONDS))
    requests(adminClient, updaterClient, builderClient, distribClient)
  }

  def akkaHttpRequests(): Unit = {
    val adminClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient("http://admin:admin@localhost:8081")), FiniteDuration(15, TimeUnit.SECONDS))
    val updaterClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient("http://updater:updater@localhost:8081")), FiniteDuration(15, TimeUnit.SECONDS))
    val builderClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient("http://builder:builder@localhost:8081")), FiniteDuration(15, TimeUnit.SECONDS))
    val distribClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient("http://distribution:distribution@localhost:8081")), FiniteDuration(15, TimeUnit.SECONDS))
    requests(adminClient, updaterClient, builderClient, distribClient)
  }

  def requests[Source[_]](adminClient: SyncDistributionClient[Source], updaterClient: SyncDistributionClient[Source],
                          builderClient: SyncDistributionClient[Source], distribClient: SyncDistributionClient[Source]): Unit = {
    it should "execute add/remove user requests" in {
      assert(adminClient.graphqlRequest(administratorMutations.addUser("user1", true, "user1", "user1", Seq(UserRole.Developer))).getOrElse(false))

      assert(!adminClient.graphqlRequest(administratorMutations.addUser("user1", true, "user1", "user1", Seq(UserRole.Developer))).getOrElse(false))

      assert(adminClient.graphqlRequest(administratorMutations.removeUser("user1")).getOrElse(false))
    }

    it should "execute distribution provider requests" in {
      assert(adminClient.graphqlRequest(administratorMutations.addProvider("distribution2",
          "http://localhost/graphql", None)).getOrElse(false))

      assertResult(Some(Seq(DistributionProviderInfo("distribution2", "http://localhost/graphql", None))))(
        adminClient.graphqlRequest(administratorQueries.getDistributionProvidersInfo()))

      assert(adminClient.graphqlRequest(administratorMutations.removeProvider("distribution2")).getOrElse(false))
    }

    it should "execute distribution consumer profiles requests" in {
      assert(adminClient.graphqlRequest(
        administratorMutations.addServicesProfile("common", Seq("service1", "service2"))).getOrElse(false))

      assert(adminClient.graphqlRequest(
        administratorMutations.removeServicesProfile("common")).getOrElse(false))
    }

    it should "execute distribution consumer requests" in {
      assert(adminClient.graphqlRequest(
        administratorMutations.addConsumer("distribution", "common", None)).getOrElse(false))

      assertResult(Some(Seq(DistributionConsumerInfo("distribution", "common", None))))(
        adminClient.graphqlRequest(administratorQueries.getDistributionConsumersInfo()))

      assert(adminClient.graphqlRequest(
        administratorMutations.removeConsumer("distribution")).getOrElse(false))
    }

    it should "execute developer version requests" in {
      val date = new Date()

      assert(adminClient.graphqlRequest(
        administratorMutations.addServicesProfile("common", Seq("service1"))).getOrElse(false))
      assert(adminClient.graphqlRequest(
        administratorMutations.addConsumer("distribution", "common", None)).getOrElse(false))

      assert(builderClient.graphqlRequest(builderMutations.addDeveloperVersionInfo(
        DeveloperVersionInfo.from("service1", DeveloperDistributionVersion.parse("test-1.2.3"),
          BuildInfo("author1", Seq(SourceConfig("test", GitConfig("git://dummy", "master", None))), date, "comment")))).getOrElse(false))

      assertResult(Some(Seq(DeveloperVersionInfo.from("service1", DeveloperDistributionVersion.parse("test-1.2.3"),
        BuildInfo("author1", Seq(SourceConfig("test", GitConfig("git://dummy", "master", None))), date, "comment")))))(
        adminClient.graphqlRequest(administratorQueries.getDeveloperVersionsInfo("service1", Some("test"),
          Some(DeveloperVersion(Build.parse("1.2.3"))))))

      assert(adminClient.graphqlRequest(administratorMutations.removeDeveloperVersion("service1",
        DeveloperDistributionVersion.parse("test-1.2.3"))).getOrElse(false))

      assert(adminClient.graphqlRequest(
        administratorMutations.setDeveloperDesiredVersions(Seq(DeveloperDesiredVersionDelta("service1", Some(DeveloperDistributionVersion.parse("test-1.2.3"))))))
        .getOrElse(false))

      assertResult(Some(Seq(DeveloperDesiredVersion("service1", DeveloperDistributionVersion.parse("test-1.2.3")))))(adminClient.graphqlRequest(
        administratorQueries.getDeveloperDesiredVersions(Seq("service1"))))

      assertResult(Some(Seq(DeveloperDesiredVersion("service1", DeveloperDistributionVersion.parse("test-1.2.3")))))(
        distribClient.graphqlRequest(distributionQueries.getDeveloperDesiredVersions(Seq("service1"))))

      assert(adminClient.graphqlRequest(
        administratorMutations.removeConsumer("distribution")).getOrElse(false))
      assert(adminClient.graphqlRequest(
        administratorMutations.removeServicesProfile("common")).getOrElse(false))
    }

    it should "execute client version requests" in {
      val date = new Date()

      assert(builderClient.graphqlRequest(builderMutations.addClientVersionInfo(
        ClientVersionInfo.from("service1", ClientDistributionVersion.parse("test-1.2.3_1"),
          BuildInfo("author1", Seq(SourceConfig("test", GitConfig("git://dummy", "master", None))), date, "comment"), InstallInfo("user1", date)))).getOrElse(false))

      assertResult(Some(Seq(ClientVersionInfo.from("service1", ClientDistributionVersion.parse("test-1.2.3_1"),
        BuildInfo("author1", Seq(SourceConfig("test", GitConfig("git://dummy", "master", None))), date, "comment"), InstallInfo("user1", date)))))(
        adminClient.graphqlRequest(administratorQueries.getClientVersionsInfo("service1", Some("test"),
          Some(ClientVersion.parse("1.2.3_1")))))

      assert(adminClient.graphqlRequest(administratorMutations.removeClientVersion("service1",
        ClientDistributionVersion.parse("test-1.2.3_1"))).getOrElse(false))

      assert(adminClient.graphqlRequest(
        administratorMutations.setClientDesiredVersions(Seq(ClientDesiredVersionDelta("service1",
          Some(ClientDistributionVersion.parse("test-1.2.3_1")))))).getOrElse(false))

      assertResult(Some(Seq(ClientDesiredVersion("service1", ClientDistributionVersion.parse("test-1.2.3_1")))))(adminClient.graphqlRequest(
        administratorQueries.getClientDesiredVersions(Seq("service1"))))

      assertResult(Some(List(ClientDesiredVersion("service1", ClientDistributionVersion.parse("test-1.2.3_1")))))(
        updaterClient.graphqlRequest(updaterQueries.getClientDesiredVersions(Seq("service1"))))
    }

    it should "execute installed versions requests" in {
      assert(distribClient.graphqlRequest(
        distributionMutations.setInstalledDesiredVersions(Seq(ClientDesiredVersion("service1", ClientDistributionVersion.parse("test-1.1.1"))))).getOrElse(false))

      assertResult(Some(Seq(ClientDesiredVersion("service1", ClientDistributionVersion.parse("test-1.1.1")))))(adminClient.graphqlRequest(
        administratorQueries.getInstalledDesiredVersions("distribution", Seq("service1"))))
    }

    it should "execute tested versions requests" in {
      assert(adminClient.graphqlRequest(
        administratorMutations.addConsumer("distribution", "common", None)).getOrElse(false))
      assert(
        distribClient.graphqlRequest(distributionMutations.setTestedVersions(Seq(DeveloperDesiredVersion("service1", DeveloperDistributionVersion.parse("test-1.2.3"))))).getOrElse(false))
      assert(adminClient.graphqlRequest(
        administratorMutations.removeConsumer("distribution")).getOrElse(false))
    }

    it should "execute service states requests" in {
      assert(updaterClient.graphqlRequest(updaterMutations.setServiceStates(Seq(InstanceServiceState("instance1", "service1", "directory1",
        ServiceState(stateDate, None, None, Some(ClientDistributionVersion.parse(s"${distribution}-1.2.1")), None, None, None, None))))).getOrElse(false))

      assert(distribClient.graphqlRequest(distributionMutations.setServiceStates(Seq(InstanceServiceState("instance1", "service1", "directory1",
        ServiceState(stateDate, None, None, Some(ClientDistributionVersion.parse(s"distribution-3.2.1")), None, None, None, None))))).getOrElse(false))

      assertResult(Some(Seq(DistributionServiceState(distributionName, InstanceServiceState("instance1", "service1", "directory1",
        ServiceState(stateDate, None, None, Some(ClientDistributionVersion.parse(s"${distribution}-1.2.1")), None, None, None, None))))))(
        adminClient.graphqlRequest(administratorQueries.getServiceStates(Some(distributionName), Some("service1"), Some("instance1"), Some("directory1"))))

      assertResult(Some(Seq(DistributionServiceState("distribution", InstanceServiceState("instance1", "service1", "directory1",
        ServiceState(stateDate, None, None, Some(ClientDistributionVersion.parse(s"distribution-3.2.1")), None, None, None, None))))))(
        adminClient.graphqlRequest(administratorQueries.getServiceStates(Some("distribution"), Some("service1"), Some("instance1"), Some("directory1"))))
    }

    it should "execute service log requests" in {
      assert(updaterClient.graphqlRequest(updaterMutations.addServiceLogs("service1", "instance1", "process1", None, "directory1",
        Seq(LogLine(new Date(), "INFO", "-", "log line", None)))).getOrElse(false))
    }

    it should "execute fault report requests" in {
      assert(updaterClient.graphqlRequest(updaterMutations.addFaultReportInfo(ServiceFaultReport("fault1", FaultInfo(stateDate, "instance1", "service1", "directory", "common", ServiceState(stateDate, None, None, None, None, None, None, None), Seq()), Seq("fault1.info", "core")))).getOrElse(false))

      assert(distribClient.graphqlRequest(distributionMutations.addFaultReportInfo(ServiceFaultReport("fault2", FaultInfo(stateDate, "instance1", "service1", "directory", "common", ServiceState(stateDate, None, None, None, None, None, None, None), Seq()), Seq("fault2.info", "core")))).getOrElse(false))

      assertResult(Some(Seq(DistributionFaultReport(distributionName, ServiceFaultReport("fault1", FaultInfo(stateDate, "instance1", "service1", "directory", "common", ServiceState(stateDate, None, None, None, None, None, None, None), Seq()), Seq("fault1.info", "core"))))))(
        adminClient.graphqlRequest(administratorQueries.getFaultReportsInfo(Some(distributionName), Some("service1"), Some(2))))

      assertResult(Some(Seq(DistributionFaultReport("distribution", ServiceFaultReport("fault2", FaultInfo(stateDate, "instance1", "service1", "directory", "common", ServiceState(stateDate, None, None, None, None, None, None, None), Seq()), Seq("fault2.info", "core"))))))(
        adminClient.graphqlRequest(administratorQueries.getFaultReportsInfo(Some("distribution"), Some("service1"), Some(2))))

      result(collections.State_FaultReportsInfo.drop())
    }
  }

  def subRequests(): Unit = {
    val developerClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl("http://developer:developer@localhost:8081")), FiniteDuration(15, TimeUnit.SECONDS))

    it should "execute SSE subscription request" in {
      val source = developerClient.graphqlRequestSSE(developerSubscriptions.testSubscription())
      var line = Option.empty[String]
      do {
        line = source.get.next()
        line.foreach(println(_))
      } while (line.isDefined)
    }
  }

  def akkaSubRequests(): Unit = {
    val adminClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient("ws://developer:developer@localhost:8081")), FiniteDuration(15, TimeUnit.SECONDS))

    it should "execute SSE subscription request" in {
      val source = adminClient.graphqlRequestSSE(developerSubscriptions.testSubscription()).get
      result(source.map(println(_)).run())
    }

    it should "execute WS subscription request" in {
      val source = adminClient.graphqlRequestWS(developerSubscriptions.testSubscription()).get
      result(source.map(println(_)).run())
    }
  }

  "Http requests" should behave like httpRequests()
  "Akka http requests" should behave like akkaHttpRequests()

  "Http subscription requests" should behave like subRequests()
  "Akka http subscription requests" should behave like akkaSubRequests()
}