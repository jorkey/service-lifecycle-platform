package com.vyulabs.update.distribution

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.vyulabs.update.common.Common
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import distribution.config.VersionHistoryConfig
import distribution.graphql.Graphql
import distribution.mongo.{DatabaseCollections, MongoDb}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}

class TestEnvironment extends FlatSpec with Matchers with BeforeAndAfterAll {
  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

  val distributionDir = new DistributionDirectory(Files.createTempDirectory("test").toFile)
  val ownServicesDir = Files.createTempDirectory("test").toFile
  IoUtils.writeServiceVersion(ownServicesDir, Common.DistributionServiceName, ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 2, 3)))))
  val mongo = new MongoDb(getClass.getSimpleName); result(mongo.dropDatabase())
  val collections = new DatabaseCollections(mongo, "self-instance", ownServicesDir, Some("build"), Some("install"), 100)
  val graphql = new Graphql()
  val versionHistoryConfig = VersionHistoryConfig(5)

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(3, TimeUnit.SECONDS))

  override protected def beforeAll(): Unit = {
  }

  override protected def afterAll(): Unit = {
    distributionDir.drop()
    IoUtils.deleteFileRecursively(ownServicesDir)
    result(mongo.dropDatabase())
  }
}
