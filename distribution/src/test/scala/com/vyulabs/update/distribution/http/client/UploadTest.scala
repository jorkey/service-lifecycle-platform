package com.vyulabs.update.distribution.http.client

import java.io.File
import java.net.URL

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.distribution.DistributionWebPaths.versionImageField
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.client.{HttpJavaClient, JavaDistributionClient}
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import distribution.client.{AkkaDistributionClient, HttpAkkaClient}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 20.11.20.
  * Copyright FanDate, Inc.
  */
class UploadTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Own distribution client upload"

  val route = distribution.route

  var server = Http().newServerAt("0.0.0.0", 8083).adaptSettings(s => s.withTransparentHeadRequests(true))
  server.bind(route)

  val adminClient = new JavaDistributionClient(distributionName, new HttpJavaClient(new URL("http://admin:admin@localhost:8083")))
  val serviceClient = new JavaDistributionClient(distributionName, new HttpJavaClient(new URL("http://service1:service1@localhost:8083")))
  val distribClient = new AkkaDistributionClient(distributionName, new HttpAkkaClient(new URL("http://distribution1:distribution1@localhost:8083")))

  it should "upload developer version image" in {
    val file = File.createTempFile("load-test", "zip")
    assert(IoUtils.writeBytesToFile(file, "developer version content".getBytes("utf8")))
    assert(adminClient.uploadDeveloperVersionImage("service1", DeveloperDistributionVersion.parse("test-1.1.1"), file))
  }

  it should "upload client version image" in {
    val file = File.createTempFile("load-test", "zip")
    assert(IoUtils.writeBytesToFile(file, "client version content".getBytes("utf8")))
    assert(adminClient.uploadClientVersionImage("service1", ClientDistributionVersion.parse("test-1.1.1_1"), file))
  }

  it should "upload fault report" in {
    val file = File.createTempFile("load-test", "zip")
    assert(IoUtils.writeBytesToFile(file, "fault report content".getBytes("utf8")))

    assert(serviceClient.uploadFaultReport("fault1", file))

    result(distribClient.uploadFaultReport("fault1", file))
  }
}
