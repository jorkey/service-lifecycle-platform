package com.vyulabs.update.distribution.http.client

import java.net.URL

import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.client.{AdministratorClient, HttpClient}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.11.20.
  * Copyright FanDate, Inc.
  */
class RequestsTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Own distribution client"

  val route = distribution.route

  var server = Http().newServerAt("0.0.0.0", 8081)
  server.bind(route)

  //val httpClient = new HttpClient(new URL("http://admin:admin@localhost:8081"), 1000, 1000)
  val httpClient = new HttpClient(new URL("http://localhost:8081"), 1000, 1000)
  val administratorClient = new AdministratorClient(distributionName, httpClient)

  it should "make simple request" in {
    println(administratorClient.getDistributionVersion())
  }
}
