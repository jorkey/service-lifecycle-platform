package com.vyulabs.update.distribution.graphql.users

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info.{DistributionConsumerInfo, DistributionConsumerProfile}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
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
    assertResult((OK,
      ("""{"data":{"addUser":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          addUser (
            user: "distribution2",
            roles: [Distribution],
            password: "password1"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"changeUserPassword":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
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
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          removeUser (
            user: "distribution2",
          )
        }
      """)))
  }

  it should "change own password" in {
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Password verification error","path":["changePassword"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext,
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
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext,
        graphql"""
        mutation {
          changePassword (
            oldPassword: "developer",
            password: "password2"
          )
        }
      """)))
  }
}