package com.vyulabs.update.updater

import com.vyulabs.update.common.common.{Arguments, Common, ThreadTimer}
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.administratorQueries
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.info.{ClientDesiredVersions, ProfiledServiceName}
import com.vyulabs.update.common.logger.{LogUploader, PrefixedLogger, TraceAppender}
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.common.version.ClientDistributionVersion
import com.vyulabs.update.updater.config.UpdaterConfig
import com.vyulabs.update.updater.uploaders.StateUploader
import org.slf4j.LoggerFactory

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 24.12.18.
  * Copyright FanDate, Inc.
  */
object UpdaterMain extends App { self =>
  implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })
  implicit val timer = new ThreadTimer()

  if (args.size < 1) {
    Utils.error(usage())
  }

  def usage(): String = {
    "Use: runServices <services=<service1>:[-<profile>][,...]>"
  }

  val command = args(0)

  var blacklist = Map.empty[ProfiledServiceName, ClientDistributionVersion]

  val config = UpdaterConfig().getOrElse {
    Utils.error("No config")
  }

  val distributionClient = new DistributionClient(new HttpClientImpl(config.distributionUrl, Some(config.accessToken)))

  TraceAppender.handleLogs("Updater", "PROCESS",
    new LogUploader[SyncSource](Common.DistributionServiceName, None, config.instance, distributionClient))

  command match {
    case "runServices" =>
      val arguments = Arguments.parse(args.drop(1), Set("services"))

      val servicesInstanceNames = arguments.getValue("services").split(",").foldLeft(Set.empty[ProfiledServiceName])((set, record) =>
        set + ProfiledServiceName.parse(record)
      )

      log.info(s"-------------------------- Start updater of version ${Utils.getManifestBuildVersion(Common.UpdaterServiceName)} of services ${servicesInstanceNames} -------------------------")

      val updaterServiceName = ProfiledServiceName(Common.UpdaterServiceName)

      val instanceState = new StateUploader(new File("."), config.instance, servicesInstanceNames + updaterServiceName, distributionClient)

      instanceState.start()

      try {
        val updaterServiceController = instanceState.getServiceStateController(updaterServiceName).get
        val selfUpdater = new SelfUpdater(updaterServiceController, distributionClient)

        val serviceUpdaters = servicesInstanceNames.foldLeft(Map.empty[ProfiledServiceName, ServiceUpdater])((updaters, service) => {
          implicit val serviceLogger = new PrefixedLogger(s"Service ${service.toString}: ", log)
          updaters + (service -> new ServiceUpdater(config.instance, service, instanceState.getServiceStateController(service).get, distributionClient))
        })

        var lastUpdateTime = 0L

        var stopping = false
        var stopped = false

        Runtime.getRuntime.addShutdownHook(new Thread() {
          override def run(): Unit = {
            log.info("Shutdown hook")
            self.synchronized {
              if (!stopped) {
                stopping = true
                self.notify()
              }
            }
            while (self.synchronized {
              if (!stopped) {
                self.wait(100)
              }
              !stopped
            }) {}
          }
        })

        var servicesStarted = false
        var mustStop = false
        var mustExitToUpdate = false

        while (!mustStop) {
          try {
            mustStop = self.synchronized {
              if (servicesStarted) {
                self.wait(1000)
              }
              stopping
            }
            if (!mustStop) {
              mustExitToUpdate = maybeUpdate()
              if (mustExitToUpdate) {
                mustStop = true
              }
            }
            if (mustStop) {
              log.info("Updater is terminating")
              close()
              log.info("All are terminated")
              self.synchronized {
                stopped = true
                self.notify()
              }
              if (mustExitToUpdate) {
                Utils.restartToUpdate("Restart to update")
              }
            } else if (!servicesStarted) {
              serviceUpdaters.values.foreach { updater =>
                if (!updater.isExecuted()) {
                  updater.execute()
                }
              }
              servicesStarted = true
            }
          } catch {
            case e: Exception =>
              instanceState.error("Exception", e)
          }
        }

        def maybeUpdate(): Boolean = {
          if (System.currentTimeMillis() - lastUpdateTime > 10000) {
            val syncDistributionClient = new SyncDistributionClient[SyncSource](distributionClient, FiniteDuration(60, TimeUnit.SECONDS))
            syncDistributionClient.graphqlRequest(administratorQueries.getClientDesiredVersions(
                Seq(Common.ScriptsServiceName, Common.UpdaterServiceName) ++ serviceUpdaters.map(_._1.name))) match {
              case Some(desiredVersions) =>
                val desiredVersionsMap = ClientDesiredVersions.toMap(desiredVersions)
                var needUpdate = serviceUpdaters.foldLeft(Map.empty[ProfiledServiceName, ClientDistributionVersion])((map, updater) => {
                  updater._2.needUpdate(desiredVersionsMap.get(updater._1.name)) match {
                    case Some(version) =>
                      map + (updater._1 -> version)
                    case None =>
                      map
                  }
                })
                if (!needUpdate.isEmpty) {
                  selfUpdater.needUpdate(Common.UpdaterServiceName,
                      desiredVersionsMap.get(Common.UpdaterServiceName)).foreach(version =>
                    needUpdate += (ProfiledServiceName(Common.UpdaterServiceName) -> version))
                  selfUpdater.needUpdate(Common.ScriptsServiceName,
                      desiredVersionsMap.get(Common.ScriptsServiceName)).foreach(version =>
                    needUpdate += (ProfiledServiceName(Common.ScriptsServiceName) -> version))
                  val toUpdate = needUpdate.filterNot { case (service, version) =>
                    blacklist.get(service) match {
                      case Some(errorVersion) =>
                        if (errorVersion == version) {
                          log.info(s"Version ${version} of service ${service} is blacklisted.")
                          true
                        } else {
                          log.info(s"Clear blacklist of service ${service}.")
                          blacklist -= service
                          false
                        }
                      case None =>
                        false
                    }
                  }
                  if (update(toUpdate)) {
                    return true
                  }
                }
              case None =>
                log.error("Can't get desired versions")
            }
            lastUpdateTime = System.currentTimeMillis()
          }
          false
        }

        def update(toUpdate: Map[ProfiledServiceName, ClientDistributionVersion]): Boolean = {
          var errorUpdates = toUpdate.filterNot { case (service, version) =>
            if (Common.isUpdateService(service.name)) {
              selfUpdater.beginServiceUpdate(service.name, version)
            } else {
              serviceUpdaters.get(service).get.beginInstall(version)
            }
          }
          errorUpdates ++= toUpdate.filterKeys(!errorUpdates.contains(_)).filterNot { case (service, version) =>
            if (!Common.isUpdateService(service.name)) {
              serviceUpdaters.get(service).get.finishInstall(version)
            } else {
              true
            }
          }
          val needRestart = (toUpdate.keySet -- errorUpdates.keySet).exists(service => Common.isUpdateService(service.name))
          if (!needRestart) {
            errorUpdates ++= toUpdate.filterKeys(!errorUpdates.contains(_)).filterNot { case (service, _) =>
              if (!Common.isUpdateService(service.name)) {
                serviceUpdaters.get(service).get.execute()
              } else {
                true
              }
            }
          }
          if (!errorUpdates.isEmpty) {
            log.error(s"Some versions are not installed: ${errorUpdates}")
            errorUpdates.foreach { case (service, version) =>
              if (Common.isUpdateService(service.name) || serviceUpdaters.get(service).get.getUpdateError().map(_.critical).getOrElse(false)) {
                log.error(s"Add service ${service} version ${version} to blacklist")
                blacklist += (service -> version)
              }
            }
          }
          needRestart
        }

        def close(): Unit = {
          log.warn("Stop running services")
          serviceUpdaters.values.foreach(_.close())
        }
      } catch {
        case e: Exception =>
          instanceState.error("Run services error", e)
          Runtime.getRuntime().halt(1)
      }
  }
}