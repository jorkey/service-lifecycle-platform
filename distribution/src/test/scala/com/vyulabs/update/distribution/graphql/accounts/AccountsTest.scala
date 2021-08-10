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
      ("""{"data":{"addAccount":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          addAccount (
            account: "distribution2",
            name: "Second Distribution",
            roles: [Distribution],
            password: "password1"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"changeAccount":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          changeAccount (
            account: "distribution2",
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
            account: "distribution2",
          )
        }
      """)))
  }

  it should "change self password" in {
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"You can change only self account","path":["changeAccount"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext,
        graphql"""
        mutation {
          changeAccount (
            account: "admin",
            oldPassword: "password",
            password: "password2"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Old password is not specified","path":["changeAccount"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext,
        graphql"""
        mutation {
          changeAccount (
            account: "developer",
            password: "password2"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Password verification error","path":["changeAccount"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext,
        graphql"""
        mutation {
          changeAccount (
            account: "developer",
            oldPassword: "bad password",
            password: "password2"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"changeAccount":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext,
        graphql"""
        mutation {
          changeAccount (
            account: "developer",
            oldPassword: "developer",
            password: "password2"
          )
        }
      """)))
  }

  it should "change account password by administrator" in {
    assertResult((OK,
      ("""{"data":{"changeAccount":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          changeAccount (
            account: "developer",
            password: "password3"
          )
        }
      """)))

  }
}
