package com.vyulabs.update.distribution.graphql.distribution

import java.util.Date
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.config.{DistributionClientConfig, DistributionClientInfo, DistributionClientProfile}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import com.vyulabs.update.distribution.mongo.{DistributionClientInfoDocument, DistributionClientProfileDocument, TestedDesiredVersionsDocument}
import com.vyulabs.update.common.info.{DeveloperDesiredVersion, TestSignature, TestedDesiredVersions}
import com.vyulabs.update.common.info.{UserInfo, UserRole}
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import com.vyulabs.update.distribution.mongo.TestedDesiredVersionsDocument
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class TestedVersionsTest extends TestEnvironment {
  behavior of "Tested Versions Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    val installProfileCollection = result(collections.Developer_DistributionClientsProfiles)
    val clientInfoCollection = result(collections.Developer_DistributionClientsInfo)

    result(installProfileCollection.insert(DistributionClientProfileDocument(DistributionClientProfile("common", Set("service1", "service2")))))

    result(clientInfoCollection.insert(DistributionClientInfoDocument(DistributionClientInfo("distribution1", DistributionClientConfig("common", None)))))
    result(clientInfoCollection.insert(DistributionClientInfoDocument(DistributionClientInfo("distribution2", DistributionClientConfig("common", Some("distribution1"))))))
  }

  it should "set/get tested versions" in {
    val graphqlContext1 = new GraphqlContext(UserInfo("distribution1", UserRole.Distribution), workspace)

    assertResult((OK,
      ("""{"data":{"setTestedVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext1, graphql"""
        mutation {
          setTestedVersions (
            versions: [
               { serviceName: "service1", version: "test-1.1.1" },
               { serviceName: "service2", version: "test-2.1.1" }
            ]
          )
        }
      """)))

    val graphqlContext2 = new GraphqlContext(UserInfo("distribution2", UserRole.Distribution), workspace)

    assertResult((OK,
      ("""{"data":{"desiredVersions":[{"serviceName":"service1","version":"test-1.1.1"},{"serviceName":"service2","version":"test-2.1.1"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext2, graphql"""
        query {
          desiredVersions {
            serviceName
            version
          }
        }
      """)))

    result(collections.State_TestedVersions.map(_.dropItems()).flatten)
  }

  it should "return error if no tested versions for the client's profile" in {
    val graphqlContext = new GraphqlContext(UserInfo("distribution2", UserRole.Distribution), workspace)
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Desired versions for profile common are not tested by anyone","path":["desiredVersions"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        query {
          desiredVersions {
            serviceName
            version
          }
        }
      """)))
  }

  it should "return error if client required preliminary testing has personal desired versions" in {
    val graphqlContext = new GraphqlContext(UserInfo("distribution1", UserRole.Distribution), workspace)
    result(collections.State_TestedVersions.map(_.insert(
      TestedDesiredVersionsDocument(TestedDesiredVersions("common", Seq(
        DeveloperDesiredVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 0))))),
        Seq(TestSignature("test-client", new Date())))))).flatten)
    result(collections.Client_DesiredVersions.map(_.dropItems()).flatten)
  }
}
