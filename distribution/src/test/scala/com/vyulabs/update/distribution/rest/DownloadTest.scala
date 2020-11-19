package com.vyulabs.update.distribution.rest

import com.vyulabs.update.distribution.TestEnvironment
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 14.01.16.
  * Copyright FanDate, Inc.
  */
class DownloadTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Distribution"

  implicit val executionContext = executor

  val route = distribution.route

  it should "download files" in {
    Get("/download/aaa/bbb") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "qwe123"
    }
  }
}
