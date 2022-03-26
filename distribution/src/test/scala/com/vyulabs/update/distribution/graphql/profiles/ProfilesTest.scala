package com.vyulabs.update.distribution.graphql.profiles

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class ProfilesTest extends TestEnvironment {
  behavior of "Service profiles"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, Utils.logException(log, "Uncatched exception", _))

  it should "add/get/remove service profiles" in {
    assertResult((OK,
      ("""{"data":{"addServicesProfile":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          addServicesProfile (
            profile: "consumer-distribution",
            services: [ "service1", "service2", "service3" ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"serviceProfiles":[{"profile":"consumer-distribution","services":["service1","service2","service3"]}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        query {
          serviceProfiles {
             profile,
             services
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"removeServicesProfile":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          removeServicesProfile (
            profile: "consumer-distribution"
          )
        }
      """)))
  }
}
