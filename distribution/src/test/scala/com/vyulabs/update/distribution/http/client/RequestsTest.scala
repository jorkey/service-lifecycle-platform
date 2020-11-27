package com.vyulabs.update.distribution.http.client

import java.net.URL
import java.util.Date

import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.client.{DistributionClient, HttpSyncClient}
import com.vyulabs.update.info.{BuildInfo, DeveloperVersionInfo, DirectoryServiceState, DistributionServiceState, ServiceState}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import distribution.mongo.{ServiceStateDocument}

import com.vyulabs.update.distribution.client.AdministratorGraphqlCoder._

import spray.json.DefaultJsonProtocol._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.11.20.
  * Copyright FanDate, Inc.
  */
class RequestsTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Own distribution client"

  val route = distribution.route

  var server = Http().newServerAt("0.0.0.0", 8081)
  server.bind(route)

  val httpClient = new HttpSyncClient(new URL("http://admin:admin@localhost:8081"), 1000, 1000)
  val administratorClient = new DistributionClient(distributionName, httpClient)

  override def beforeAll() = {
    val serviceStatesCollection = result(collections.State_ServiceStates)

    result(serviceStatesCollection.insert(ServiceStateDocument(0,
      DistributionServiceState(distributionName, "instance1", DirectoryServiceState("distribution", "directory1",
        ServiceState(date = new Date(), None, None, version =
          Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 2, 3))))), None, None, None, None))))))
  }

  it should "execute query request" in {
    assertResult(Some(ClientDistributionVersion.parse("test-1.2.3")))(administratorClient.getDistributionVersion())
  }

  it should "execute mutation request" in {
    assert(administratorClient.graphqlMutation(administratorMutations.addDeveloperVersionInfo(
      DeveloperVersionInfo("service1", DeveloperDistributionVersion.parse("test-1.2.3"),
        BuildInfo("author1", Seq("master"), new Date(), Some("comment"))))))
  }
}
