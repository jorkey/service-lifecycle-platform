package com.vyulabs.update.distribution.graphql.versions.client

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info.ServicesProfile
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class ClientDesiredVersionsTest extends TestEnvironment {
  behavior of "Client Desired Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, Utils.logException(log, "Uncatched exception", _))

  override def beforeAll() = {
    result(collections.Developer_ServiceProfiles.insert(ServicesProfile("common", Seq("service1", "service2"))))
  }

  it should "set/get client desired versions" in {
    assertResult((OK,
      ("""{"data":{"setClientDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation {
          setClientDesiredVersions (
            versions: [
               { service: "service1", version: { distribution: "test", developerBuild: [1,1,2], clientBuild: 0 } },
               { service: "service2", version: { distribution: "test", developerBuild: [2,1,4], clientBuild: 0 } }
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"service":"service1","version":{"distribution":"test","developerBuild":[1,1,2],"clientBuild":0}},{"service":"service2","version":{"distribution":"test","developerBuild":[2,1,4],"clientBuild":0}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          clientDesiredVersions {
             service
             version { distribution, developerBuild, clientBuild }
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"service":"service1","version":{"distribution":"test","developerBuild":[1,1,2],"clientBuild":0}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          clientDesiredVersions (services: ["service1"]) {
             service
             version { distribution, developerBuild, clientBuild }
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"setClientDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation {
          setClientDesiredVersions (
            versions: [
               { service: "service1" },
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"service":"service2","version":{"distribution":"test","developerBuild":[2,1,4],"clientBuild":0}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          clientDesiredVersions {
             service
             version { distribution, developerBuild, clientBuild }
          }
        }
      """)))
  }
}
