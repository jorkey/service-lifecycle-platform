package com.vyulabs.update.distribution.graphql.accounts

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info.{ServicesProfile}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class AccountsTest extends TestEnvironment {
  behavior of "Accounts Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    result(collections.Developer_ServiceProfiles.insert(ServicesProfile("common", Seq("service1", "service2"))))
  }

  it should "add/change password/remove accounts" in {
    assertResult((OK,
      ("""{"data":{"addUserAccount":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          addUserAccount (
            account: "user",
            name: "Test User",
            role: Developer,
            password: "password1",
            properties: {
              email: "http://test.com",
              notifications: []
            }
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"changeUserAccount":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          changeUserAccount (
            account: "user",
            password: "password2"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"removeAccount":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          removeAccount (
            account: "user",
          )
        }
      """)))
  }

  it should "change self password" in {
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"You can change only self account","path":["changeUserAccount"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext,
        graphql"""
        mutation {
          changeUserAccount (
            account: "admin",
            oldPassword: "password",
            password: "password2"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Old password is not specified","path":["changeUserAccount"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext,
        graphql"""
        mutation {
          changeUserAccount (
            account: "developer",
            password: "password2"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Password verification error","path":["changeUserAccount"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext,
        graphql"""
        mutation {
          changeUserAccount (
            account: "developer",
            oldPassword: "bad password",
            password: "password2"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"changeUserAccount":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext,
        graphql"""
        mutation {
          changeUserAccount (
            account: "developer",
            oldPassword: "developer",
            password: "password2"
          )
        }
      """)))
  }

  it should "change account password by administrator" in {
    assertResult((OK,
      ("""{"data":{"changeUserAccount":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          changeUserAccount (
            account: "developer",
            password: "password3"
          )
        }
      """)))

  }
}
