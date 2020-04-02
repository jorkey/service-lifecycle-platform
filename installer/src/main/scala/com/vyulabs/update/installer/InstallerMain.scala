package com.vyulabs.update.installer

import java.io.File
import java.net.{URI, URL}

import com.vyulabs.update.common.Common
import com.vyulabs.update.distribution.distribution.ClientAdminRepository
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.common.com.vyulabs.common.utils.Arguments
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectoryClient
import com.vyulabs.update.installer.config.InstallerConfig
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.LoggerFactory

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 01.02.19.
  * Copyright FanDate, Inc.
  */
object InstallerMain extends App {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def usage() =
    "Arguments: initDeveloper <distributionServicePort=value>\n" +
    "           initClient <clientName=value> [testClientMatch=regularValue] <adminRepositoryUrl=value> <clientDistributionUrl=value> <developerDistributionUrl=value> <distributionServicePort=value>\n" +
    "           installUpdates [servicesOnly=<service1>:[-<profile>][,...]] [localConfigOnly=true] [setDesiredVersions=true]\n" +
    "           getDesiredVersions\n" +
    "           setDesiredVersions [services=<service[:version]>,[service1[:version1]],...]\n" +
    "           signVersionsAsTested"

  if (args.size < 1) {
    sys.error(usage())
  }

  val command = args(0)
  val arguments = Arguments.parse(args.drop(1))

  implicit val filesLocker = new SmartFilesLocker()

  Utils.synchronize(new File(s"installer.lock"), false,
    (attempt, _) => {
      if (attempt == 1) {
        log.info("Another installer is running - wait ...")
      }
      Thread.sleep(5000)
      true
    },
    () => {
      command match {
        case "initDeveloper" =>
          val initDeveloper = new InitDeveloper()
          val distributionServicePort = arguments.getIntValue("distributionServicePort")
          if (!initDeveloper.initDeveloper(distributionServicePort)) {
            sys.error("Init developer error")
          }

        case "initClient" =>
          val initClient = new InitClient()
          val clientName = arguments.getValue("clientName")
          val testClientMatch = arguments.getOptionValue("testClientMatch").map(_.r)
          val adminRepositoryUri = new URI(arguments.getValue("adminRepositoryUrl"))
          val clientDistributionUrl = new URL(arguments.getValue("clientDistributionUrl"))
          val developerDistributionUrl = new URL(arguments.getValue("developerDistributionUrl"))
          val distributionServicePort = arguments.getIntValue("distributionServicePort")
          if (!initClient.initClient(clientName, testClientMatch, adminRepositoryUri, developerDistributionUrl, clientDistributionUrl, distributionServicePort)) {
            sys.error("Init client error")
          }

        case "installUpdates" =>
          val updateClient = new UpdateClient()

          val config = InstallerConfig().getOrElse {
            sys.error("No config")
          }

          log.info(s"Initialize admin repository")
          val adminRepository =
            ClientAdminRepository(config.adminRepositoryUri, new File("admin")).getOrElse {
              sys.error("Admin repository initialize error")
            }

          val clientDistribution = new ClientDistributionDirectoryClient(config.clientDistributionUrl)
          val developerDistribution = new DeveloperDistributionDirectoryClient(config.developerDistributionUrl)

          val servicesOnly = arguments.getOptionValue("servicesOnly")
            .map(_.split(",").foldLeft(Set.empty[ServiceName])((set, record) => {
              if (record == Common.InstallerServiceName) {
                sys.error("No need to update installer. It updates itself when running.")
              }
              set + record
            }))
          val localConfigOnly = arguments.getOptionBooleanValue("localConfigOnly").getOrElse(false)
          if (localConfigOnly && servicesOnly.isEmpty) {
            sys.error("Use option localConfigOnly with servicesOnly")
          }
          val setDesiredVersions = arguments.getOptionBooleanValue("setDesiredVersions").getOrElse(true)
          if (!updateClient.installUpdates(config.clientName, config.testClientMatch, adminRepository, clientDistribution, developerDistribution,
               servicesOnly, localConfigOnly, setDesiredVersions)) {
            sys.error("Install update error")
          }

        case "getDesiredVersions" =>
          val config = InstallerConfig().getOrElse {
            sys.error("No config")
          }
          val updateClient = new UpdateClient()

          val clientDistributionUrl = config.clientDistributionUrl
          val clientDistribution = new ClientDistributionDirectoryClient(clientDistributionUrl)

          val versions = updateClient.getClientDesiredVersions(clientDistribution).getOrElse {
            sys.error("Can't get desired versions")
          }
          log.info("Desired versions:")
          versions.foreach { case (serviceName, version) => log.info(s"  ${serviceName} ${version}") }

        case "setDesiredVersions" =>
          val config = InstallerConfig().getOrElse {
            sys.error("No config")
          }
          val updateClient = new UpdateClient()
          log.info(s"Initialize admin repository")
          val adminRepository =
            ClientAdminRepository(config.adminRepositoryUri, new File("admin")).getOrElse {
              sys.error("Admin repository initialize error")
            }

          val clientDistribution = new ClientDistributionDirectoryClient(config.clientDistributionUrl)

          var servicesVersions = Map.empty[ServiceName, Option[BuildVersion]]
          for (services <- arguments.getOptionValue("services")) {
            for (record <- services.split(',')) {
              val fields = record.split(":")
              if (fields.size == 1) {
                servicesVersions += (fields(0) -> None)
              } else if (fields.size == 2) {
                servicesVersions += (fields(0) -> Some(BuildVersion.parse(fields(1))))
              } else {
                sys.error(s"Invalid service record ${record}")
              }
            }
          }

          log.info(s"Set desired versions ${servicesVersions}")
          if (!updateClient.setDesiredVersions(adminRepository, clientDistribution, servicesVersions)) {
            sys.error("Desired versions assignment error")
          }

        case "signVersionsAsTested" =>
          val config = InstallerConfig().getOrElse {
            sys.error("No config")
          }
          val updateClient = new UpdateClient()
          log.info(s"Initialize admin repository")
          val adminRepository =
            ClientAdminRepository(config.adminRepositoryUri, new File("admin")).getOrElse {
              sys.error("Admin repository initialize error")
            }

          val clientDistribution = new ClientDistributionDirectoryClient(config.clientDistributionUrl)
          val developerDistribution = new DeveloperDistributionDirectoryClient(config.developerDistributionUrl)

          if (!updateClient.signVersionsAsTested(adminRepository, clientDistribution, developerDistribution)) {
            sys.error("Sign versions as tested error")
          }

        case command =>
          sys.error(s"Invalid command ${command}\n${usage()}")
      }
    })
}