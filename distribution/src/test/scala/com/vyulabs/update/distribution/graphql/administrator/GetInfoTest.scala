package com.vyulabs.update.distribution.graphql.administrator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.config.{DistributionConsumerConfig, DistributionConsumerInfo}
import com.vyulabs.update.common.info.{UserInfo, UserRole}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class GetInfoTest extends TestEnvironment {
  behavior of "Misc Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = new GraphqlContext(UserInfo("admin", UserRole.Administrator), workspace)

  override def beforeAll() = {
    result(collections.Distribution_ConsumersInfo.insert(DistributionConsumerInfo("distribution1", DistributionConsumerConfig("common", Some("test")))))
  }

  it should "get user info" in {
    assertResult((OK,
      ("""{"data":{"userInfo":{"name":"admin","role":"Administrator"}}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          userInfo {
            name
            role
          }
        }
      """))
    )
  }

  it should "get distribution clients info" in {
    assertResult((OK,
      ("""{"data":{"distributionClientsInfo":[{"distributionName":"distribution1","clientConfig":{"installProfile":"common","testDistributionMatch":"test"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
        graphql"""
        query {
          distributionClientsInfo {
            distributionName
            clientConfig {
              installProfile
              testDistributionMatch
            }
          }
        }
      """))
    )
  }
}
