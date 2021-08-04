package com.vyulabs.update.distribution.graphql.provider

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.info._
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
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    val servicesProfileCollection = collections.Developer_ServiceProfiles

    result(servicesProfileCollection.insert(ServicesProfile("common", Seq("service1", "service2"))))
  }

  it should "set/get tested versions" in {
    val graphqlContext1 = GraphqlContext(Some(AccessToken("distribution1", Seq(AccountRole.Consumer), Some(Common.CommonServiceProfile))), workspace)

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

    val graphqlContext2 = GraphqlContext(Some(AccessToken("distribution2", Seq(AccountRole.Consumer), Some(Common.CommonServiceProfile))), workspace)

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

  it should "return error if no tested versions for the client's profile" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("distribution2", Seq(AccountRole.Consumer), Some(Common.CommonServiceProfile))), workspace)
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Desired versions for profile common are not tested","path":["developerDesiredVersions"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
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
      TestedDesiredVersions("common", Seq(
        DeveloperDesiredVersion("service1", DeveloperDistributionVersion("test", Seq(1, 1, 0)))),
        Seq(TestSignature("test-client", new Date())))))
    result(collections.Client_DesiredVersions.drop())
  }
}
