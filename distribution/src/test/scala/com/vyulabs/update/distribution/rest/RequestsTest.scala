package com.vyulabs.update.distribution.rest

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, RawHeader}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.distribution.TestEnvironment
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 18.11.20.
  * Copyright FanDate, Inc.
  */
class RequestsTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Distribution Some Requests"

  val route = distribution.route

  it should "response to ping" in {
    Get("/ping") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "pong"
    }
  }

  val adminCred = BasicHttpCredentials("admin", "admin")

  it should "process graphql post request" in {
    Post("/graphql", """{ "query": "{ userInfo { name, role } }" }""".parseJson) ~> addCredentials(adminCred) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual """{"data":{"userInfo":{"name":"admin","role":"Administrator"}}}"""
    }
  }
}
