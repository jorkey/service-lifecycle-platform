package com.vyulabs.update.distribution.graphql.service

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{DistributionServiceState, ServiceState}
import distribution.users.{UserInfo, UserRole}
import com.vyulabs.update.version.DeveloperDistributionVersion
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._
import com.vyulabs.update.utils.Utils.DateJson._

import scala.concurrent.ExecutionContext

class StateInfoTest extends TestEnvironment {
  behavior of "State Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = new GraphqlContext("distribution", versionHistoryConfig, collections, distributionDir, UserInfo("user1", UserRole.Distribution))

  it should "set/get own service state" in {
    assertResult((OK,
      ("""{"data":{"setServicesState":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        mutation ServicesState($$date: Date!) {
          setServicesState (
            state: [
              {
                instanceId: "instance1",
                serviceName: "service1",
                directory: "dir",
                service: {
                  date: $$date,
                  version: "test-1.2.3"
                }
              }
            ]
          )
        }
      """, variables = JsObject("date" -> new Date().toJson))))

    assertResult((OK,
      ("""{"data":{"serviceState":[{"service":{"version":"test-1.2.3"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        query {
          serviceState (instance: "instance1", service: "service1", directory: "dir") {
            service  {
              version
            }
          }
        }
      """, None, variables = JsObject("directory" -> JsString(ownServicesDir.getCanonicalPath)))))
  }
}
