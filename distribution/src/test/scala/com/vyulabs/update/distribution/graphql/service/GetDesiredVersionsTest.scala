package com.vyulabs.update.distribution.graphql.service

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.distribution.GraphqlTestEnvironment
import com.vyulabs.update.info.DesiredVersion
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.BuildVersion
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.documents.{DesiredVersion, DesiredVersionsDocument}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class GetDesiredVersionsTest extends GraphqlTestEnvironment {
  behavior of "Desired Versions Service Requests"

  override def beforeAll() = {
    val desiredVersionsCollection = result(collections.Client_DesiredVersions)

    desiredVersionsCollection.insert(DesiredVersionsDocument(Seq(DesiredVersion("service1", BuildVersion(1)), DesiredVersion("service2", BuildVersion(2)))))
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
