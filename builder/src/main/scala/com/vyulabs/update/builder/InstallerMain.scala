package com.vyulabs.update.builder

import com.vyulabs.update.common.com.vyulabs.common.utils.Arguments
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.{IoUtils, Utils}
import org.slf4j.LoggerFactory

import java.io.File
import java.net.{URI, URL}

/**
 * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 01.02.19.
 * Copyright FanDate, Inc.
 */
object InstallerMain extends App {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  val command = args(0)
  val arguments = Arguments.parse(args.drop(1))

  implicit val filesLocker = new SmartFilesLocker()

  IoUtils.synchronize(new File(s"installer.lock"), false,
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
            Utils.error("Init developer error")
          }

        case "initClient" =>
          val initClient = new BuildDistribution()
          val cloudProvider = arguments.getValue("cloudProvider")
          val distributionName = arguments.getValue("distributionName")
          val adminRepositoryUrl = new URI(arguments.getValue("adminRepositoryUrl"))
          val clientDistributionUrl = new URL(arguments.getValue("clientDistributionUrl"))
          val developerDistributionUrl = new URL(arguments.getValue("developerDistributionUrl"))
          val distributionServicePort = arguments.getIntValue("distributionServicePort")
          if (!initClient.initClient(cloudProvider, distributionName, adminRepositoryUrl, developerDistributionUrl, clientDistributionUrl, distributionServicePort)) {
            Utils.error("Init client error")
          }

        /* TODO graphql
        case "installUpdates" =>
          val updateClient = new UpdateClient()

          val config = InstallerConfig().getOrElse {
            Utils.error("No config")
          }

          log.info(s"Initialize admin repository")
          val adminRepository =
            AdminRepository(config.adminRepositoryUrl, new File("admin")).getOrElse {
              Utils.error("Admin repository initialize error")
            }

          val clientDistribution = new OldDistributionInterface(config.clientDistributionUrl)
          val developerDistribution = new OldDistributionInterface(config.developerDistributionUrl)

          val servicesOnly = arguments.getOptionValue("servicesOnly").map(_.split(",").toSet)
          val localConfigOnly = arguments.getOptionBooleanValue("localConfigOnly").getOrElse(false)
          if (localConfigOnly && servicesOnly.isEmpty) {
            Utils.error("Use option localConfigOnly with servicesOnly")
          }
          val setDesiredVersions = arguments.getOptionBooleanValue("setDesiredVersions").getOrElse(true)
          val result = updateClient.installUpdates(adminRepository, clientDistribution, developerDistribution,
               servicesOnly, localConfigOnly, setDesiredVersions)
          if (result == InstallResult.Complete) {
            log.info("Updates successfully installed")
          } else if (result == InstallResult.Failure) {
            Utils.error("Install update error")
          } else if (result == InstallResult.NeedRestartToUpdate) {
            Utils.restartToUpdate("Restart to update")
          }

        case "getDesiredVersions" =>
          val config = InstallerConfig().getOrElse {
            Utils.error("No config")
          }
          val updateClient = new UpdateClient()

          val clientDistributionUrl = config.clientDistributionUrl
          val clientDistribution = new OldDistributionInterface(clientDistributionUrl)

          val versions = updateClient.getClientDesiredVersions(clientDistribution).getOrElse {
            Utils.error("Can't get desired versions")
          }
          log.info("Desired versions:")
          versions.foreach { case (serviceName, version) => log.info(s"  ${serviceName} ${version}") }

        case "setDesiredVersions" =>
          val config = InstallerConfig().getOrElse {
            Utils.error("No config")
          }
          val updateClient = new UpdateClient()
          log.info(s"Initialize admin repository")
          val adminRepository =
            AdminRepository(config.adminRepositoryUrl, new File("admin")).getOrElse {
              Utils.error("Admin repository initialize error")
            }

          val clientDistribution = new OldDistributionInterface(config.clientDistributionUrl)

          var servicesVersions = Map.empty[ServiceName, Option[ClientDistributionVersion]]
          for (services <- arguments.getOptionValue("services")) {
            for (record <- services.split(',')) {
              val fields = record.split(":")
              if (fields.size == 1) {
                servicesVersions += (fields(0) -> None)
              } else if (fields.size == 2) {
                servicesVersions += (fields(0) -> Some(ClientDistributionVersion.parse(fields(1))))
              } else {
                Utils.error(s"Invalid service record ${record}")
              }
            }
          }

          log.info(s"Set desired versions ${servicesVersions}")
          if (!updateClient.setDesiredVersions(clientDistribution, servicesVersions)) {
            Utils.error("Desired versions assignment error")
          }

        case "signVersionsAsTested" =>
          val config = InstallerConfig().getOrElse {
            Utils.error("No config")
          }
          val updateClient = new UpdateClient()
          log.info(s"Initialize admin repository")

          val clientDistribution = new OldDistributionInterface(config.clientDistributionUrl)
          val developerDistribution = new OldDistributionInterface(config.developerDistributionUrl)

          if (!updateClient.signVersionsAsTested(clientDistribution, developerDistribution)) {
            Utils.error("Sign versions as tested error")
          }*/

        case command =>
          Utils.error(s"Invalid command ${command}\n${usage()}")
      }
    })
}
