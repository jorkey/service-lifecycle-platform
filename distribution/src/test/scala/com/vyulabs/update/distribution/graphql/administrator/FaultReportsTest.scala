package com.vyulabs.update.distribution.graphql.administrator

import java.nio.file.Files
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.common.Common._
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.info.{ClientFaultReport, FaultInfo, ServiceState}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.DatabaseCollections
import distribution.config.VersionHistoryConfig
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import distribution.mongo.MongoDb
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}

class FaultReportsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "AdaptationMeasure"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
  implicit val filesLocker = new SmartFilesLocker()

  val versionHistoryConfig = VersionHistoryConfig(5)

  val dir = new DistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb(getClass.getSimpleName); result(mongo.dropDatabase())
  val collections = new DatabaseCollections(mongo, "self-instance", Some("builder"), 100)

  val collection = result(collections.State_FaultReports)

  val graphqlContext = new GraphqlContext(versionHistoryConfig, dir, collections, UserInfo("user", UserRole.Administrator))
  val graphql = new Graphql()

  val client1 = "client1"
  val client2 = "client2"

  val instance1 = "instance1"
  val instance2 = "instance2"

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(3, TimeUnit.SECONDS))

  override def beforeAll() = {
    result(collection.insert(
      ClientFaultReport(Some(client1), "fault1", Seq("fault.info", "core"),
        FaultInfo(new Date(), instance1, "directory", "serviceA", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty))))
    result(collection.insert(
      ClientFaultReport(Some(client2), "fault1", Seq("fault.info", "core1"),
        FaultInfo(new Date(), instance1, "directory", "serviceA", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty))))
    result(collection.insert(
      ClientFaultReport(Some(client1), "fault2", Seq("fault.info", "core"),
        FaultInfo(new Date(), instance2, "directory", "serviceB", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty))))
  }

  override protected def afterAll(): Unit = {
    result(mongo.dropDatabase())
  }

  it should "get last fault reports for specified client" in {
    assertResult((OK,
      ("""{"data":{"faultReports":[""" +
       """{"clientName":"client1","reportDirectory":"fault2","serviceName":"serviceB","instanceId":"instance2","reportFiles":["fault.info","core"]}""" +
      """]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          faultReports (client: "client1", last: 1) {
            clientName
            reportDirectory
            serviceName
            instanceId
            reportFiles
          }
        }
      """))
    )
  }

  it should "get last fault reports for specified service" in {
    assertResult((OK,
      ("""{"data":{"faultReports":[""" +
        """{"clientName":"client2","reportDirectory":"fault1","serviceName":"serviceA","instanceId":"instance1","reportFiles":["fault.info","core1"]}""" +
        """]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          faultReports (service: "serviceA", last: 1) {
            clientName
            reportDirectory
            serviceName
            instanceId
            reportFiles
          }
        }
      """))
    )
  }

  it should "get fault reports for specified service in parameters" in {
    assertResult((OK,
      ("""{"data":{"faultReports":[""" +
        """{"clientName":"client1","reportDirectory":"fault2","serviceName":"serviceB","instanceId":"instance2","reportFiles":["fault.info","core"]}""" +
        """]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition,
        graphqlContext, graphql"""
          query FaultsQuery($$service: String!) {
            faultReports (service: $$service) {
              clientName
              reportDirectory
              serviceName
              instanceId
              reportFiles
            }
          }
        """, None, variables = JsObject("service" -> JsString("serviceB")))))
  }
}