package com.vyulabs.update.distribution.graphql.versions.client

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info.{DistributionConsumerInfo, DistributionConsumerProfile}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class ClientDesiredVersionsTest extends TestEnvironment {
  behavior of "Client Desired Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    result(collections.Distribution_ConsumerProfiles.insert(DistributionConsumerProfile("common", Seq("service1", "service2"))))
    result(collections.Distribution_ConsumersInfo.insert(DistributionConsumerInfo("client2", "common", None)))
  }

  it should "set/get client desired versions" in {
    assertResult((OK,
      ("""{"data":{"setClientDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation {
          setClientDesiredVersions (
            versions: [
               { serviceName: "service1", version: { distributionName: "test", build: { build: "1.1.2", clientBuild: 0 } } },
               { serviceName: "service2", version: { distributionName: "test", build: { build: "2.1.4", clientBuild: 0 } } }
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"serviceName":"service1","version":{"distributionName":"test","build":{"build":"1.1.2","clientBuild":0}}},{"serviceName":"service2","version":{"distributionName":"test","build":{"build":"2.1.4","clientBuild":0}}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          clientDesiredVersions {
             serviceName
             version {
               distributionName
               build {
                 build
                 clientBuild
               }
             }
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"serviceName":"service1","version":{"distributionName":"test","build":{"build":"1.1.2","clientBuild":0}}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          clientDesiredVersions (services: ["service1"]) {
             serviceName
             version {
               distributionName
               build {
                 build
                 clientBuild
               }
             }
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"setClientDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation {
          setClientDesiredVersions (
            versions: [
               { serviceName: "service1" },
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"serviceName":"service2","version":{"distributionName":"test","build":{"build":"2.1.4","clientBuild":0}}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          clientDesiredVersions {
             serviceName
             version {
               distributionName
               build {
                 build
                 clientBuild
               }
             }
          }
        }
      """)))
  }
}
