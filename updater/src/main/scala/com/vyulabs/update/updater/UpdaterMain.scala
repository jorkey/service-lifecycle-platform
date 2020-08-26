package com.vyulabs.update.updater

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.com.vyulabs.common.utils.Arguments
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.info.DesiredVersions
import com.vyulabs.update.state.ProfiledServiceName
import com.vyulabs.update.updater.config.UpdaterConfig
import com.vyulabs.update.updater.uploaders.StateUploader
import com.vyulabs.update.utils.{IOUtils, Utils}
import com.vyulabs.update.version.BuildVersion
import org.slf4j.LoggerFactory

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 24.12.18.
  * Copyright FanDate, Inc.
  */
object UpdaterMain extends App { self =>
  implicit val log = LoggerFactory.getLogger(this.getClass)

  if (args.size < 1) {
    Utils.error(usage())
  }

  def usage(): String = {
    "Use: runServices <services=<service1>:[-<profile>][,...]>"
  }

  val command = args(0)
  val arguments = Arguments.parse(args.drop(1))

  val config = UpdaterConfig().getOrElse {
    Utils.error("No config")
  }

  command match {
    case "runServices" =>
      val servicesInstanceNames = arguments.getValue("services").split(",").foldLeft(Set.empty[ProfiledServiceName])((set, record) =>
        set + ProfiledServiceName.parse(record)
      )

      log.info(s"-------------------------- Start updater of version ${Utils.getManifestBuildVersion(Common.UpdaterServiceName)} for services ${servicesInstanceNames} -------------------------")

      val updaterServiceName = ProfiledServiceName(Common.UpdaterServiceName)

      val clientDirectory = new ClientDistributionDirectoryClient(config.clientDistributionUrl)

      val instanceState = new StateUploader(config.instanceId, servicesInstanceNames + updaterServiceName, clientDirectory)

      instanceState.start()

      try {
        val updaterServiceController = instanceState.getServiceStateController(updaterServiceName).get
        val selfUpdater = SelfUpdater(updaterServiceController, clientDirectory)

        val serviceUpdaters = servicesInstanceNames.foldLeft(Seq.empty[ServiceUpdater])((updaters, service) =>
          updaters :+ new ServiceUpdater(config.instanceId, service, instanceState.getServiceStateController(service).get, clientDirectory))

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
                log.info("Exit with status 9 to update")
                System.exit(9)
              }
            } else if (!servicesStarted) {
              serviceUpdaters.foreach { updater =>
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
            clientDirectory.downloadDesiredVersions() match {
              case Some(desiredVersions) =>
                val needUpdate = serviceUpdaters.foldLeft(Map.empty[ServiceUpdater, BuildVersion])((map, updater) => {
                  updater.needUpdate(desiredVersions.desiredVersions.get(updater.profiledServiceName.service)) match {
                    case Some(version) =>
                      map + (updater -> version)
                    case None =>
                      map
                  }
                })
                if (!needUpdate.isEmpty) {
                  if (update(needUpdate, desiredVersions)) {
                    return true
                  }
                }
              case None =>
                updaterServiceController.error("Can't get desired versions")
            }
            lastUpdateTime = System.currentTimeMillis()
          }
          false
        }

        def update(needUpdate: Map[ServiceUpdater, BuildVersion], desiredVersions: DesiredVersions): Boolean = {
          var errored = needUpdate.filterNot { case (updater, version) =>
            updater.beginInstall(version)
          }
          val needRestart = selfUpdater.maybeBeginSelfUpdate(desiredVersions)
          errored ++= needUpdate.filterKeys(!errored.contains(_)).filterNot { case (updater, version) =>
            updater.finishInstall(version)
          }
          if (!needRestart) {
            errored ++= needUpdate.filterKeys(!errored.contains(_)).filterNot { case (updater, _) =>
              updater.execute()
            }
          }
          if (!errored.isEmpty) {
            log.error(s"Some versions are not installed: ${errored.map{ case (updater, version) => (updater.profiledServiceName, version)} }")
          }
          needRestart
        }

        def close(): Unit = {
          log.warn("Stop running services")
          serviceUpdaters.foreach(_.close())
        }
      } catch {
        case e: Exception =>
          instanceState.error("Run services error", e)
          Runtime.getRuntime().halt(1)
      }
  }
}