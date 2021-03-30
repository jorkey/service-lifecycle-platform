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
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 20.11.20.
  * Copyright FanDate, Inc.
  */
class UploadTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Own distribution client upload"

  val route = distribution.route

  var server = Http().newServerAt("0.0.0.0", 8083).adaptSettings(s => s.withTransparentHeadRequests(true))
  server.bind(route)

  def httpUpload(): Unit = {
    val builderClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl(new URL("http://builder:builder@localhost:8083"))), FiniteDuration(15, TimeUnit.SECONDS))
    val updaterClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl(new URL("http://updater:updater@localhost:8083"))), FiniteDuration(15, TimeUnit.SECONDS))
    val distribClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl(new URL("http://distribution:distribution@localhost:8083"))), FiniteDuration(15, TimeUnit.SECONDS))
    upload(builderClient, updaterClient, distribClient)
  }

  def akkaHttpUpload(): Unit = {
    val builderClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient(new URL("http://builder:builder@localhost:8083"))), FiniteDuration(15, TimeUnit.SECONDS))
    val updaterClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient(new URL("http://updater:updater@localhost:8083"))), FiniteDuration(15, TimeUnit.SECONDS))
    val distribClient = new SyncDistributionClient(
      new DistributionClient(new AkkaHttpClient(new URL("http://distribution:distribution@localhost:8083"))), FiniteDuration(15, TimeUnit.SECONDS))
    upload(builderClient, updaterClient, distribClient)
  }

  override def dbName = super.dbName + "-client"

  def upload[Source[_]](builderClient: SyncDistributionClient[Source], serviceClient: SyncDistributionClient[Source], distribClient: SyncDistributionClient[Source]): Unit = {
    it should "upload developer version image" in {
      val file = File.createTempFile("load-test", "zip")
      assert(IoUtils.writeBytesToFile(file, "developer version content".getBytes("utf8")))
      assert(builderClient.uploadDeveloperVersionImage("service1", DeveloperDistributionVersion.parse("test-1.1.1"), file))
      assertResult(25)(
        distributionDir.getDeveloperVersionImageFile("service1", DeveloperDistributionVersion.parse("test-1.1.1")).length())
    }

    it should "upload client version image" in {
      val file = File.createTempFile("load-test", "zip")
      assert(IoUtils.writeBytesToFile(file, "client version content".getBytes("utf8")))
      assert(builderClient.uploadClientVersionImage("service1", ClientDistributionVersion.parse("test-1.1.1_1"), file))
      assertResult(22)(
        distributionDir.getClientVersionImageFile("service1", ClientDistributionVersion.parse("test-1.1.1_1")).length())
    }

    it should "upload fault report" in {
      val file = File.createTempFile("load-test", "zip")
      assert(IoUtils.writeBytesToFile(file, "fault report content".getBytes("utf8")))

      assert(serviceClient.uploadFaultReport("fault1", file))
      assertResult(20)(
        distributionDir.getFaultReportFile("fault1").length())

      assert(distribClient.uploadFaultReport("fault2", file))
      assertResult(20)(
        distributionDir.getFaultReportFile("fault2").length())
    }
  }

  "Http upload" should behave like httpUpload()
  "Akka http upload" should behave like akkaHttpUpload()
}
