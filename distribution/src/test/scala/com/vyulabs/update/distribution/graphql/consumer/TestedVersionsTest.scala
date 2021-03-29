package com.vyulabs.update.distribution.graphql.consumer

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.version.{DistributionVersion, Version}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.util.Date
import scala.concurrent.ExecutionContext

class TestedVersionsTest extends TestEnvironment {
  behavior of "Tested Versions Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    val consumerProfileCollection = collections.Distribution_ConsumerProfiles
    val clientInfoCollection = collections.Distribution_ConsumersInfo

    result(consumerProfileCollection.insert(DistributionConsumerProfile("common", Seq("service1", "service2"))))

    result(clientInfoCollection.insert(DistributionConsumerInfo("distribution1", "common", None)))
    result(clientInfoCollection.insert(DistributionConsumerInfo("distribution2", "common", Some("distribution1"))))
  }

  it should "set/get tested versions" in {
    val graphqlContext1 = GraphqlContext(Some(AccessToken("distribution1", Seq(UserRole.Distribution))), workspace)

    assertResult((OK,
      ("""{"data":{"setTestedVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, graphqlContext1, graphql"""
        mutation {
          setTestedVersions (
            versions: [
               { serviceName: "service1", version: { distributionName: "test", build: "1.1.1" } },
               { serviceName: "service2", version: { distributionName: "test", build: "2.1.1" } }
            ]
          )
        }
      """)))

    val graphqlContext2 = GraphqlContext(Some(AccessToken("distribution2", Seq(UserRole.Distribution))), workspace)

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[{"serviceName":"service1","version":{"distributionName":"test","build":"1.1.1"}},{"serviceName":"service2","version":{"distributionName":"test","build":"2.1.1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, graphqlContext2, graphql"""
        query {
          developerDesiredVersions {
            serviceName
            version {
               distributionName
               build
             }
          }
        }
      """)))

    result(collections.State_TestedVersions.drop())
  }

  it should "return error if no tested versions for the client's profile" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("distribution2", Seq(UserRole.Distribution))), workspace)
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Desired versions for profile common are not tested by anyone","path":["developerDesiredVersions"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, graphqlContext, graphql"""
        query {
          developerDesiredVersions {
            serviceName
            version {
               distributionName
               build
             }
          }
        }
      """)))
  }

  it should "return error if client required preliminary testing has personal desired versions" in {
    result(collections.State_TestedVersions.insert(
      TestedDesiredVersions("common", Seq(
        DeveloperDesiredVersion("service1", DistributionVersion("test", Version(Seq(1, 1, 0))))),
        Seq(TestSignature("test-client", new Date())))))
    result(collections.Client_DesiredVersions.drop())
  }
}
