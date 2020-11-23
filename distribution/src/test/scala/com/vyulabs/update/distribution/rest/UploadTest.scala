package com.vyulabs.update.distribution.rest

import java.io.File

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 20.11.20.
  * Copyright FanDate, Inc.
  */
class UploadTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Distribution Upload Requests"

  val route = distribution.route

  it should "upload developer version image" in {
    Post("/developer-version-image/service1/test-1.1.1", makeVersionMultipart()) ~> addCredentials(adminClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "Complete"
    }

    checkVersionFileContent(distributionDir.getDeveloperVersionImageFile("service1", DeveloperDistributionVersion.parse("test-1.1.1")))
  }

  it should "upload client version image" in {
    Post("/client-version-image/service1/test-1.1.1_1", makeVersionMultipart()) ~> addCredentials(adminClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "Complete"
    }

    checkVersionFileContent(distributionDir.getClientVersionImageFile("service1", ClientDistributionVersion.parse("test-1.1.1_1")))
  }

  it should "return error when illegal access" in {
    Post("/client-version-image/service1/test-1.1.1_1", makeVersionMultipart()) ~> addCredentials(distributionClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  it should "upload fault report" in {

  }

  def makeVersionMultipart(): Multipart.FormData.Strict = {
    Multipart.FormData(Multipart.FormData.BodyPart.Strict("version-image",
      HttpEntity(ContentTypes.`application/octet-stream`, "version image content".getBytes),
      Map("filename" -> "version.zip")))
  }

  def checkVersionFileContent(file: File): Unit = {
    assertResult("version image content")(IoUtils.readFileToBytes(file).map(new String(_, "utf8")).get)
  }

  def makeFaultReportMultipart(): Multipart.FormData.Strict = {

    Multipart.FormData(Multipart.FormData.BodyPart.Strict("fault-report",
      HttpEntity(ContentTypes.`application/octet-stream`, "version image content".getBytes),
      Map("filename" -> "version.zip")))
  }
}
