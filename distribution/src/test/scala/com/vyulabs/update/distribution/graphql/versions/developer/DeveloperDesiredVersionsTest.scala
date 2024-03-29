package com.vyulabs.update.distribution.graphql.versions.developer

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

class DeveloperDesiredVersionsTest extends TestEnvironment {
  behavior of "Developer Desired Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, Utils.logException(log, "Uncatched exception", _))

  override def beforeAll() = {
    result(collections.Developer_ServiceProfiles.insert(ServicesProfile("common", Seq("service1", "service2"))))
  }

  it should "set/get developer desired versions" in {
    assertResult((OK,
      ("""{"data":{"setDeveloperDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation {
          setDeveloperDesiredVersions (
            versions: [
               { service: "service1", version: { distribution: "test", build: [ 1, 1, 2 ] } },
               { service: "service2", version: { distribution: "test", build: [ 2, 1, 4 ] } }
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[{"service":"service1","version":{"distribution":"test","build":[1,1,2]}},{"service":"service2","version":{"distribution":"test","build":[2,1,4]}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          developerDesiredVersions {
             service
             version { distribution, build }
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"setDeveloperDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation {
          setDeveloperDesiredVersions (
            versions: [
               { service: "service2"}
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[{"service":"service1","version":{"distribution":"test","build":[1,1,2]}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          developerDesiredVersions {
             service
             version { distribution, build }
          }
        }
      """)))
  }
}
