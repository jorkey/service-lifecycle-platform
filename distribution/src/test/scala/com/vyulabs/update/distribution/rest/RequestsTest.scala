package com.vyulabs.update.distribution.rest

import java.net.URLEncoder

import akka.http.scaladsl.model._
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

  it should "process graphql post request" in {
    Post("/graphql", """{ "query": "{ userInfo { name, role } }" }""".parseJson) ~> addCredentials(adminClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual """{"data":{"userInfo":{"name":"admin","role":"Administrator"}}}"""
    }
  }

  it should "process graphql get request" in {
    Get(s"/graphql?query=" + URLEncoder.encode("""{ userInfo { name, role } }""", "utf8")) ~> addCredentials(adminClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual """{"data":{"userInfo":{"name":"admin","role":"Administrator"}}}"""
    }
  }

  it should "process bad graphql post request" in {
    Post("/graphql", """{ "query": "{ badRequest }" }""".parseJson) ~> addCredentials(adminClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.BadRequest
    }
  }

  it should "process bad graphql get request" in {
    Get(s"/graphql?query=" + URLEncoder.encode("""{ badRequest }""", "utf8")) ~> addCredentials(adminClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.BadRequest
    }
  }

  it should "return html on other requests" in {
    Get("/qwerty") ~> addCredentials(adminClientCredentials) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      assert (responseAs[String] startsWith "<!doctype html>")
    }
  }
}
