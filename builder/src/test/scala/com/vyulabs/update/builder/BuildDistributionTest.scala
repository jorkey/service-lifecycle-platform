package com.vyulabs.update.builder

import com.vyulabs.update.common.accounts.ConsumerAccountProperties
import com.vyulabs.update.common.common.{Common, JWT}
import com.vyulabs.update.common.config.MongoDbConfig
import com.vyulabs.update.common.info.AccessToken
import com.vyulabs.update.distribution.mongo.MongoDb
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class BuildDistributionTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "BuildDistribution"

  implicit val log = LoggerFactory.getLogger(this.getClass)

  val providerDistributionName = "test-provider"
  val providerDistributionDir = Files.createTempDirectory("distrib").toFile

  val consumerDistributionName = "test-consumer"
  val consumerDistributionDir = Files.createTempDirectory("distrib").toFile

  val mongoDbConnection = s"mongodb://localhost:27017"

  val providerMongoDbName = "BuildDistributionTest-provider"
  val consumerMongoDbName = "BuildDistributionTest-consumer"

  override protected def beforeAll(): Unit = {
    Await.result(new MongoDb(providerMongoDbName).dropDatabase(), FiniteDuration(3, TimeUnit.SECONDS))
    Await.result(new MongoDb(consumerMongoDbName).dropDatabase(), FiniteDuration(3, TimeUnit.SECONDS))
  }

  override protected def afterAll(): Unit = {
    ProcessHandle.current().children().forEach(handle => handle.destroy())
    Await.result(new MongoDb(providerMongoDbName).dropDatabase(), FiniteDuration(10, TimeUnit.SECONDS))
    Await.result(new MongoDb(consumerMongoDbName).dropDatabase(), FiniteDuration(10, TimeUnit.SECONDS))
  }

  it should "build provider and consumer distribution servers" in {
    log.info("")
    log.info(s"########################### Build provider distribution from sources")
    log.info("")
    val providerDistributionBuilder = new DistributionBuilder(
      "None", providerDistributionName, providerDistributionDir,
      "localhost", 8000, None, None,
      "Test developer distribution server",
      MongoDbConfig(mongoDbConnection, providerMongoDbName, Some(true)),
      Some(MongoDbConfig(mongoDbConnection, providerMongoDbName, Some(true))), false)
    assert(providerDistributionBuilder.buildDistributionFromSources("ak"))
    assert(providerDistributionBuilder.addConsumerAccount(consumerDistributionName,
      "Distribution Consumer", ConsumerAccountProperties(Common.CommonConsumerProfile, "http://localhost:8001")))

    log.info("")
    log.info(s"########################### Build consumer distribution from provider distribution")
    log.info("")
    val consumerDistributionBuilder = new DistributionBuilder(
      "None", consumerDistributionName, consumerDistributionDir,
      "localhost", 8001, None, None,
      "Test client distribution server",
      MongoDbConfig(mongoDbConnection, consumerMongoDbName, Some(true)),
      Some(MongoDbConfig(mongoDbConnection, consumerMongoDbName, Some(true))),false)
    assert(consumerDistributionBuilder.buildFromProviderDistribution(providerDistributionName,
      "http://localhost:8000",
      JWT.encodeAccessToken(AccessToken(consumerDistributionName), providerDistributionBuilder.config.get.jwtSecret),
      Some(Common.CommonConsumerProfile), "ak"))
    assert(consumerDistributionBuilder.updateDistributionFromProvider())
  }
}
