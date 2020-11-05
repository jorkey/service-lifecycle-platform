package com.vyulabs.update.distribution.graphql.client

import java.nio.file.Files
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.distribution.{DistributionDirectory, GraphqlTestEnvironment}
import com.vyulabs.update.info.{ClientServiceState, DesiredVersion, InstalledDesiredVersions, ServiceState, TestSignature, TestedDesiredVersions}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.BuildVersion
import distribution.config.VersionHistoryConfig
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import distribution.mongo.MongoDb
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}
import com.vyulabs.update.utils.Utils.DateJson._
import distribution.DatabaseCollections

class StateInfoTest extends GraphqlTestEnvironment {
  behavior of "Tested Versions Info Requests"

  override protected def beforeAll(): Unit = {
    result(collections.Developer_ClientsInfo.map(_.insert(ClientInfo("client1", ClientConfig("common", Some("test"))))))
  }

  it should "set tested versions" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("client1", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"setTestedVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setTestedVersions (
            versions: [
              { serviceName: "service1", buildVersion: "1.1.2" },
              { serviceName: "service2", buildVersion: "2.1.2" }
            ]
          )
        }
      """)))

    val date = new Date()
    result(collections.State_TestedVersions.map(v => result(v.find().map(_.map(v => TestedDesiredVersions(v.profileName, v.versions, v.signatures.map(s => TestSignature(s.clientName, date)))))
      .map(assertResult(_)(Seq(TestedDesiredVersions("common",
        Seq(DesiredVersion("service1", BuildVersion(1, 1, 2)), DesiredVersion("service2", BuildVersion(2, 1, 2))), Seq(TestSignature("client1", date)))))))))
    result(collections.State_TestedVersions.map(_.dropItems()))
  }

  it should "set installed desired versions" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("client1", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"setInstalledDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setInstalledDesiredVersions (
            versions: [
               { serviceName: "service1", buildVersion: "1.1.1" },
               { serviceName: "service2", buildVersion: "2.1.1" }
            ]
          )
        }
      """)))

    result(collections.State_InstalledDesiredVersions.map(v => result(v.find().map(assertResult(Seq(InstalledDesiredVersions("client1",
      Seq(DesiredVersion("service1", BuildVersion(1, 1, 1)), DesiredVersion("service2", BuildVersion(2, 1, 1))))))(_)))))
    result(collections.State_InstalledDesiredVersions.map(_.dropItems()))
  }

  it should "set services state" in {
    val graphqlContext1 = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("client1", UserRole.Client))
    assertResult((OK,
      ("""{"data":{"setServicesState":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext1, graphql"""
        mutation ServicesState($$date: Date!) {
          setServicesState (
            state: [
              { instanceId: "instance1", serviceName: "service1", directory: "dir",
                  state: { date: $$date, version: "1.2.3" }
              }
            ]
          )
        }
      """, variables = JsObject("date" -> new Date().toJson))))

    val graphqlContext2 = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("client1", UserRole.Administrator))
    assertResult((OK,
      ("""{"data":{"servicesState":[{"instanceId":"instance1","state":{"version":"1.2.3"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext2, graphql"""
        query {
          servicesState (client: "client1", service: "service1") {
            instanceId
            state {
              version
            }
          }
        }
      """))
    )

    result(collections.State_ServiceStates.map(_.dropItems()))
  }
}
