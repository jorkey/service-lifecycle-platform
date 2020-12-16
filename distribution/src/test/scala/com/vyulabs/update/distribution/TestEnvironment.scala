package com.vyulabs.update.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.UserRole
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import com.vyulabs.update.distribution.config.{FaultReportsConfig, VersionHistoryConfig}
import com.vyulabs.update.distribution.graphql.{Graphql, GraphqlWorkspace}
import com.vyulabs.update.distribution.mongo.{DatabaseCollections, MongoDb, UserInfoDocument}
import com.vyulabs.update.distribution.users.{PasswordHash, ServerUserInfo, UserCredentials}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}

abstract class TestEnvironment(val versionHistoryConfig: VersionHistoryConfig  = VersionHistoryConfig(3),
                               val faultReportsConfig: FaultReportsConfig = FaultReportsConfig(30000, 3)) extends FlatSpec with Matchers with BeforeAndAfterAll {
  private implicit val system = ActorSystem("Distribution")
  private implicit val materializer: Materializer = ActorMaterializer()
  private implicit val executionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  implicit val log = LoggerFactory.getLogger(this.getClass)

  def dbName = getClass.getSimpleName

  val distributionName = "test"
  val adminClientCredentials = BasicHttpCredentials("admin", "admin")
  val distributionClientCredentials = BasicHttpCredentials("distribution1", "distribution1")
  val serviceClientCredentials = BasicHttpCredentials("service1", "service1")

  val adminCredentials = UserCredentials(UserRole.Administrator, PasswordHash(adminClientCredentials.password))
  val distributionCredentials = UserCredentials(UserRole.Distribution, PasswordHash(distributionClientCredentials.password))
  val serviceCredentials = UserCredentials(UserRole.Service, PasswordHash(serviceClientCredentials.password))

  val mongo = new MongoDb("mongodb://localhost:27017", dbName); result(mongo.dropDatabase())
  val collections = new DatabaseCollections(mongo, 100)
  val distributionDir = new DistributionDirectory(Files.createTempDirectory("test").toFile)

  val ownServicesDir = Files.createTempDirectory("test").toFile

  val graphql = new Graphql()

  val workspace = GraphqlWorkspace(distributionName, versionHistoryConfig, faultReportsConfig, collections, distributionDir)
  val distribution = new Distribution(workspace, graphql)

  result(for {
    collection <- collections.Users_Info
    _ <- collection.insert(UserInfoDocument(ServerUserInfo(adminClientCredentials.username,
      adminCredentials.role.toString, adminCredentials.password)))
    _ <- collection.insert(UserInfoDocument(ServerUserInfo(distributionClientCredentials.username,
      distributionCredentials.role.toString, distributionCredentials.password)))
    _ <- collection.insert(UserInfoDocument(ServerUserInfo(serviceClientCredentials.username,
      serviceCredentials.role.toString, serviceCredentials.password)))
  } yield {})

  IoUtils.writeServiceVersion(ownServicesDir, Common.DistributionServiceName, ClientDistributionVersion(distributionName, ClientVersion(DeveloperVersion(Seq(1, 2, 3)))))

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(15, TimeUnit.SECONDS))

  override protected def afterAll(): Unit = {
    distributionDir.drop()
    IoUtils.deleteFileRecursively(ownServicesDir)
    result(mongo.dropDatabase())
  }
}
