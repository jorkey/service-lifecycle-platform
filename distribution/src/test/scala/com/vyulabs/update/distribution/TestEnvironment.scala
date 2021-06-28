package com.vyulabs.update.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestDuration
import com.mongodb.client.model.{Filters, Updates}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.config._
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.{AccessToken, UserRole}
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.ClientDistributionVersion
import com.vyulabs.update.distribution.common.AkkaTimer
import com.vyulabs.update.distribution.graphql.{Graphql, GraphqlContext, GraphqlWorkspace}
import com.vyulabs.update.distribution.logger.LogStorekeeper
import com.vyulabs.update.distribution.mongo.{DatabaseCollections, MongoDb}
import com.vyulabs.update.distribution.task.TaskManager
import com.vyulabs.update.distribution.users.{PasswordHash, ServerUserInfo, UserCredentials}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, Awaitable, ExecutionContext}

abstract class TestEnvironment(createIndices: Boolean = false) extends FlatSpec with Matchers with BeforeAndAfterAll {
  private implicit val system = ActorSystem("Distribution")
  private implicit val materializer: Materializer = ActorMaterializer()
  private implicit val executionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })
  private implicit val timer = new AkkaTimer(system.scheduler)

  implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(new DurationInt(5).second.dilated(system))

  def dbName = getClass.getSimpleName

  val distributionDirectory = Files.createTempDirectory("distribution-").toFile
  val builderDirectory = new File(distributionDirectory, "builder"); builderDirectory.mkdir()

  def mongoDbConfig = MongoDbConfig("mongodb://localhost:27017", dbName, true)
  def networkConfig = NetworkConfig(0, None)
  def versionsConfig = VersionsConfig(3)
  def instanceStateConfig = InstanceStateConfig(FiniteDuration(60, TimeUnit.SECONDS))
  def faultReportsConfig = FaultReportsConfig(FiniteDuration(30, TimeUnit.SECONDS), 3)

  val config = DistributionConfig("test", "Test distribution server", "instance1", "secret", mongoDbConfig,
                                  networkConfig, None, versionsConfig, instanceStateConfig, faultReportsConfig)

  val distributionName = config.distribution
  val instance = config.instance

  val mongo = new MongoDb(config.mongoDb.name, config.mongoDb.connection, config.mongoDb.temporary); result(mongo.dropDatabase())
  val collections = new DatabaseCollections(mongo, FiniteDuration(100, TimeUnit.SECONDS), createIndices)
  val distributionDir = new DistributionDirectory(distributionDirectory)
  val taskManager = new TaskManager(task => new LogStorekeeper(distributionName, Common.DistributionServiceName, Some(task),
    instance, collections.State_ServiceLogs))

  val graphql = new Graphql()
  val workspace = GraphqlWorkspace(config, collections, distributionDir, taskManager)
  val distribution = new Distribution(workspace, graphql)

  val adminHttpCredentials = BasicHttpCredentials("admin", "admin")
  val developerHttpCredentials = BasicHttpCredentials("developer", "developer")
  val distributionHttpCredentials = BasicHttpCredentials("distribution", "distribution")
  val builderHttpCredentials = BasicHttpCredentials("builder", "builder")
  val updaterHttpCredentials = BasicHttpCredentials("updater", "updater")

  val adminCredentials = UserCredentials(Seq(UserRole.Administrator), PasswordHash(adminHttpCredentials.password))
  val developerCredentials = UserCredentials(Seq(UserRole.Developer), PasswordHash(developerHttpCredentials.password))
  val distributionCredentials = UserCredentials(Seq(UserRole.Distribution), PasswordHash(distributionHttpCredentials.password))
  val builderCredentials = UserCredentials(Seq(UserRole.Builder), PasswordHash(builderHttpCredentials.password))
  val updaterCredentials = UserCredentials(Seq(UserRole.Updater), PasswordHash(updaterHttpCredentials.password))

  val adminContext = GraphqlContext(Some(AccessToken("admin", Seq(UserRole.Administrator))), workspace)
  val developerContext = GraphqlContext(Some(AccessToken("developer", Seq(UserRole.Developer))), workspace)
  val distributionContext = GraphqlContext(Some(AccessToken("distribution", Seq(UserRole.Distribution))), workspace)
  val builderContext = GraphqlContext(Some(AccessToken("builder", Seq(UserRole.Builder))), workspace)
  val updaterContext = GraphqlContext(Some(AccessToken("updater", Seq(UserRole.Updater))), workspace)

  result(for {
    _ <- collections.Users.insert(ServerUserInfo(adminHttpCredentials.username, true, "Test Administrator", adminCredentials.passwordHash, adminCredentials.roles.map(_.toString), None, Seq.empty))
    _ <- collections.Users.insert(ServerUserInfo(developerHttpCredentials.username, true, "Test Developer", developerCredentials.passwordHash, developerCredentials.roles.map(_.toString), None, Seq.empty))
    _ <- collections.Users.insert(ServerUserInfo(builderHttpCredentials.username, false, "Test Builder", builderCredentials.passwordHash, builderCredentials.roles.map(_.toString), None, Seq.empty))
    _ <- collections.Users.insert(ServerUserInfo(distributionHttpCredentials.username, false, "Test Distribution", distributionCredentials.passwordHash, distributionCredentials.roles.map(_.toString), None, Seq.empty))
    _ <- collections.Users.insert(ServerUserInfo(updaterHttpCredentials.username, false, "Test Updater", updaterCredentials.passwordHash, updaterCredentials.roles.map(_.toString), None, Seq.empty))
  } yield {})

  IoUtils.writeServiceVersion(distributionDir.directory, Common.DistributionServiceName, ClientDistributionVersion(distributionName, Seq(1, 2, 3), 0))

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(15, TimeUnit.SECONDS))

  override protected def afterAll(): Unit = {
    mongo.close()
    distributionDir.drop()
    IoUtils.deleteFileRecursively(builderDirectory)
  }

  def setSequence(name: String, sequence: Long): Unit = {
    result(result(collections.Sequences).updateOne(Filters.eq("name", name), Updates.set("sequence", sequence)))
  }
}
