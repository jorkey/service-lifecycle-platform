package com.vyulabs.update.distribution.graphql.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import com.vyulabs.update.distribution.mongo.DeveloperVersionInfoDocument
import com.vyulabs.update.common.info.{BuildInfo, DeveloperVersionInfo, UserInfo, UserRole}
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.util.Date
import scala.concurrent.ExecutionContext

class GetVersionsInfoTest extends TestEnvironment {
  behavior of "Get Versions Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = new GraphqlContext(UserInfo("distribution1", UserRole.Distribution), workspace)

  override def beforeAll(): Unit = {
    result(result(collections.Developer_VersionsInfo).insert(DeveloperVersionInfoDocument(0,
      DeveloperVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 1))), BuildInfo("author1", Seq.empty, new Date(), None)))))
  }

  it should "get version info" in {
    assertResult((OK,
      ("""{"data":{"versionsInfo":[{"version":"test-1.1.1","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        query {
          versionsInfo (service: "service1", version: "test-1.1.1") {
            version
            buildInfo {
              author
            }
          }
        }
      """
    )))
  }
}
