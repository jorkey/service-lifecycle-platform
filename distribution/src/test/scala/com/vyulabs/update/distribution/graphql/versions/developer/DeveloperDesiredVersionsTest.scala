package com.vyulabs.update.distribution.graphql.versions.developer

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info.{DistributionConsumerInfo, DistributionConsumerProfile}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class DeveloperDesiredVersionsTest extends TestEnvironment {
  behavior of "Developer Desired Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    result(collections.Distribution_ConsumerProfiles.insert(DistributionConsumerProfile("common", Seq("service1", "service2"))))
    result(collections.Distribution_ConsumersInfo.insert(DistributionConsumerInfo("client2", "common", None)))
  }

  it should "set/get developer desired versions" in {
    assertResult((OK,
      ("""{"data":{"setDeveloperDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation {
          setDeveloperDesiredVersions (
            versions: [
               { serviceName: "service1", version: { distributionName: "test", build: [ 1, 1, 2 ] } },
               { serviceName: "service2", version: { distributionName: "test", build: [ 2, 1, 4 ] } }
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[{"serviceName":"service1","version":{"distributionName":"test","build":[1,1,2]}},{"serviceName":"service2","version":{"distributionName":"test","build":[2,1,4]}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          developerDesiredVersions {
             serviceName
             version { distributionName, build }
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"setDeveloperDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation {
          setDeveloperDesiredVersions (
            versions: [
               { serviceName: "service2"}
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[{"serviceName":"service1","version":{"distributionName":"test","build":[1,1,2]}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          developerDesiredVersions {
             serviceName
             version { distributionName, build }
          }
        }
      """)))
  }
}
