package com.vyulabs.update.distribution.graphql.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import com.vyulabs.update.distribution.mongo.InstalledDesiredVersions
import sangria.macros.LiteralGraphQLStringContext
import spray.json._
import com.vyulabs.update.common.utils.JsonFormats._

import java.util.Date
import scala.concurrent.ExecutionContext

class SetStateInfoTest extends TestEnvironment {
  behavior of "Tested Versions Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override protected def beforeAll(): Unit = {
    result(collections.Distribution_ConsumersInfo.insert(DistributionConsumerInfo("distribution1", "common", Some("test"))))
  }

  it should "set tested versions" in {
    val graphqlContext = new GraphqlContext(UserInfo("distribution1", UserRole.Distribution), workspace)

    assertResult((OK,
      ("""{"data":{"setTestedVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setTestedVersions (
            versions: [
              { serviceName: "service1", version: "test-1.1.2" },
              { serviceName: "service2", version: "test-2.1.2" }
            ]
          )
        }
      """)))

    val date = new Date()

    assertResult(Seq(TestedDesiredVersions("common", Seq(
      DeveloperDesiredVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 2)))),
      DeveloperDesiredVersion("service2", DeveloperDistributionVersion("test", DeveloperVersion(Seq(2, 1, 2))))),
      Seq(TestSignature("distribution1", date)))))(result(collections.State_TestedVersions.find().map(_.map(v => TestedDesiredVersions(
        v.consumerProfile, v.versions, v.signatures.map(s => TestSignature(s.distributionName, date)))))))
    result(collections.State_TestedVersions.drop())
  }

  it should "set installed desired versions" in {
    val graphqlContext = new GraphqlContext(UserInfo("distribution1", UserRole.Distribution), workspace)

    assertResult((OK,
      ("""{"data":{"setInstalledDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setInstalledDesiredVersions (
            versions: [
               { serviceName: "service1", version: "test-1.1.1" },
               { serviceName: "service2", version: "test-2.1.1" }
            ]
          )
        }
      """)))

    result(collections.State_InstalledDesiredVersions.find().map(assertResult(Seq(InstalledDesiredVersions("distribution1", Seq(
      ClientDesiredVersion("service1", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))),
      ClientDesiredVersion("service2", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2, 1, 1)))))))))(_)))
    result(collections.State_InstalledDesiredVersions.drop())
  }

  it should "set services state" in {
    val graphqlContext = new GraphqlContext(UserInfo("distribution1", UserRole.Distribution), workspace)

    assertResult((OK,
      ("""{"data":{"setServiceStates":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        mutation ServicesState($$date: Date!) {
          setServiceStates (
            states: [
              { instanceId: "instance1", serviceName: "service1", directory: "dir",
                  service: { date: $$date, version: "test-1.2.3" }
              }
            ]
          )
        }
      """, variables = JsObject("date" -> new Date().toJson))))

    assertResult((OK,
      ("""{"data":{"setServiceStates":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        mutation ServicesState($$date: Date!) {
          setServiceStates (
            states: [
              { instanceId: "instance1", serviceName: "service1", directory: "dir",
                  service: { date: $$date, version: "test-1.2.4" }
              }
            ]
          )
        }
      """, variables = JsObject("date" -> new Date().toJson))))

    assertResult((OK,
      ("""{"data":{"serviceStates":[{"instance":{"instanceId":"instance1","service":{"version":"test-1.2.4"}}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          serviceStates (distribution: "distribution1", service: "service1") {
            instance {
              instanceId
              service {
                version
              }
            }
          }
        }
      """))
    )

    result(collections.State_ServiceStates.drop())
  }
}
