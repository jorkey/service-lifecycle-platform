package com.vyulabs.update.builder

import com.vyulabs.update.builder.config.BuilderConfig
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.common.com.vyulabs.common.utils.Arguments
import com.vyulabs.update.distribution.AdminRepository
import com.vyulabs.update.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.logger.{LogBuffer, LogSender, TraceAppender}
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.{DeveloperDistributionVersion, DeveloperVersion}
import org.slf4j.LoggerFactory

import java.io.File
import java.util.concurrent.TimeUnit
import java.util.{Timer, TimerTask}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 21.02.19.
  * Copyright FanDate, Inc.
  */
object BuilderMain extends App {
  implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val filesLocker = new SmartFilesLocker()
  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  def usage(): String =
    "Use: buildVersion <author=value> <service=value>\n" +
    "                 [version=value] [comment=value] [sourceBranches=value1,value2,...] [setDesiredVersion=true]\n" +
    "   getDesiredVersions\n" +
    "   setDesiredVersions[services=<service[:version]>,[service1[:version1]],...]"

  if (args.size < 1) {
    Utils.error(usage())
  }

  val command = args(0)
  if (command != "buildVersion" && command != "getDesiredVersions" && command != "setDesiredVersions") {
    Utils.error(usage())
  }
  val arguments = Arguments.parse(args.drop(1))

  val config = BuilderConfig().getOrElse {
    Utils.error("No config")
  }

  val distributionClient = new DistributionClient(config.distributionName, new HttpClientImpl(config.distributionUrl))
  val syncDistributionClient = new SyncDistributionClient(distributionClient, FiniteDuration(60, TimeUnit.SECONDS))

  // TODO localise this code
  val logSender = new LogSender(Common.DistributionServiceName, config.instanceId, distributionClient)
  val logger = Utils.getLogbackLogger(this.getClass)
  val appender = logger.getAppender("TRACE").asInstanceOf[TraceAppender]
  val buffer = new LogBuffer(logSender, 25, 1000)
  appender.addListener(buffer)
  new Timer().schedule(new TimerTask() {
    override def run(): Unit = buffer.flush()
  }, 1000)

  command match {
    case "buildVersion" =>
      val author: String = arguments.getValue("author")
      val serviceName: ServiceName = arguments.getValue("service")
      val comment: Option[String] = arguments.getOptionValue("comment")
      val version = arguments.getOptionValue("version").map(DeveloperVersion.parse(_))
      val sourceBranches = arguments.getOptionValue("sourceBranches").map(_.split(",").toSeq).getOrElse(Seq.empty)
      val setDesiredVersion = arguments.getOptionBooleanValue("setDesiredVersion").getOrElse(true)

      log.info(s"Make new version of service ${serviceName}")
      val adminRepository = AdminRepository(config.adminRepositoryUrl, new File("admin")).getOrElse {
        Utils.error("Init admin repository error")
      }
      val builder = new Builder(syncDistributionClient)
      builder.makeVersion(adminRepository.getDirectory(), author, serviceName, comment, version, sourceBranches) match {
        case Some(version) =>
          if (setDesiredVersion) {
            builder.setDesiredVersions(Map(serviceName -> Some(DeveloperDistributionVersion(config.distributionName, version))))
          }
        case None =>
          log.error("Make version error")
          System.exit(1)
      }

    case "getDesiredVersions" =>
      new Builder(syncDistributionClient).getDesiredVersions() match {
        case Some(versions) =>
          log.info("Desired versions:")
          versions.foreach { case (serviceName, version) => log.info(s"  ${serviceName} ${version}") }
        case None =>
          Utils.error("Get desired versions error")
      }

    case "setDesiredVersions" =>
      var servicesVersions = Map.empty[ServiceName, Option[DeveloperDistributionVersion]]

      for (services <- arguments.getOptionValue("services")) {
        val servicesRecords: Seq[String] = services.split(",")
        for (record <- servicesRecords) {
          val fields = record.split(":")
          if (fields.size == 2) {
            val version = if (fields(1) != "-") {
              Some(DeveloperDistributionVersion.parse(fields(1)))
            } else {
              None
            }
            servicesVersions += (fields(0) -> version)
          } else {
            Utils.error(s"Invalid service record ${record}")
          }
        }
      }

      if (!new Builder(syncDistributionClient).setDesiredVersions(servicesVersions)) {
        Utils.error("Set desired versions error")
      }

    case command =>
      Utils.error(s"Invalid command ${command}\n${usage()}")
  }
}