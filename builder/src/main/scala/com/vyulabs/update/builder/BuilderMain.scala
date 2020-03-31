package com.vyulabs.update.builder

import com.vyulabs.update.builder.config.BuilderConfig
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.common.com.vyulabs.common.utils.Arguments
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectoryClient
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.version.BuildVersion
import org.slf4j.LoggerFactory

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 21.02.19.
  * Copyright FanDate, Inc.
  */
object BuilderMain extends App {
  implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val filesLocker = new SmartFilesLocker()

  def usage(): String =
    "Use: buildVersion <author=value> <service=value> [client=value]\n" +
    "                 [version=value] [comment=value] [sourceBranches=value1,value2,...] [setDesiredVersion=true]\n" +
    "   getDesiredVersions [clientName=value]\n" +
    "   setDesiredVersions [clientName=value] [services=<service[:version]>,[service1[:version1]],...] [tested=true]"

  if (args.size < 1) {
    sys.error(usage())
  }

  val command = args(0)
  if (command != "buildVersion" && command != "getDesiredVersions" && command != "setDesiredVersions") {
    sys.error(usage())
  }
  val arguments = Arguments.parse(args.drop(1))

  val config = BuilderConfig().getOrElse {
    sys.error("No config")
  }

  val developerDistribution = DeveloperDistributionDirectoryClient(config.developerDistributionUrl)

  command match {
    case "buildVersion" =>
      val author: String = arguments.getValue("author")
      val serviceName: ServiceName = arguments.getValue("service")
      val clientName: Option[ClientName] = arguments.getOptionValue("client")
      val comment: Option[String] = arguments.getOptionValue("comment")
      val version = {
        arguments.getOptionValue("version").map(BuildVersion.parse(_)) map { version =>
          clientName match {
            case Some(clientName) =>
              version.client match {
                case Some(client) if (client != clientName) =>
                  sys.error(s"Client name in the version ${client} != client ${clientName}")
                case Some(_) =>
                  version
                case None =>
                  BuildVersion.apply(clientName, version.build)
              }
            case None =>
              version
          }
        }
      }
      val sourceBranches = arguments.getOptionValue("sourceBranches").map(_.split(",").toSeq).getOrElse(Seq.empty)
      val setDesiredVersion = arguments.getOptionBooleanValue("setDesiredVersion").getOrElse(true)

      log.info(s"Make new version of service ${serviceName}")
      val builder = new Builder(developerDistribution, config.adminRepositoryUri)
      builder.makeVersion(author, serviceName, clientName, comment, version, sourceBranches) match {
        case Some(version) =>
          if (setDesiredVersion) {
            builder.setDesiredVersions(version.client, Some(Map(serviceName -> Some(version))), false)
            if (serviceName == Common.DistributionServiceName) {
              log.info("Update distribution server")
              if (!developerDistribution.waitForServerUpdated(version)) {
                log.error("Can't update distribution server")
              }
            }
          }
        case None =>
          log.error("Make version error")
          System.exit(1)
      }
    case "getDesiredVersions" =>
      val clientName: Option[ClientName] = arguments.getOptionValue("clientName")
      clientName match {
        case Some(clientName) =>
          val commonVersions = new Builder(developerDistribution, config.adminRepositoryUri).getDesiredVersions(None).getOrElse {
            sys.error("Get desired versions error")
          }
          val clientVersions = new Builder(developerDistribution, config.adminRepositoryUri).getDesiredVersions(Some(clientName))
            .getOrElse(Map.empty)
          log.info("Common desired versions:")
          commonVersions.foreach { case (serviceName, version) => {
            if (!clientVersions.contains(serviceName)) log.info(s"  ${serviceName} ${version}")
          }
          }
          if (!clientVersions.isEmpty) {
            log.info("Client desired versions:")
            clientVersions.foreach { case (serviceName, version) => log.info(s"  ${serviceName} ${version}") }
          }

        case None =>
          new Builder(developerDistribution, config.adminRepositoryUri).getDesiredVersions(None) match {
            case Some(versions) =>
              log.info("Desired versions:")
              versions.foreach { case (serviceName, version) => log.info(s"  ${serviceName} ${version}") }
            case None =>
              sys.error("Get desired versions error")
          }
      }
    case "setDesiredVersions" =>
      val clientName: Option[ClientName] = arguments.getOptionValue("clientName")
      var servicesVersions = Option.empty[Map[ServiceName, Option[BuildVersion]]]

      for (services <- arguments.getOptionValue("services")) {
        var versions = Map.empty[ServiceName, Option[BuildVersion]]
        val servicesRecords: Seq[String] = services.split(",")
        for (record <- servicesRecords) {
          val fields = record.split(":")
          if (fields.size == 2) {
            val version = if (fields(1) != "-") {
              Some(BuildVersion.parse(fields(1)))
            } else {
              None
            }
            versions += (fields(0) -> version)
          } else {
            sys.error(s"Invalid service record ${record}")
          }
        }
        servicesVersions = Some(versions)
      }

      val tested = arguments.getOptionBooleanValue("tested").getOrElse(false)

      if (!new Builder(developerDistribution, config.adminRepositoryUri).setDesiredVersions(clientName, servicesVersions, tested)) {
        sys.error("Set desired versions error")
      }
      for (servicesVersions <- servicesVersions) {
        for (distributionVersion <- servicesVersions.get(Common.DistributionServiceName)) {
          for (distributionVersion <- distributionVersion) {
            log.info("Update distribution server")
            if (!developerDistribution.waitForServerUpdated(distributionVersion)) {
              log.error("Can't update distribution server")
            }
          }
        }
      }
    case command =>
      sys.error(s"Invalid command ${command}\n${usage()}")
  }
}