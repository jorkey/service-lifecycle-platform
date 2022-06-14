package com.vyulabs.update.distribution.http.client

import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.common.accounts.UserAccountProperties
import com.vyulabs.update.common.common.JWT
import com.vyulabs.update.common.config.{GitConfig, Repository}
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder._
import com.vyulabs.update.common.distribution.client.graphql.ConsumerGraphqlCoder.{distributionMutations, distributionQueries}
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
    result(collections.State_Instances.insert(
      DistributionInstanceState(distributionName, "instance1", DirectoryInstanceState("consumer", "directory1",
        InstanceState(time = stateDate, None, None, version =
          Some(ClientDistributionVersion(distributionName, Seq(1, 2, 3), 0)), None, None, None, None)))))
  }

  def httpRequests(): Unit = {
    val adminClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl("http://admin:admin@localhost:8081")),
        FiniteDuration(15, TimeUnit.SECONDS))
    val updaterClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl("http://localhost:8081", Some(JWT.encodeAccessToken(AccessToken("updater"), config.jwtSecret)))),
        FiniteDuration(15, TimeUnit.SECONDS))
    val consumerClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl("http://localhost:8081", Some(JWT.encodeAccessToken(AccessToken("consumer"), config.jwtSecret)))),
        FiniteDuration(15, TimeUnit.SECONDS))
    requests(adminClient, updaterClient, consumerClient)
  }

  def akkaHttpRequests(): Unit = {
    val adminClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient("http://admin:admin@localhost:8081")),
        FiniteDuration(15, TimeUnit.SECONDS))
    val updaterClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient("http://localhost:8081", Some(JWT.encodeAccessToken(AccessToken("updater"), config.jwtSecret)))),
        FiniteDuration(15, TimeUnit.SECONDS))
    val consumerClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient("http://localhost:8081", Some(JWT.encodeAccessToken(AccessToken("consumer"), config.jwtSecret)))),
        FiniteDuration(15, TimeUnit.SECONDS))
    requests(adminClient, updaterClient, consumerClient)
  }

  def requests[Source[_]](adminClient: SyncDistributionClient[Source], updaterClient: SyncDistributionClient[Source],
                          consumerClient: SyncDistributionClient[Source]): Unit = {
    it should "execute add/remove account requests" in {
      assert(adminClient.graphqlRequest(administratorMutations.addUserAccount("account1", "account1", AccountRole.Developer,
        "account1", UserAccountProperties(None, Seq.empty))).getOrElse(false))

      assert(!adminClient.graphqlRequest(administratorMutations.addUserAccount("account1", "account1", AccountRole.Developer,
        "account1", UserAccountProperties(None, Seq.empty))).getOrElse(false))

      assert(adminClient.graphqlRequest(administratorMutations.removeAccount("account1")).getOrElse(false))
    }

    it should "execute distribution provider requests" in {
      assert(adminClient.graphqlRequest(administratorMutations.addProvider("distribution2",
          "http://localhost/graphql", "token", None, Some(true), None)).getOrElse(false))

      assertResult(Some(Seq(DistributionProviderInfo("distribution2", "http://localhost/graphql", "token", None, Some(true), None))))(
        adminClient.graphqlRequest(administratorQueries.getDistributionProvidersInfo()))

      assert(adminClient.graphqlRequest(administratorMutations.removeProvider("distribution2")).getOrElse(false))
    }

    it should "execute distribution consumer profiles requests" in {
      assert(adminClient.graphqlRequest(
        administratorMutations.addServicesProfile("common", Seq("service1", "service2"))).getOrElse(false))

      assert(adminClient.graphqlRequest(
        administratorMutations.removeServicesProfile("common")).getOrElse(false))
    }

    it should "execute developer version requests" in {
      val date = new Date()

      assert(adminClient.graphqlRequest(
        administratorMutations.addServicesProfile("common", Seq("service1"))).getOrElse(false))

      assert(adminClient.graphqlRequest(administratorMutations.addDeveloperVersionInfo(
        DeveloperVersionInfo.from("service1", DeveloperDistributionVersion.parse("test-1.2.3"),
          BuildInfo("author1", Seq(Repository("test", GitConfig("git://dummy", "master", None), None)), date, "comment")))).getOrElse(false))

      assertResult(Some(Seq(DeveloperVersionInfo.from("service1", DeveloperDistributionVersion.parse("test-1.2.3"),
        BuildInfo("author1", Seq(Repository("test", GitConfig("git://dummy", "master", None), None)), date, "comment")))))(
        adminClient.graphqlRequest(administratorQueries.getDeveloperVersionsInfo("service1", Some("test"),
          Some(DeveloperVersion(Build.parse("1.2.3"))))))

      assert(adminClient.graphqlRequest(administratorMutations.removeDeveloperVersion("service1",
        DeveloperDistributionVersion.parse("test-1.2.3"))).getOrElse(false))

      assert(adminClient.graphqlRequest(
        administratorMutations.setDeveloperDesiredVersions(Seq(DeveloperDesiredVersionDelta("service1", Some(DeveloperDistributionVersion.parse("test-1.2.3"))))))
        .getOrElse(false))

      assertResult(Some(Seq(DeveloperDesiredVersion("service1", DeveloperDistributionVersion.parse("test-1.2.3")))))(adminClient.graphqlRequest(
        administratorQueries.getDeveloperDesiredVersions(services = Seq("service1"))))

      assertResult(Some(Seq(DeveloperDesiredVersion("service1", DeveloperDistributionVersion.parse("test-1.2.3")))))(
        consumerClient.graphqlRequest(distributionQueries.getDeveloperDesiredVersions(services = Seq("service1"))))

      assert(adminClient.graphqlRequest(
        administratorMutations.removeServicesProfile("common")).getOrElse(false))
    }

    it should "execute client version requests" in {
      val date = new Date()

      assert(adminClient.graphqlRequest(administratorMutations.addClientVersionInfo(
        ClientVersionInfo.from("service1", ClientDistributionVersion.parse("test-1.2.3_1"),
          BuildInfo("author1", Seq(Repository("test", GitConfig("git://dummy", "master", None), None)), date, "comment"), InstallInfo("account1", date)))).getOrElse(false))

      assertResult(Some(Seq(ClientVersionInfo.from("service1", ClientDistributionVersion.parse("test-1.2.3_1"),
        BuildInfo("author1", Seq(Repository("test", GitConfig("git://dummy", "master", None), None)), date, "comment"), InstallInfo("account1", date)))))(
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
      assert(consumerClient.graphqlRequest(
        distributionMutations.setInstalledDesiredVersions(Seq(ClientDesiredVersion("service1", ClientDistributionVersion.parse("test-1.1.1"))))).getOrElse(false))

      assertResult(Some(Seq(ClientDesiredVersion("service1", ClientDistributionVersion.parse("test-1.1.1")))))(adminClient.graphqlRequest(
        administratorQueries.getInstalledDesiredVersions("consumer", Seq("service1"))))
    }

    it should "execute tested versions requests" in {
      assert(
        consumerClient.graphqlRequest(distributionMutations.setTestedVersions(Seq(DeveloperDesiredVersion("service1", DeveloperDistributionVersion.parse("test-1.2.3"))))).getOrElse(false))
    }

    it should "execute service states requests" in {
      assert(updaterClient.graphqlRequest(updaterMutations.setInstanceStates(Seq(AddressedInstanceState("instance1", "service1", "directory1",
        InstanceState(stateDate, None, None, Some(ClientDistributionVersion.parse(s"${distribution}-1.2.1")), None, None, None, None))))).getOrElse(false))

      assert(consumerClient.graphqlRequest(distributionMutations.setInstanceStates(Seq(AddressedInstanceState("instance1", "service1", "directory1",
        InstanceState(stateDate, None, None, Some(ClientDistributionVersion.parse(s"distribution-3.2.1")), None, None, None, None))))).getOrElse(false))

      assertResult(Some(Seq(DistributionInstanceState(distributionName, "instance1", "service1", "directory1",
        InstanceState(stateDate, None, None, Some(ClientDistributionVersion.parse(s"${distribution}-1.2.1")), None, None, None, None)))))(
        adminClient.graphqlRequest(administratorQueries.getInstanceStates(Some(distributionName), Some("service1"), Some("instance1"), Some("directory1"))))

      assertResult(Some(Seq(DistributionInstanceState("consumer", "instance1", "service1", "directory1",
        InstanceState(stateDate, None, None, Some(ClientDistributionVersion.parse(s"distribution-3.2.1")), None, None, None, None)))))(
        adminClient.graphqlRequest(administratorQueries.getInstanceStates(Some("consumer"), Some("service1"), Some("instance1"), Some("directory1"))))
    }

    it should "execute service log requests" in {
      assert(updaterClient.graphqlRequest(updaterMutations.addLogs("service1", "instance1", "process1", None, "directory1",
        Seq(LogLine(new Date(), "INFO", "-", "log line", None)))).getOrElse(false))
    }

    it should "execute fault report requests" in {
      assert(updaterClient.graphqlRequest(updaterMutations.addFaultReportInfo(ServiceFaultReport("fault1", FaultInfo(stateDate, "instance1", "service1", None, "directory", InstanceState(stateDate, None, None, None, None, None, None, None), Seq()),
        Seq(FileInfo("fault1.info", stateDate, 1234), FileInfo("core", stateDate, 123456789))))).getOrElse(false))

      assert(consumerClient.graphqlRequest(distributionMutations.addFaultReportInfo(ServiceFaultReport("fault2", FaultInfo(stateDate, "instance1", "service1", None, "directory", InstanceState(stateDate, None, None, None, None, None, None, None), Seq()),
        Seq(FileInfo("fault2.info", stateDate, 1234), FileInfo("core", stateDate, 123456789))))).getOrElse(false))

      assertResult(Some(Seq(DistributionFaultReport(distributionName, "fault1", FaultInfo(stateDate, "instance1", "service1", None, "directory", InstanceState(stateDate, None, None, None, None, None, None, None), Seq()),
          Seq(FileInfo("fault1.info", stateDate, 1234), FileInfo("core", stateDate, 123456789))))))(
        adminClient.graphqlRequest(administratorQueries.getFaultReportsInfo(Some(distributionName), Some("service1"), Some(2))))

      assertResult(Some(Seq(DistributionFaultReport("consumer", "fault2", FaultInfo(stateDate, "instance1", "service1", None, "directory", InstanceState(stateDate, None, None, None, None, None, None, None), Seq()),
          Seq(FileInfo("fault2.info", stateDate, 1234), FileInfo("core", stateDate, 123456789))))))(
        adminClient.graphqlRequest(administratorQueries.getFaultReportsInfo(Some("consumer"), Some("service1"), Some(2))))

      result(collections.Faults_ReportsInfo.drop())
    }
  }

  "Http requests" should behave like httpRequests()
  "Akka http requests" should behave like akkaHttpRequests()
}