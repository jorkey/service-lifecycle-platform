package com.vyulabs.update.distribution.graphql.service

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.DeveloperDesiredVersion
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.DeveloperDistributionVersion
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.DeveloperDesiredVersionsDocument
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class GetDesiredVersionsTest extends TestEnvironment {
  behavior of "Desired Versions Service Requests"

  override def beforeAll() = {
    val desiredVersionsCollection = result(collections.Client_DesiredVersions)

    desiredVersionsCollection.insert(DeveloperDesiredVersionsDocument(Seq(DeveloperDesiredVersion("service1", BuildVersion(1)), DeveloperDesiredVersion("service2", BuildVersion(2)))))
  }

  it should "get desired versions for service" in {
    val graphqlContext = new GraphqlContext(VersionHistoryConfig(5), distributionDir, collections, UserInfo("service1", UserRole.Service))

    assertResult((OK,
      ("""{"data":{"desiredVersions":[{"serviceName":"service1","buildVersion":"1"},{"serviceName":"service2","buildVersion":"2"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        query {
          desiredVersions {
             serviceName
             buildVersion
          }
        }
      """)))
  }
}
