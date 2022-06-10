package com.vyulabs.update.distribution.graphql.state

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.accounts.{ConsumerAccountInfo, ConsumerAccountProperties}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import com.vyulabs.update.distribution.mongo.InstalledDesiredVersions
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.util.Date
import scala.concurrent.ExecutionContext

class StateInfoTest extends TestEnvironment {
  behavior of "Tested Versions Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, Utils.logException(log, "Uncatched exception", _))

  override protected def beforeAll(): Unit = {
  }

  it should "set tested versions" in {
    assertResult((OK,
      ("""{"data":{"setTestedVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, consumerContext, graphql"""
        mutation {
          setTestedVersions (
            versions: [
              { service: "service1", version: { distribution: "test", build: [1,1,2] } },
              { service: "service2", version: { distribution: "test", build: [2,1,2] } }
            ]
          )
        }
      """)))

    val date = new Date()

    assertResult(Seq(TestedVersions("common", "consumer", Seq(
      DeveloperDesiredVersion("service1", DeveloperDistributionVersion("test", Seq(1, 1, 2))),
      DeveloperDesiredVersion("service2", DeveloperDistributionVersion("test", Seq(2, 1, 2)))),
      date)))(result(collections.Developer_TestedVersions.find()
        .map(_.map(v => TestedVersions(v.profile, v.consumerDistribution, v.versions, date)))))
    result(collections.Developer_TestedVersions.drop())
  }

  it should "set/get installed desired versions" in {
    assertResult((OK,
      ("""{"data":{"setInstalledDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, consumerContext, graphql"""
        mutation {
          setInstalledDesiredVersions (
            versions: [
               { service: "service1", version: { distribution: "test", developerBuild: [1,1,1], clientBuild: 0 } },
               { service: "service2", version: { distribution: "test", developerBuild: [2,1,1], clientBuild: 0 } }
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"installedDesiredVersions":[{"service":"service1","version":{"distribution":"test","developerBuild":[1,1,1],"clientBuild":0}},{"service":"service2","version":{"distribution":"test","developerBuild":[2,1,1],"clientBuild":0}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          installedDesiredVersions (distribution: "consumer") {
            service
            version { distribution, developerBuild, clientBuild }
          }
        }
      """)))

    result(collections.Consumers_InstalledDesiredVersions.find().map(assertResult(Seq(InstalledDesiredVersions("consumer", Seq(
      ClientDesiredVersion("service1", ClientDistributionVersion("test", Seq(1, 1, 1), 0)),
      ClientDesiredVersion("service2", ClientDistributionVersion("test", Seq(2, 1, 1), 0))))))(_)))
    result(collections.Consumers_InstalledDesiredVersions.drop())
  }

  it should "set services state" in {
    val distributionContext = GraphqlContext(Some(AccessToken("consumer")),
      Some(ConsumerAccountInfo("consumer", "Distribution Consumer", AccountRole.DistributionConsumer,
        ConsumerAccountProperties(Common.CommonConsumerProfile, "http://dummy"))), workspace)

    assertResult((OK,
      ("""{"data":{"setServiceStates":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, distributionContext, graphql"""
        mutation ServicesState($$time: Date!) {
          setServiceStates (
            states: [
              { instance: "instance1", service: "service1", directory: "dir",
                  state: { time: $$time, version: { distribution: "test", developerBuild: [1,2,3], clientBuild: 0 } }
              }
            ]
          )
        }
      """, variables = JsObject("time" -> new Date().toJson))))

    assertResult((OK,
      ("""{"data":{"setServiceStates":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, distributionContext, graphql"""
        mutation ServicesState($$time: Date!) {
          setServiceStates (
            states: [
              { instance: "instance1", service: "service1", directory: "dir",
                  state: { time: $$time, version: { distribution: "test", developerBuild: [1,2,4], clientBuild: 0 } }
              }
            ]
          )
        }
      """, variables = JsObject("time" -> new Date().toJson))))

    assertResult((OK,
      ("""{"data":{"serviceStates":[{"instance":"instance1","state":{"version":{"distribution":"test","developerBuild":[1,2,4],"clientBuild":0}}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          serviceStates (distribution: "consumer", service: "service1") {
            instance
            state {
              version { distribution, developerBuild, clientBuild }
            }
          }
        }
      """))
    )

    result(collections.State_Instances.drop())
  }
}
