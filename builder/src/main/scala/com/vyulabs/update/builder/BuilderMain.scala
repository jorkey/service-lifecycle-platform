package com.vyulabs.update.builder

import com.vyulabs.update.builder.ClientBuilder._
import com.vyulabs.update.builder.DeveloperBuilder._
import com.vyulabs.update.builder.DistributionBuilder._
import com.vyulabs.update.builder.config.BuilderConfig
import com.vyulabs.update.common.common.{Arguments, ThreadTimer}
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient}
import com.vyulabs.update.common.distribution.server.SettingsDirectory
import com.vyulabs.update.common.lock.SmartFilesLocker
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.GitRepositoryUtils
import org.slf4j.LoggerFactory

import java.io.File
import java.util.concurrent.TimeUnit
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
  implicit val timer = new ThreadTimer()

  def usage(): String =
    "Use: <command> [arguments]\n" +
    "  Commands:\n" +
    "    buildDistribution <distributionName=value> <author=value> [sourceBranches=value1[,value2]...]\n" +
    "    buildDistribution <distributionName=value> <developerDistributionName=value>\n" +
    "    buildDeveloperVersion <distributionName=value> <service=value> <version=value> [comment=value] [sourceBranches=value1[,value2]...]\n" +
    "    buildClientVersion <distributionName=value> <service=value> <developerVersion=value> <clientVersion=value>"

  if (args.size < 1) Utils.error(usage())

  val command = args(0)
  val config = BuilderConfig().getOrElse { Utils.error("No config") }

  val settingsDir = GitRepositoryUtils.getGitRepository(config.adminRepositoryUrl, "master",
    false, new File("settings")).getOrElse {
    Utils.error("Init settings repository error")
  }.getDirectory()
  val settingsDirectory  = new SettingsDirectory(settingsDir)

  command match {
    case "buildDistribution" =>
      val arguments = Arguments.parse(args.drop(1), Set.empty)

      arguments.getOptionValue("author") match {
        case Some(author) =>
          val arguments = Arguments.parse(args.drop(1), Set("distributionName", "author", "sourceBranches"))
          val distributionName = arguments.getValue("distributionName")
          val sourceBranch = arguments.getOptionValue("sourceBranch").getOrElse("master")
          if (!buildDistributionFromSources(distributionName, settingsDirectory, sourceBranch, author)) {
            Utils.error("Build distribution error")
          }
        case None =>
          val arguments = Arguments.parse(args.drop(1), Set("distributionConfigFile", "developerDistributionName"))
          val distributionConfigFile = new File(arguments.getValue("distributionConfigFile"))
          arguments.getOptionValue("developerDistributionName") match {
            case Some(developerDistributionName) =>
              if (!buildFromDeveloperDistribution(developerDistributionName, distributionConfigFile)) {
                Utils.error("Build distribution error")
              }
            case None =>
              Utils.error(usage())
          }
      }
    case _ =>
      val arguments = Arguments.parse(args.drop(1), Set.empty)

      val distributionName = arguments.getValue("distributionName")
      val distributionUrl = config.distributionLinks.find(_.distributionName == distributionName).map(_.distributionUrl).getOrElse {
        Utils.error(s"Unknown URL to distribution ${distributionName}")
      }
      val asyncDistributionClient = new DistributionClient(distributionName, new HttpClientImpl(distributionUrl))
      val distributionClient = new SyncDistributionClient(asyncDistributionClient, FiniteDuration(60, TimeUnit.SECONDS))
      //TraceAppender.handleLogs(new LogSender(Common.DistributionServiceName, config.instanceId, asyncDistributionClient))

      command match {
        case "buildDeveloperVersion" =>
          val author = arguments.getValue("author")
          val serviceName = arguments.getValue("service")
          val version = DeveloperVersion.parse(arguments.getValue("version"))
          val comment: Option[String] = arguments.getOptionValue("comment")
          val sourceBranches = arguments.getOptionValue("sourceBranches").map(_.split(",").toSeq).getOrElse(Seq.empty)
          if (!buildDeveloperVersion(distributionClient, settingsDirectory, author, serviceName,
            DeveloperDistributionVersion(distributionName, version), comment, sourceBranches)) {
            Utils.error("Developer version is not generated")
          }
        case "buildClientVersion" =>
          val author = arguments.getValue("author")
          val serviceName = arguments.getValue("service")
          val developerVersion = DeveloperVersion.parse(arguments.getValue("developerVersion"))
          val clientVersion = ClientVersion.parse(arguments.getValue("clientVersion"))
          val buildArguments = Map("distribDirectoryUrl" -> distributionUrl.toString, "version" -> clientVersion.toString)
          if (!buildClientVersion(distributionClient, settingsDirectory, serviceName,
              DeveloperDistributionVersion(distributionName, developerVersion),
              ClientDistributionVersion(distributionName, clientVersion), author, buildArguments)) {
            Utils.error("Client version is not generated")
          }
      }
  }
}