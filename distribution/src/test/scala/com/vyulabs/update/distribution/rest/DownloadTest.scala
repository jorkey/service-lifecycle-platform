package com.vyulabs.update.distribution.rest

import com.vyulabs.update.distribution.TestEnvironment
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 20.11.20.
  * Copyright FanDate, Inc.
  */
class DownloadTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Distribution Download Requests"

  val route = distribution.route

  it should "download developer version image" in {
    IoUtils.writeBytesToFile(distributionDir.getDeveloperVersionImageFile("service1", DeveloperDistributionVersion.parse("test-1.1.1")),
      "qwerty123".getBytes("utf8"))
    Get("/developer-version-image/service1/test-1.1.1") ~> addCredentials(adminClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "qwerty123"
    }
  }

  it should "download client version image" in {
    IoUtils.writeBytesToFile(distributionDir.getClientVersionImageFile("service1", ClientDistributionVersion.parse("test-1.1.1")),
      "qwerty456".getBytes("utf8"))
    Get("/client-version-image/service1/test-1.1.1") ~> addCredentials(serviceClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "qwerty456"
    }
  }

  it should "return error when illegal access" in {
    IoUtils.writeBytesToFile(distributionDir.getDeveloperVersionImageFile("service1", DeveloperDistributionVersion.parse("test-1.1.1")),
      "qwerty123".getBytes("utf8"))
    Get("/developer-version-image/service1/test-1.1.1") ~> addCredentials(serviceClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }
}
