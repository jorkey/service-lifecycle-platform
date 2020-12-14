package com.vyulabs.update.builder

import com.vyulabs.update.builder.config.BuilderConfig
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.common.com.vyulabs.common.utils.Arguments
import com.vyulabs.update.distribution.SettingsDirectory
import com.vyulabs.update.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.logger.{LogSender, TraceAppender}
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.{DeveloperDistributionVersion, DeveloperVersion}
import org.slf4j.LoggerFactory

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import DeveloperBuilder._
import ClientBuilder._

import javax.print.DocFlavor.URL

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 21.02.19.
  * Copyright FanDate, Inc.
  */
object BuilderMain extends App {
  implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val filesLocker = new SmartFilesLocker()
  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  def usage(): String =
    "Use: <command1> [arguments]\n" +
    "     runCommands \"<command1> [arguments]\" [\"<command2> [arguments]\"] ...\n" +
    "  Commands:\n" +
    "    buildDistribution <distributionConfigFile=value> <author=value> [sourceBranches=value1[,value2]...]\n" +
    "    buildDistribution <distributionConfigFile=value> <developerDistributionName=value>\n" +
    "    buildDeveloperVersion <distributionName=value> <author=value> <service=value> [version=value] [comment=value] [sourceBranches=value1[,value2]...] [setDesiredVersion=true|false]\n" +
    "    downloadUpdates <distributionName=value> <developerDistributionName=value> [services=<service1>[,<service2>]...]\n" +
    "    buildClientVersions <distributionName=value> <author=value> <services=<service1>[,<service2>]...> [setDesiredVersion=true|false]\n" +
    "    signVersionsAsTested <distributionName=value> <developerDistributionName=value>"

  if (args.size < 1) {
    Utils.error(usage())
  }

  val command = args(0)
  val config = BuilderConfig().getOrElse { Utils.error("No config") }

  val commands: Map[String, Array[String]] = args(0) match {
    case "runCommands" =>
      args.drop(1).foldLeft(Map.empty[String, Array[String]])((map, cmd) => {
        val args = cmd.split(" ")
        map + (args(0) -> args.drop(1))
      })
    case _ =>
      Map(args(0) -> args.drop(1))
  }

  commands foreach {
    case (command, args) =>
      command match {
        case "buildDistribution" =>
          val arguments = Arguments.parse(args.drop(1), Set.empty)
          val buildDistribution = new BuildDistribution()

          arguments.getOptionValue("author") match {
            case Some(author) =>
              val arguments = Arguments.parse(args.drop(1), Set("distributionConfigFile", "author", "sourceBranches"))
              val distributionConfigFile = new File(arguments.getValue("distributionConfigFile"))
              val sourceBranches = arguments.getOptionValue("sourceBranches").map(_.split(",").toSeq).getOrElse(Seq.empty)
              if (!buildDistribution.buildFromSources(author, sourceBranches, distributionConfigFile)) {
                Utils.error("Build distribution error")
              }
            case None =>
              val arguments = Arguments.parse(args.drop(1), Set("distributionConfigFile", "developerDistributionName"))
              val distributionConfigFile = new File(arguments.getValue("distributionConfigFile"))
              arguments.getOptionValue("developerDistributionName") match {
                case Some(developerDistributionName) =>
                  if (!buildDistribution.buildFromDeveloperDistribution(developerDistributionName, distributionConfigFile)) {
                    Utils.error("Build distribution error")
                  }
                case None =>
                  Utils.error(usage())
              }
          }
        case _ =>
          val arguments = Arguments.parse(args.drop(1), Set.empty)

          val adminRepository = SettingsDirectory(config.adminRepositoryUrl, new File("admin")).getOrElse {
            Utils.error("Init admin repository error")
          }

          val distributionName = arguments.getValue("distributionName")
          val distributionUrl = config.distributionLinks.find(_.distributionName == distributionName).map(_.distributionUrl).getOrElse {
            Utils.error(s"Unknown URL to distribution ${distributionName}")
          }
          val distributionClient = new DistributionClient(distributionName, new HttpClientImpl(distributionUrl))
          val syncDistributionClient = new SyncDistributionClient(distributionClient, FiniteDuration(60, TimeUnit.SECONDS))
          TraceAppender.handleLogs(new LogSender(Common.DistributionServiceName, config.instanceId, distributionClient))

          command match {
            case "buildDeveloperVersion" =>
              val author = arguments.getValue("author")
              val serviceName = arguments.getValue("service")
              val version = arguments.getOptionValue("version").map(DeveloperVersion.parse(_))
              val comment: Option[String] = arguments.getOptionValue("comment")
              val sourceBranches = arguments.getOptionValue("sourceBranches").map(_.split(",").toSeq).getOrElse(Seq.empty)
              val setDesiredVersion = arguments.getOptionBooleanValue("setDesiredVersion").getOrElse(true)
              buildDeveloperVersion(syncDistributionClient, adminRepository, author, serviceName, version, comment, sourceBranches) match {
                case Some(newBuilderVersion) =>
                  if (setDesiredVersion && !setDeveloperDesiredVersions(syncDistributionClient, Map(serviceName -> Some(DeveloperDistributionVersion(distributionName, newBuilderVersion))))) {
                    Utils.error("Can't set developer desired version")
                  }
                case None =>
                  Utils.error("Developer version is not generated")
              }
            case "downloadUpdates" =>
            //"    downloadUpdates <distributionName=value> <developerDistributionName=value> [services=<service1>[,<service2>]...]\n" +
            case "buildClientVersions" =>
              //"    buildClientVersions <distributionName=value> <author=value> <services=<service1>[,<service2>]...> [setDesiredVersion=true|false]\n" +
              val author = arguments.getValue("author")
              val serviceNames = arguments.getOptionValue("services").map(_.split(",").toSeq).getOrElse(Seq.empty)
              val setDesiredVersion = arguments.getOptionBooleanValue("setDesiredVersion").getOrElse(true)
              val settings = Map("distribDirectoryUrl" -> distributionUrl.toString)
              buildClientVersions(syncDistributionClient, adminRepository, author, serviceNames, settings)

            case "setClientDesiredVersions" =>
              Set("services")
            case "signVersionsAsTested" =>
              Set("developerDistributionName")
            case _ =>
              Utils.error(s"Invalid command ${command}\n${usage()}")
          }
      }
  }
}