package com.vyulabs.update.distribution.http

import java.net.URLEncoder
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.distribution.TestEnvironment
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import spray.json._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 18.11.20.
  * Copyright FanDate, Inc.
  */
class RequestsTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Distribution Some Requests"

  val route = distribution.route

  it should "process graphql post request" in {
    Post("/graphql", """{ "query": "query { usersInfo(user:\"admin\") { user, roles } }" }""".parseJson) ~> addCredentials(adminHttpCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual """{"data":{"usersInfo":[{"user":"admin","roles":["Administrator"]}]}}"""
    }
  }

  it should "process graphql get request" in {
    Get(s"/graphql?query=" + URLEncoder.encode("""{ usersInfo(user:"admin") { user, roles } }""", "utf8")) ~> addCredentials(adminHttpCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual """{"data":{"usersInfo":[{"user":"admin","roles":["Administrator"]}]}}"""
    }
  }

  it should "process bad graphql post request" in {
    Post("/graphql", """{ "query": "{ badRequest }" }""".parseJson) ~> addCredentials(adminHttpCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.BadRequest
    }
  }

  it should "process bad graphql get request" in {
    Get(s"/graphql?query=" + URLEncoder.encode("""{ badRequest }""", "utf8")) ~> addCredentials(adminHttpCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.BadRequest
    }
  }

  it should "process change password request" in {
    Get(s"/graphql?query=" + URLEncoder.encode("""mutation { changePassword ( oldPassword: "admin", password: "password1" ) }""", "utf8")) ~> addCredentials(adminHttpCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    val newAdminClientCredentials = BasicHttpCredentials("admin", "password1")

    Post("/graphql", """{ "query": "query { usersInfo(user:\"admin\") { user, roles } }" }""".parseJson) ~> addCredentials(newAdminClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual """{"data":{"usersInfo":[{"user":"admin","roles":["Administrator"]}]}}"""
    }
  }
}
