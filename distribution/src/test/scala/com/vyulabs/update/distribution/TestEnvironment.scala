package com.vyulabs.update.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestDuration
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.config.{BuilderConfig, DistributionConfig, FaultReportsConfig, InstanceStateConfig, NetworkConfig, VersionHistoryConfig}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.UserRole
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import com.vyulabs.update.distribution.common.AkkaTimer
import com.vyulabs.update.distribution.graphql.{Graphql, GraphqlWorkspace}
import com.vyulabs.update.distribution.logger.LogStorer
import com.vyulabs.update.distribution.mongo.{DatabaseCollections, MongoDb}
import com.vyulabs.update.distribution.task.TaskManager
import com.vyulabs.update.distribution.users.{PasswordHash, ServerUserInfo, UserCredentials}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, Awaitable, ExecutionContext}

abstract class TestEnvironment() extends FlatSpec with Matchers with BeforeAndAfterAll {
  private implicit val system = ActorSystem("Distribution")
  private implicit val materializer: Materializer = ActorMaterializer()
  private implicit val executionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })
  private implicit val timer = new AkkaTimer(system.scheduler)

  implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(new DurationInt(5).second.dilated(system))

  def dbName = getClass.getSimpleName

  val distributionDirectory = Files.createTempDirectory("distribution-").toFile
  val builderDirectory = Files.createTempDirectory("builder-").toFile

  def networkConfig = NetworkConfig(0, None)
  def builderConfig = BuilderConfig(None, None)
  def versionHistoryConfig = VersionHistoryConfig(3)
  def instanceStateConfig = InstanceStateConfig(60)
  def faultReportsConfig = FaultReportsConfig(30000, 3)

  val config = DistributionConfig("test", "Test distribution server", "instance1", "mongodb://localhost:27017", dbName,
                                  networkConfig, distributionDirectory.toString,
                                  builderConfig, versionHistoryConfig, instanceStateConfig, faultReportsConfig, None)

  val distributionName = config.distributionName
  val instanceId = config.instanceId
  val adminClientCredentials = BasicHttpCredentials("admin", "admin")
  val distributionClientCredentials = BasicHttpCredentials("distribution1", "distribution1")
  val serviceClientCredentials = BasicHttpCredentials("service1", "service1")

  val adminCredentials = UserCredentials(UserRole.Administrator, PasswordHash(adminClientCredentials.password))
  val distributionCredentials = UserCredentials(UserRole.Distribution, PasswordHash(distributionClientCredentials.password))
  val serviceCredentials = UserCredentials(UserRole.Service, PasswordHash(serviceClientCredentials.password))

  val mongo = new MongoDb(config.mongoDbConnection, dbName); result(mongo.dropDatabase())
  val collections = new DatabaseCollections(mongo, 100)
  val distributionDir = new DistributionDirectory(distributionDirectory)
  val taskManager = new TaskManager(taskId => new LogStorer(distributionName, Common.DistributionServiceName, Some(taskId),
    instanceId, collections.State_ServiceLogs))

  val graphql = new Graphql()
  val workspace = GraphqlWorkspace(config, collections, distributionDir, taskManager)
  val distribution = new Distribution(workspace, graphql)

  result(for {
    _ <- collections.Users_Info.insert(ServerUserInfo(adminClientCredentials.username, adminCredentials.role.toString, adminCredentials.passwordHash))
    _ <- collections.Users_Info.insert(ServerUserInfo(distributionClientCredentials.username, distributionCredentials.role.toString, distributionCredentials.passwordHash))
    _ <- collections.Users_Info.insert(ServerUserInfo(serviceClientCredentials.username, serviceCredentials.role.toString, serviceCredentials.passwordHash))
  } yield {})

  IoUtils.writeServiceVersion(distributionDir.directory, Common.DistributionServiceName, ClientDistributionVersion(distributionName, ClientVersion(DeveloperVersion(Seq(1, 2, 3)))))

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(15, TimeUnit.SECONDS))

  override protected def afterAll(): Unit = {
    //distributionDir.drop()
    //config.builderConfig.builderDirectory.foreach(dir => IoUtils.deleteFileRecursively(new File(dir)))
    //result(mongo.dropDatabase())
  }
}
