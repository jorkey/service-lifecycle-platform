package com.vyulabs.update.distribution.graphql.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info.{AccessToken, BuildInfo, DeveloperVersionInfo, UserInfo, UserRole}
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._
import java.util.Date

import scala.concurrent.ExecutionContext

class GetDeveloperVersionsInfoTest extends TestEnvironment {
  behavior of "Get Versions Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = GraphqlContext(Some(AccessToken("distribution1", Seq(UserRole.Distribution))), workspace)

  override def beforeAll(): Unit = {
    result(collections.Developer_VersionsInfo.insert(
      DeveloperVersionInfo.from("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 1))), BuildInfo("author1", Seq.empty, new Date(), None))))
  }

  it should "get developer versions info" in {
    assertResult((OK,
      ("""{"data":{"developerVersionsInfo":[{"version":"test-1.1.1","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        query {
          developerVersionsInfo (service: "service1", distribution: "test", version: "1.1.1") {
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