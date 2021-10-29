package com.vyulabs.update.distribution.http.client

import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.common.common.JWT
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient}
import com.vyulabs.update.common.info.AccessToken
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.client.AkkaHttpClient

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 01.12.20.
  * Copyright FanDate, Inc.
  */
class DownloadTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Own distribution client download"

  val route = distribution.route
  var server = Http().newServerAt("0.0.0.0", 8082).adaptSettings(s => s.withTransparentHeadRequests(true))
  server.bind(route)

  def httpDownload(): Unit = {
    val adminClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl("http://admin:admin@localhost:8082")),
      FiniteDuration(15, TimeUnit.SECONDS))
    val updaterClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl("http://localhost:8082", Some(JWT.encodeAccessToken(AccessToken("updater"), config.jwtSecret)))),
      FiniteDuration(15, TimeUnit.SECONDS))
    val consumerClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl("http://localhost:8082", Some(JWT.encodeAccessToken(AccessToken("consumer"), config.jwtSecret)))),
      FiniteDuration(15, TimeUnit.SECONDS))
    download(adminClient, updaterClient, consumerClient)
  }

  def akkaHttpDownload(): Unit = {
    val adminClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient("http://admin:admin@localhost:8082")),
      FiniteDuration(15, TimeUnit.SECONDS))
    val updaterClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient("http://localhost:8082", Some(JWT.encodeAccessToken(AccessToken("updater"), config.jwtSecret)))),
      FiniteDuration(15, TimeUnit.SECONDS))
    val consumerClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient("http://localhost:8082", Some(JWT.encodeAccessToken(AccessToken("consumer"), config.jwtSecret)))),
      FiniteDuration(15, TimeUnit.SECONDS))
    download(adminClient, updaterClient, consumerClient)
  }

  override def dbName = super.dbName + "-client"

  def download[Source[_]](adminClient: SyncDistributionClient[Source], serviceClient: SyncDistributionClient[Source], consumerClient: SyncDistributionClient[Source]): Unit = {
    it should "download developer version image" in {
      IoUtils.writeBytesToFile(distributionDir.getDeveloperVersionImageFile("service1", DeveloperDistributionVersion.parse("test-1.1.1")),
        "qwerty123".getBytes("utf8"))
      val outFile = File.createTempFile("load-test", "zip")
      assert(adminClient.downloadDeveloperVersionImage("service1",
        DeveloperDistributionVersion.parse("test-1.1.1"), outFile))
      assertResult(9)(outFile.length())
      outFile.delete()
      assert(consumerClient.downloadDeveloperVersionImage("service1",
        DeveloperDistributionVersion.parse("test-1.1.1"), outFile))
      assertResult(9)(outFile.length())
    }

    it should "download client version image" in {
      IoUtils.writeBytesToFile(distributionDir.getClientVersionImageFile("service1", ClientDistributionVersion.parse("test-1.1.1_1")),
        "qwerty456".getBytes("utf8"))
      val outFile = File.createTempFile("load-test", "zip")
      assert(serviceClient.downloadClientVersionImage("service1",
        ClientDistributionVersion.parse("test-1.1.1_1"), outFile))
      assertResult(9)(outFile.length())
    }
  }

  "Http download" should behave like httpDownload()
  "Akka http download" should behave like akkaHttpDownload()
}
