package com.vyulabs.update.distribution.graphql.administrator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info.{AccessToken, DistributionConsumerInfo, DistributionConsumerProfile, UserInfo, UserRole}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class UsersTest extends TestEnvironment {
  behavior of "Users Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    result(collections.Distribution_ConsumerProfiles.insert(DistributionConsumerProfile("common", Seq("service1", "service2"))))
    result(collections.Distribution_ConsumersInfo.insert(DistributionConsumerInfo("client2", "common", None)))
  }

  it should "add/change password/remove users" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("admin", Seq(UserRole.Administrator))), workspace)

    assertResult((OK,
      ("""{"data":{"addUser":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
        graphql"""
        mutation {
          addUser (
            user: "distribution2",
            role: Distribution,
            password: "password1"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"changeUserPassword":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
        graphql"""
        mutation {
          changeUserPassword (
            user: "distribution2",
            password: "password2"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"removeUser":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
        graphql"""
        mutation {
          removeUser (
            user: "distribution2",
          )
        }
      """)))
  }

  it should "get user info" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("admin", Seq(UserRole.Administrator))), workspace)

    assertResult((OK,
      ("""{"data":{"userInfo":{"name":"admin","role":"Administrator"}}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        query {
          userInfo {
            name
            role
          }
        }
      """))
    )
  }

  it should "change own password" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("admin", Seq(UserRole.Administrator))), workspace)

    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Password verification error","path":["changePassword"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
        graphql"""
        mutation {
          changePassword (
            oldPassword: "bad password",
            password: "password2"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"changePassword":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
        graphql"""
        mutation {
          changePassword (
            oldPassword: "admin",
            password: "password2"
          )
        }
      """)))
  }
}
