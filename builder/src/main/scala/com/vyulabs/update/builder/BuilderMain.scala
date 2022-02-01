package com.vyulabs.update.builder

import com.vyulabs.update.common.common.{Arguments, Common, ThreadTimer}
import com.vyulabs.update.common.config.{NamedStringValue, Repository, ServicePrivateFile}
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient}
import com.vyulabs.update.common.lock.SmartFilesLocker
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperVersion}
import org.slf4j.LoggerFactory
import spray.json._
import spray.json.DefaultJsonProtocol._

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
    "Use: <command> {[argument=value]}\n" +
    "  Commands:\n" +
    "    buildProviderDistribution [cloudProvider=?] distribution=? directory=?\n" +
    "       host=? port=? [sslKeyStoreFile=?] [sslKeyStorePassword=?]\n" +
    "       title=? mongoDbConnection=? mongoDbName=? [sourceBranches=?[,?]...] [persistent=?]\n" +
    "    buildConsumerDistribution [cloudProvider=?] distribution=? directory=?\n" +
    "       host=? port=? [sslKeyStoreFile=?] [sslKeyStorePassword=?]\n" +
    "       title=? mongoDbConnection=? mongoDbName=? provider=? providerUrl=?\n" +
    "       consumerAccessToken=? [testConsumerMatch=?] [persistent=?]\n" +
    "    buildDeveloperVersion service=? version=? sourceRepositories=? [privateFiles=?] [macroValues=?] comment=?\n" +
    "    buildClientVersion service=? developerVersion=? clientVersion=? [settingsRepositories=?] [privateFiles=?] [macroValues=?]"

  if (args.size < 1) Utils.error(usage())

  println("builder output")

  try {
    val command = args(0)

    command match {
      case "buildProviderDistribution" | "buildConsumerDistribution" =>
        val arguments = Arguments.parse(args.drop(1), Set.empty)

        val cloudProvider = arguments.getOptionValue("cloudProvider").getOrElse("None")
        val directory = arguments.getValue("directory")
        val distribution = arguments.getValue("distribution")
        val host = arguments.getValue("host")
        val port = arguments.getIntValue("port")
        val sslKeyStoreFile = arguments.getOptionValue("sslKeyStoreFile")
        val sslKeyStorePassword = arguments.getOptionValue("sslKeyStorePassword")
        val title = arguments.getValue("title")
        val mongoDbConnection = arguments.getValue("mongoDbConnection")
        val mongoDbName = arguments.getValue("mongoDbName")
        val persistent = arguments.getOptionBooleanValue("persistent").getOrElse(false)

        val distributionBuilder = new DistributionBuilder(cloudProvider,
          distribution, new File(directory), host, port, sslKeyStoreFile, sslKeyStorePassword,
          title, mongoDbConnection, mongoDbName, false, persistent)

        if (command == "buildProviderDistribution") {
          if (!distributionBuilder.buildDistributionFromSources(Common.AuthorBuilder)) {
            Utils.error("Build distribution error")
          }
        } else {
          val provider = arguments.getValue("provider")
          val providerUrl = arguments.getValue("providerUrl")
          val consumerAccessToken = arguments.getValue("consumerAccessToken")
          val testConsumer = arguments.getOptionValue("testConsumer")
          if (!distributionBuilder.buildFromProviderDistribution(
                provider, providerUrl, consumerAccessToken, testConsumer, Common.AuthorBuilder) ||
              !distributionBuilder.updateDistributionFromProvider()) {
            Utils.error("Build distribution error")
          }
        }
      case _ =>
        val distribution = System.getenv("distribution")
        if (distribution == null) {
          Utils.error("Environment variable distribution is not defined")
        }
        val distributionUrl = System.getenv("distributionUrl")
        if (distributionUrl == null) {
          Utils.error("Environment variable distributionUrl is not defined")
        }
        val accessToken = System.getenv("accessToken")
        if (accessToken == null) {
          Utils.error("Environment variable accessToken is not defined")
        }

        val arguments = Arguments.parse(args.drop(1), Set.empty)

        val macroValues = arguments.getOptionValue("macroValues")
          .map(_.parseJson.convertTo[Seq[NamedStringValue]]).getOrElse(Seq.empty)

        val httpClient = new HttpClientImpl(distributionUrl)
        httpClient.accessToken = Some(accessToken)
        val asyncDistributionClient = new DistributionClient(httpClient)
        val distributionClient = new SyncDistributionClient(asyncDistributionClient, FiniteDuration(60, TimeUnit.SECONDS))

        command match {
          case "buildDeveloperVersion" =>
            val author = arguments.getValue("author")
            val service = arguments.getValue("service")
            val version = DeveloperVersion.parse(arguments.getValue("version"))
            val sourceRepositories = arguments.getValue("sourceRepositories").parseJson.convertTo[Seq[Repository]]
            val privateFiles = arguments.getOptionValue("privateFiles")
              .map(_.parseJson.convertTo[Seq[ServicePrivateFile]]).getOrElse(Seq.empty)
            val comment = arguments.getValue("comment")
            val developerBuilder = new DeveloperBuilder(new File("."), distribution)
            val buildValues = macroValues.foldLeft(Map.empty[String, String])((m, e) => m + (e.name -> e.value))
            if (!developerBuilder.buildDeveloperVersion(distributionClient, author, service, version, comment,
                sourceRepositories, privateFiles, buildValues)) {
              Utils.error("Developer version is not generated")
            }
          case "buildClientVersion" =>
            val author = arguments.getValue("author")
            val service = arguments.getValue("service")
            val version = ClientDistributionVersion.parse(arguments.getValue("version"))
            val settingsRepositories = arguments.getOptionValue("settingsRepositories")
              .map(_.parseJson.convertTo[Seq[Repository]]).getOrElse(Seq.empty)
            val privateFiles = arguments.getOptionValue("privateFiles")
              .map(_.parseJson.convertTo[Seq[ServicePrivateFile]]).getOrElse(Seq.empty)
            var buildValues = macroValues.foldLeft(Map.empty[String, String])((m, e) => m + (e.name -> e.value))
            buildValues += ("distributionUrl" -> distributionUrl)
            buildValues += ("version" -> version.toString)
            val clientBuilder = new ClientBuilder(new File("."))
            if (!clientBuilder.buildClientVersion(distributionClient, service, version, author,
                settingsRepositories, privateFiles, buildValues)) {
              Utils.error("Client version is not generated")
            }
        }
    }
  } catch {
    case ex: Exception =>
      println(s"Error: ${ex.getMessage}")
      ex.printStackTrace()
      sys.exit(1)
  }

  sys.exit()
}