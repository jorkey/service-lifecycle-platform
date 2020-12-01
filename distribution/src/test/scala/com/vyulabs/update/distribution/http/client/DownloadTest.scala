package com.vyulabs.update.distribution.http.client

import java.io.File
import java.net.URL

import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.client.{HttpJavaClient, JavaDistributionClient}
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import distribution.client.{AkkaDistributionClient, HttpAkkaClient}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 01.12.20.
  * Copyright FanDate, Inc.
  */
class DownloadTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Own distribution client download"

  val route = distribution.route

  var server = Http().newServerAt("0.0.0.0", 8082).adaptSettings(s => s.withTransparentHeadRequests(true))
  server.bind(route)

  val adminClient = new JavaDistributionClient(distributionName, new HttpJavaClient(new URL("http://admin:admin@localhost:8082")))
  val serviceClient = new JavaDistributionClient(distributionName, new HttpJavaClient(new URL("http://service1:service1@localhost:8082")))
  val distribClient = new AkkaDistributionClient(distributionName, new HttpAkkaClient(new URL("http://distribution1:distribution1@localhost:8082")))

  it should "download developer version image" in {
    IoUtils.writeBytesToFile(distributionDir.getDeveloperVersionImageFile("service1", DeveloperDistributionVersion.parse("test-1.1.1")),
      "qwerty123".getBytes("utf8"))
    assert(adminClient.downloadDeveloperVersionImage("service1",
      DeveloperDistributionVersion.parse("test-1.1.1"), File.createTempFile("load-test", "zip")))
    result(distribClient.downloadDeveloperVersionImage("service1",
      DeveloperDistributionVersion.parse("test-1.1.1"), File.createTempFile("load-test", "zip")))
  }

  it should "download client version image" in {
    IoUtils.writeBytesToFile(distributionDir.getClientVersionImageFile("service1", ClientDistributionVersion.parse("test-1.1.1_1")),
      "qwerty456".getBytes("utf8"))
    assert(serviceClient.downloadClientVersionImage("service1",
      ClientDistributionVersion.parse("test-1.1.1_1"), File.createTempFile("load-test", "zip")))
  }
}
