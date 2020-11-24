package com.vyulabs.update.distribution.graphql.distribution

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{ClientDesiredVersion, DeveloperDesiredVersion, TestSignature, TestedDesiredVersions}
import com.vyulabs.update.info.{UserInfo, UserRole}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.{DistributionClientInfoDocument, InstalledDesiredVersionsDocument, TestedDesiredVersionsDocument}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._
import com.vyulabs.update.utils.Utils.DateJson._

import scala.concurrent.ExecutionContext

class SetStateInfoTest extends TestEnvironment {
  behavior of "Tested Versions Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override protected def beforeAll(): Unit = {
    result(collections.Developer_DistributionClientsInfo.map(_.insert(DistributionClientInfoDocument(DistributionClientInfo("distribution1", DistributionClientConfig("common", Some("test")))))).flatten)
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

    result(collections.State_TestedVersions.map(v => result(v.find().map(_.map(v => TestedDesiredVersionsDocument(TestedDesiredVersions(
      v.versions.profileName, v.versions.versions, v.versions.signatures.map(s => TestSignature(s.distributionName, date))))))
      .map(assertResult(_)(Seq(TestedDesiredVersionsDocument(TestedDesiredVersions("common", Seq(
        DeveloperDesiredVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 2)))),
        DeveloperDesiredVersion("service2", DeveloperDistributionVersion("test", DeveloperVersion(Seq(2, 1, 2))))),
        Seq(TestSignature("distribution1", date))))))))))
    result(collections.State_TestedVersions.map(_.dropItems()).flatten)
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

    result(collections.State_InstalledDesiredVersions.map(v => result(v.find().map(assertResult(Seq(InstalledDesiredVersionsDocument("distribution1", Seq(
      ClientDesiredVersion("service1", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))),
      ClientDesiredVersion("service2", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2, 1, 1)))))))))(_)))))
    result(collections.State_InstalledDesiredVersions.map(_.dropItems()).flatten)
  }

  it should "set services state" in {
    val graphqlContext = new GraphqlContext(UserInfo("distribution1", UserRole.Distribution), workspace)

    assertResult((OK,
      ("""{"data":{"setServiceStates":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        mutation ServicesState($$date: Date!) {
          setServiceStates (
            state: [
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
            state: [
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

    result(collections.State_ServiceStates.map(_.dropItems()).flatten)
  }
}
