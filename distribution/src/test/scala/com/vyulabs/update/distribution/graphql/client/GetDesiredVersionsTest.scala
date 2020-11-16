package com.vyulabs.update.distribution.graphql.client

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.DeveloperDesiredVersion
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.DeveloperDistributionVersion
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.{ClientInfoDocument, DeveloperDesiredVersionsDocument, PersonalDesiredVersionsDocument}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class GetDesiredVersionsTest extends TestEnvironment {
  behavior of "Developer Desired Versions Client Requests"

  override def beforeAll() = {
    val clientsInfoCollection = result(collections.Developer_ClientsInfo)
    val desiredVersionsCollection = result(collections.Developer_DesiredVersions)
    val personalDesiredVersionsCollection = result(collections.Developer_PersonalDesiredVersions)

    result(clientsInfoCollection.insert(ClientInfoDocument(ClientInfo("client1", ClientConfig("common", None)))))

    desiredVersionsCollection.insert(DeveloperDesiredVersionsDocument(Seq(DeveloperDesiredVersion("service1", BuildVersion(1)), DeveloperDesiredVersion("service2", BuildVersion(2)))))
    personalDesiredVersionsCollection.insert(PersonalDesiredVersionsDocument("client1", Seq(
      DeveloperDesiredVersion("service1", BuildVersion("client1", 2)),
      DeveloperDesiredVersion("service3", BuildVersion("client1", 3)))))
  }

  it should "get desired versions for client" in {
    val graphqlContext = new GraphqlContext(VersionHistoryConfig(5), distributionDir, collections, UserInfo("client1", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"desiredVersions":[{"serviceName":"service1","buildVersion":"client1-2"},{"serviceName":"service2","buildVersion":"2"},{"serviceName":"service3","buildVersion":"client1-3"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        query {
          desiredVersions {
             serviceName
             buildVersion
          }
        }
      """)))
  }
}
