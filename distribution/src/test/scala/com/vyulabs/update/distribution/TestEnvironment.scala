package com.vyulabs.update.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, OAuth2BearerToken}
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestDuration
import com.mongodb.client.model.{Filters, Updates}
import com.vyulabs.update.common.common.{Common, JWT}
import com.vyulabs.update.common.config._
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.{AccessToken, AccountRole}
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.ClientDistributionVersion
import com.vyulabs.update.distribution.common.AkkaTimer
import com.vyulabs.update.distribution.graphql.{Graphql, GraphqlContext, GraphqlWorkspace}
import com.vyulabs.update.distribution.logger.LogStorekeeper
import com.vyulabs.update.distribution.mongo.{DatabaseCollections, MongoDb}
import com.vyulabs.update.distribution.task.TaskManager
import com.vyulabs.update.common.accounts.{ConsumerAccountInfo, ConsumerAccountProperties, PasswordHash, ServerAccountInfo, ServiceAccountInfo, UserAccountInfo, UserAccountProperties}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.{LoggerFactory}

import java.io.{File}
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

  def mongoDbConfig = MongoDbConfig("mongodb://localhost:27017", dbName, Some(true))
  def networkConfig = NetworkConfig("localhost", 0, None)
  def versionsConfig = VersionsConfig(3)
  def instanceStateConfig = ServiceStatesConfig(FiniteDuration(60, TimeUnit.SECONDS))
  def logsConfig = LogsConfig(FiniteDuration(60, TimeUnit.SECONDS))
  def faultReportsConfig = FaultReportsConfig(FiniteDuration(30, TimeUnit.SECONDS), 3)

  val config = DistributionConfig("test", "Test distribution server", "instance1", "secret", mongoDbConfig,
                                  networkConfig, versionsConfig, instanceStateConfig, logsConfig, faultReportsConfig)

  val distributionName = config.distribution
  val instance = config.instance

  val distributionDirectory = Files.createTempDirectory("distribution-").toFile

  val mongo = new MongoDb(config.mongoDb.name, config.mongoDb.connection,
    config.mongoDb.temporary.getOrElse(false)); result(mongo.dropDatabase())
  val collections = new DatabaseCollections(mongo,
    instanceStateConfig.expirationTimeout,
    logsConfig.expirationTimeout,
    createIndices)
  val distributionDir = new DistributionDirectory(distributionDirectory)
  val taskManager = new TaskManager(task => new LogStorekeeper(Common.DistributionServiceName, Some(task),
    instance, collections.Log_Lines))

  val graphql = new Graphql()
  val workspace = GraphqlWorkspace(config, collections, distributionDir, taskManager)
  val distribution = new Distribution(workspace, graphql)

  val adminHttpCredentials = BasicHttpCredentials("admin", "admin")
  val developerHttpCredentials = BasicHttpCredentials("developer", "developer")

  val builderHttpCredentials = OAuth2BearerToken(JWT.encodeAccessToken(AccessToken("builder"), config.jwtSecret))
  val updaterHttpCredentials = OAuth2BearerToken(JWT.encodeAccessToken(AccessToken("updater"), config.jwtSecret))
  val consumerHttpCredentials = OAuth2BearerToken(JWT.encodeAccessToken(AccessToken("consumer"), config.jwtSecret))

  val adminAccountInfo = UserAccountInfo("admin", "Administrator", AccountRole.Administrator, UserAccountProperties(None, Seq.empty))
  val developerAccountInfo = UserAccountInfo("developer", "Developer", AccountRole.Developer, UserAccountProperties(None, Seq.empty))
  val consumerAccountInfo = ConsumerAccountInfo("consumer", "Distribution Consumer", AccountRole.DistributionConsumer,
    ConsumerAccountProperties(Common.CommonConsumerProfile, "http://localhost:8001"))

  val adminContext = GraphqlContext(Some(AccessToken("admin")), Some(adminAccountInfo), workspace)
  val developerContext = GraphqlContext(Some(AccessToken("developer")), Some(developerAccountInfo), workspace)
  val builderContext = GraphqlContext(Some(AccessToken("builder")),
    Some(ServiceAccountInfo("builder", "Builder", AccountRole.Builder)), workspace)
  val updaterContext = GraphqlContext(Some(AccessToken("updater")),
    Some(ServiceAccountInfo("updater", "Updater", AccountRole.Updater)), workspace)
  val consumerContext = GraphqlContext(Some(AccessToken("consumer")), Some(consumerAccountInfo), workspace)

  val builderDirectory = new File(distributionDirectory, "builder/" + config.distribution); builderDirectory.mkdirs()
  val consumerBuilderDirectory = new File(distributionDirectory, "builder/consumer"); consumerBuilderDirectory.mkdirs()

  result(for {
    _ <- collections.Accounts.insert(ServerAccountInfo(ServerAccountInfo.TypeUser, "admin", "Test Administrator",
        AccountRole.Administrator.toString, Some(PasswordHash(adminHttpCredentials.password)), Some(adminAccountInfo.properties), None))
    _ <- collections.Accounts.insert(ServerAccountInfo(ServerAccountInfo.TypeUser, developerHttpCredentials.username, "Test Developer",
        AccountRole.Developer.toString,  Some(PasswordHash(developerHttpCredentials.password)), Some(developerAccountInfo.properties), None))
    _ <- collections.Accounts.insert(ServerAccountInfo(ServerAccountInfo.TypeService, "updater", "Test Updater",
        AccountRole.Updater.toString, None, None,  None))
    _ <- collections.Accounts.insert(ServerAccountInfo(ServerAccountInfo.TypeService, "builder", "Test builder",
        AccountRole.Builder.toString, None, None,  None))
    _ <- collections.Accounts.insert(ServerAccountInfo(ServerAccountInfo.TypeConsumer, "consumer", "Test Distribution Consumer",
        AccountRole.DistributionConsumer.toString, None, None,
      consumer = Some(ConsumerAccountProperties(profile = Common.CommonConsumerProfile, url = "http://localhost:8001"))))
    _ <- collections.Developer_Builder.insert(BuilderConfig(distributionName))
    _ <- collections.Client_Builder.insert(BuilderConfig(distributionName))
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
