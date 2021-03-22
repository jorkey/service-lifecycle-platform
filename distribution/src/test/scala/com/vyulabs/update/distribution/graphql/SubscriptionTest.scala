package com.vyulabs.update.distribution.graphql

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import com.vyulabs.update.common.info._
import com.vyulabs.update.distribution.TestEnvironment
import sangria.macros.LiteralGraphQLStringContext

import java.util.Date

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.11.20.
  * Copyright FanDate, Inc.
  */
class SubscriptionTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Subscription graphql requests"

  val route = distribution.route

  var server = Http().newServerAt("0.0.0.0", 8081).adaptSettings(s => s.withTransparentHeadRequests(true))
  server.bind(route)

  val stateDate = new Date()

  override def dbName = super.dbName

  it should "test subscription" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("admin", Seq(UserRole.Administrator))), workspace)
    val subscribeResponse = result(graphql.executeSubscriptionQuery(GraphqlSchema.SchemaDefinition, graphqlContext, graphql"""
      subscription {
        testSubscription
      }
    """))

    val logSource = subscribeResponse.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val logInput = logSource.runWith(TestSink.probe[ServerSentEvent])

    println(logInput.requestNext())
    println(logInput.requestNext())
    println(logInput.requestNext())
    println(logInput.requestNext())
    println(logInput.requestNext())
  }
}