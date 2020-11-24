package com.vyulabs.update.distribution

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.Common
import distribution.users.{PasswordHash, UserCredentials, UserRole, UsersCredentials}
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import distribution.Distribution
import distribution.config.{DistributionConfig, FaultReportsConfig, VersionHistoryConfig}
import distribution.graphql.{Graphql, GraphqlWorkspace}
import distribution.mongo.{DatabaseCollections, MongoDb}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}

abstract class TestEnvironment extends FlatSpec with Matchers with BeforeAndAfterAll {
  private implicit val system = ActorSystem("Distribution")
  private implicit val materializer: Materializer = ActorMaterializer()
  private implicit val executionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  implicit val log = LoggerFactory.getLogger(this.getClass)

  def dbName = getClass.getSimpleName

  val distributionName = "test"
  val adminClientCredentials = BasicHttpCredentials("admin", "admin")
  val distributionClientCredentials = BasicHttpCredentials("clientDistribution", "clientDistribution")
  val serviceClientCredentials = BasicHttpCredentials("service", "service")

  val adminCredentials = UserCredentials(UserRole.Administrator, PasswordHash(adminClientCredentials.password))
  val distributionCredentials = UserCredentials(UserRole.Distribution, PasswordHash(distributionClientCredentials.password))
  val serviceCredentials = UserCredentials(UserRole.Service, PasswordHash(serviceClientCredentials.password))

  val usersCredentials = new UsersCredentials(Map(
    adminClientCredentials.username -> adminCredentials,
    distributionClientCredentials.username -> distributionCredentials,
    serviceClientCredentials.username -> serviceCredentials))

  val mongo = new MongoDb(dbName); result(mongo.dropDatabase())
  val collections = new DatabaseCollections(mongo, 100)
  val distributionDir = new DistributionDirectory(Files.createTempDirectory("test").toFile)
  val versionHistoryConfig = VersionHistoryConfig(3)
  val faultReportsConfig = FaultReportsConfig(3000, 3)

  val ownServicesDir = Files.createTempDirectory("test").toFile

  val graphql = new Graphql()

  val workspace = new GraphqlWorkspace(distributionName, versionHistoryConfig, faultReportsConfig, collections, distributionDir)
  val distribution = new Distribution(workspace, usersCredentials, graphql)

  IoUtils.writeServiceVersion(ownServicesDir, Common.DistributionServiceName, ClientDistributionVersion(distributionName, ClientVersion(DeveloperVersion(Seq(1, 2, 3)))))

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(15, TimeUnit.SECONDS))

  override protected def afterAll(): Unit = {
    distributionDir.drop()
    IoUtils.deleteFileRecursively(ownServicesDir)
    result(mongo.dropDatabase())
  }
}
