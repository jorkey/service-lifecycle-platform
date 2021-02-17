package com.vyulabs.update.distribution.http.client

import java.io.File
import java.net.URL
import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.client.AkkaHttpClient
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}

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
      new DistributionClient(new HttpClientImpl(new URL("http://admin:admin@localhost:8082"))), FiniteDuration(15, TimeUnit.SECONDS))
    val serviceClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl(new URL("http://service1:service1@localhost:8082"))), FiniteDuration(15, TimeUnit.SECONDS))
    val distribClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl(new URL("http://distribution1:distribution1@localhost:8082"))), FiniteDuration(15, TimeUnit.SECONDS))
    download(adminClient, serviceClient, distribClient)
  }

  def akkaHttpDownload(): Unit = {
    val adminClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient(new URL("http://admin:admin@localhost:8082"))), FiniteDuration(15, TimeUnit.SECONDS))
    val serviceClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient(new URL("http://service1:service1@localhost:8082"))), FiniteDuration(15, TimeUnit.SECONDS))
    val distribClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient(new URL("http://distribution1:distribution1@localhost:8082"))), FiniteDuration(15, TimeUnit.SECONDS))
    download(adminClient, serviceClient, distribClient)
  }

  override def dbName = super.dbName + "-client"

  def download[Source[_]](adminClient: SyncDistributionClient[Source], serviceClient: SyncDistributionClient[Source], distribClient: SyncDistributionClient[Source]): Unit = {
    it should "download developer version image" in {
      IoUtils.writeBytesToFile(distributionDir.getDeveloperVersionImageFile("service1", DeveloperDistributionVersion.parse("test-1.1.1")),
        "qwerty123".getBytes("utf8"))
      assert(adminClient.downloadDeveloperVersionImage("service1",
        DeveloperDistributionVersion.parse("test-1.1.1"), File.createTempFile("load-test", "zip")))
      assert(distribClient.downloadDeveloperVersionImage("service1",
        DeveloperDistributionVersion.parse("test-1.1.1"), File.createTempFile("load-test", "zip")))
    }

    it should "download client version image" in {
      IoUtils.writeBytesToFile(distributionDir.getClientVersionImageFile("service1", ClientDistributionVersion.parse("test-1.1.1_1")),
        "qwerty456".getBytes("utf8"))
      assert(serviceClient.downloadClientVersionImage("service1",
        ClientDistributionVersion.parse("test-1.1.1_1"), File.createTempFile("load-test", "zip")))
    }
  }

  "Http download" should behave like httpDownload()
  "Akka http download" should behave like akkaHttpDownload()
}
