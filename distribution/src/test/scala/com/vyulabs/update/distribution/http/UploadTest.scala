package com.vyulabs.update.distribution.http

import java.io.File

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.common.distribution.DistributionWebPaths.imageField
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 20.11.20.
  * Copyright FanDate, Inc.
  */
class UploadTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Distribution Upload Requests"

  val route = distribution.route

  it should "upload developer version image" in {
    Post("/load/developer-version-image/service1/test-1.1.1", makeVersionMultipart()) ~> addCredentials(adminClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    checkVersionFileContent(distributionDir.getDeveloperVersionImageFile("service1", DeveloperDistributionVersion.parse("test-1.1.1")))
  }

  it should "upload client version image" in {
    Post("/load/client-version-image/service1/test-1.1.1_1", makeVersionMultipart()) ~> addCredentials(adminClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    checkVersionFileContent(distributionDir.getClientVersionImageFile("service1", ClientDistributionVersion.parse("test-1.1.1_1")))
  }

  it should "return error when illegal access" in {
    Post("/load/client-version-image/service1/test-1.1.1_1", makeVersionMultipart()) ~> addCredentials(distributionClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  it should "upload fault report" in {
    Post("/load/fault-report/fault1", makeFaultReportMultipart()) ~> addCredentials(distributionClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  def makeVersionMultipart(): Multipart.FormData.Strict = {
    Multipart.FormData(Multipart.FormData.BodyPart.Strict(imageField,
      HttpEntity(ContentTypes.`application/octet-stream`, "version image content".getBytes),
      Map("filename" -> "version.zip")))
  }

  def checkVersionFileContent(file: File): Unit = {
    assertResult("version image content")(IoUtils.readFileToBytes(file).map(new String(_, "utf8")).get)
  }

  def makeFaultReportMultipart(): Multipart.FormData.Strict = {
    Multipart.FormData(Multipart.FormData.BodyPart.Strict("fault-report",
      HttpEntity(ContentTypes.`application/octet-stream`, "fault report content".getBytes),
      Map("filename" -> "fault-report.zip")))
  }
}
