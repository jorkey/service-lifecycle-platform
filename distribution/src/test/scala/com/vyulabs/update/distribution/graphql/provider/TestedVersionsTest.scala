package com.vyulabs.update.distribution.graphql.provider

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.accounts.{ConsumerAccountInfo, ConsumerAccountProperties, UserAccountInfo}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.common.version.DeveloperDistributionVersion
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
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, Utils.logException(log, "Uncatched exception", _))

  override def beforeAll() = {
    val profileCollection = collections.Developer_ServiceProfiles

    result(profileCollection.insert(ServicesProfile("common", Seq("service1", "service2"))))
  }

  it should "set/get tested versions" in {
    val graphqlContext1 = GraphqlContext(Some(AccessToken("distribution1")),
      Some(ConsumerAccountInfo("distribution1", "Test Consumer Distribution 1", AccountRole.DistributionConsumer,
        ConsumerAccountProperties(Common.CommonConsumerProfile, "http://localhost:8001"))), workspace)

    assertResult((OK,
      ("""{"data":{"setTestedVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, graphqlContext1, graphql"""
        mutation {
          setTestedVersions (
            versions: [
               { service: "service1", version: { distribution: "test", build: [1,1,1] } },
               { service: "service2", version: { distribution: "test", build: [2,1,1] } }
            ]
          )
        }
      """)))

    val graphqlContext2 = GraphqlContext(Some(AccessToken("distribution2")),
      Some(ConsumerAccountInfo("distribution2", "Test Consumer Distribution 2", AccountRole.DistributionConsumer,
        ConsumerAccountProperties(Common.CommonConsumerProfile, "http://localhost:8002"))), workspace)

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[{"service":"service1","version":{"distribution":"test","build":[1,1,1]}},{"service":"service2","version":{"distribution":"test","build":[2,1,1]}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, graphqlContext2, graphql"""
        query DeveloperDesiredVersions($$testConsumer: String) {
          developerDesiredVersions(testConsumer: $$testConsumer) {
            service
            version { distribution, build }
          }
        }
      """, variables = JsObject("testConsumer" -> JsString("distribution1")))))

    result(collections.Developer_TestedVersions.drop())
  }

  it should "return empty list if no tested versions for the client's profile" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("distribution2")),
      Some(ConsumerAccountInfo("distribution2", "Test Consumer Distribution", AccountRole.DistributionConsumer,
        ConsumerAccountProperties(Common.CommonConsumerProfile, "http://localhost:8002"))), workspace)
    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, graphqlContext, graphql"""
        query DeveloperDesiredVersions($$testConsumer: String) {
          developerDesiredVersions(testConsumer: $$testConsumer) {
            service
            version { distribution, build }
          }
        }
      """, variables = JsObject("testConsumer" -> JsString("distribution1")))))
  }

  it should "return error if client required preliminary testing has personal desired versions" in {
    result(collections.Developer_TestedVersions.insert(
      TestedVersions("common", "test-client", Seq(
        DeveloperDesiredVersion("service1", DeveloperDistributionVersion("test", Seq(1, 1, 0)))),
        new Date())))
    result(collections.Client_DesiredVersions.drop())
  }
}
