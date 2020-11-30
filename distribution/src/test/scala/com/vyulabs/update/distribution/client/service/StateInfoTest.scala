package com.vyulabs.update.distribution.client.service

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{UserInfo, UserRole}
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._
import com.vyulabs.update.utils.Utils.DateJson._

import scala.concurrent.ExecutionContext

class StateInfoTest extends TestEnvironment {
  behavior of "State Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = new GraphqlContext(UserInfo("service", UserRole.Service), workspace)

  it should "set/get own service state" in {
    assertResult((OK,
      ("""{"data":{"setServiceStates":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        mutation ServicesState($$date: Date!) {
          setServiceStates (
            states: [
              {
                instanceId: "instance1",
                serviceName: "service1",
                directory: "dir",
                service: {
                  date: $$date,
                  version: "test-1.2.3"
                }
              }
            ]
          )
        }
      """, variables = JsObject("date" -> new Date().toJson))))

    assertResult((OK,
      ("""{"data":{"serviceStates":[{"service":{"version":"test-1.2.3"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        query {
          serviceStates (instance: "instance1", service: "service1", directory: "dir") {
            service  {
              version
            }
          }
        }
      """, None, variables = JsObject("directory" -> JsString(ownServicesDir.getCanonicalPath)))))
  }
}
