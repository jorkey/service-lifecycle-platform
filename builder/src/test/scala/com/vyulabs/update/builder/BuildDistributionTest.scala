package com.vyulabs.update.builder

import com.vyulabs.update.common.accounts.ConsumerAccountProperties
import com.vyulabs.update.common.common.{Common, JWT}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.AccessToken
import com.vyulabs.update.common.process.ChildProcess
import com.vyulabs.update.distribution.mongo.MongoDb
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class BuildDistributionTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "BuildDistribution"

  implicit val log = LoggerFactory.getLogger(this.getClass)

  val providerDistributionName = "test-provider"
  val providerDistributionDir = Files.createTempDirectory("distrib").toFile

  val consumerDistributionName = "test-consumer"
  val consumerDistributionDir = Files.createTempDirectory("distrib").toFile

  val providerMongoDbName = "BuildDistributionTest-provider"
  val consumerMongoDbName = "BuildDistributionTest-consumer"

  var processes = Set.empty[ChildProcess]

  override protected def beforeAll(): Unit = {
    Await.result(new MongoDb(providerMongoDbName).dropDatabase(), FiniteDuration(3, TimeUnit.SECONDS))
    Await.result(new MongoDb(consumerMongoDbName).dropDatabase(), FiniteDuration(3, TimeUnit.SECONDS))
  }

  override protected def afterAll(): Unit = {
    synchronized {
      processes.foreach(_.terminate())
    }
    Await.result(new MongoDb(providerMongoDbName).dropDatabase(), FiniteDuration(3, TimeUnit.SECONDS))
    Await.result(new MongoDb(consumerMongoDbName).dropDatabase(), FiniteDuration(3, TimeUnit.SECONDS))
  }

  it should "build provider and consumer distribution servers" in {
    log.info("")
    log.info(s"########################### Build provider distribution from sources")
    log.info("")
    val providerDistributionBuilder = new DistributionBuilder(
      "None", () => startService(providerDistributionDir), new DistributionDirectory(providerDistributionDir),
      providerDistributionName, "Test developer distribution server",
      providerMongoDbName,true, 8000)
    assert(providerDistributionBuilder.buildDistributionFromSources())
    assert(providerDistributionBuilder.addDistributionAccounts())
    assert(providerDistributionBuilder.generateAndUploadInitialVersions("ak"))
    assert(providerDistributionBuilder.addCommonServicesProfile())
    assert(providerDistributionBuilder.addOwnServicesProfile())
    assert(providerDistributionBuilder.addConsumerAccount(consumerDistributionName,
      "Distribution Consumer", ConsumerAccountProperties(Common.CommonServiceProfile, "http://localhost:8001")))

    log.info("")
    log.info(s"########################### Build consumer distribution from provider distribution")
    log.info("")
    val consumerDistributionBuilder = new DistributionBuilder(
      "None", () => startService(consumerDistributionDir), new DistributionDirectory(consumerDistributionDir),
      consumerDistributionName, "Test client distribution server",
      consumerMongoDbName,true, 8001)

    assert(consumerDistributionBuilder.buildFromProviderDistribution(providerDistributionName,
      "http://localhost:8000", Common.AdminAccount,
      JWT.encodeAccessToken(AccessToken(consumerDistributionName), providerDistributionBuilder.config.get.jwtSecret),
      Common.CommonConsumerProfile, None, "ak"))
    assert(consumerDistributionBuilder.addDistributionAccounts())
    assert(consumerDistributionBuilder.updateDistributionFromProvider())
  }

  def startService(directory: File): Boolean = {
    log.info("Start distribution server")
    val startProcess = ChildProcess.start("/bin/sh", Seq("distribution.sh"), Map.empty, directory)
    startProcess.onComplete {
      case Success(process) =>
        log.info("Distribution server started")
        synchronized {
          this.processes += process
        }
      case Failure(ex) =>
        sys.error("Can't start distribution process")
        ex.printStackTrace()
    }
    true
  }
}
