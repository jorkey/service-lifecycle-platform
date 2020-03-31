package com.vyulabs.update.updater

import java.net.URL

import com.vyulabs.update.common.{Common, ServiceInstanceName}
import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.common.com.vyulabs.common.utils.Arguments
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.info.DesiredVersions
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.LoggerFactory

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 24.12.18.
  * Copyright FanDate, Inc.
  */
object UpdaterMain extends App { self =>
  implicit val log = LoggerFactory.getLogger(this.getClass)

  if (args.size < 1) {
    sys.error(usage())
  }

  def usage(): String = {
    "Use: runServices <clientDirectoryUrl=value> <instanceId=value> <services=<service1>:[-<profile>][,...]>"
  }

  val command = args(0)
  val arguments = Arguments.parse(args.drop(1))

  command match {
    case "runServices" =>
      val clientDirectoryUrl = new URL(arguments.getValue("clientDirectoryUrl"))
      val instanceId: InstanceId = arguments.getValue("instanceId")
      val servicesInstanceNames = arguments.getValue("services").split(",").foldLeft(Set.empty[ServiceInstanceName])((set, record) =>
        set + ServiceInstanceName.parse(record)
      )

      log.info(s"-------------------------- Start updater of version ${Utils.getManifestBuildVersion(Common.UpdaterServiceName)} for services ${servicesInstanceNames} -------------------------")

      val currentVersion = Utils.getManifestBuildVersion("updater").getOrElse {
        val version = BuildVersion(None, Seq(0, 0, 0))
        log.error(s"Can't get current updater version - assume ${version}")
        version
      }

      val updaterServiceName = ServiceInstanceName(Common.UpdaterServiceName)

      val clientDirectory = new ClientDistributionDirectoryClient(clientDirectoryUrl)

      val instanceState = new InstanceStateUploader(instanceId, currentVersion, servicesInstanceNames + updaterServiceName, clientDirectory)

      instanceState.start()

      try {
        val updaterServiceController = instanceState.getServiceStateController(updaterServiceName).get
        val selfUpdater = SelfUpdater(updaterServiceController, clientDirectory)

        val serviceUpdaters = servicesInstanceNames.foldLeft(Seq.empty[ServiceUpdater])((updaters, service) =>
          updaters :+ new ServiceUpdater(instanceId,
            service, instanceState.getServiceStateController(service).get, clientDirectory))

        var lastUpdateTime = 0L

        var stopping = false
        var stopped = false

        Runtime.getRuntime.addShutdownHook(new Thread() {
          override def run(): Unit = {
            log.info("Shutdown hook")
            self.synchronized {
              stopping = true
              self.notify()
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

        while (true) {
          val mustStop = self.synchronized {
            if (servicesStarted) {
              self.wait(1000)
            }
            stopping
          }
          if (mustStop) {
            log.info("Updater is terminating")
            close()
            log.info("All are terminated")
            self.synchronized {
              stopped = true
              self.notify()
            }
            Runtime.getRuntime.halt(0)
          } else {
            maybeUpdate()
            if (!servicesStarted) {
              serviceUpdaters.foreach { updater =>
                if (!updater.isExecuted())
                  updater.execute()
              }
              servicesStarted = true
            }
          }
        }

        def maybeUpdate(): Unit = {
          if (System.currentTimeMillis() - lastUpdateTime > 10000) {
            clientDirectory.downloadDesiredVersions() match {
              case Some(desiredVersions) =>
                val needUpdate = serviceUpdaters.foldLeft(Map.empty[ServiceUpdater, BuildVersion])((map, updater) => {
                  updater.needUpdate(desiredVersions.Versions.get(updater.serviceInstanceName.serviceName)) match {
                    case Some(version) =>
                      map + (updater -> version)
                    case None =>
                      map
                  }
                })
                if (!needUpdate.isEmpty) {
                  update(needUpdate, desiredVersions)
                }
              case None =>
                updaterServiceController.error("Can't get desired versions")
            }
            lastUpdateTime = System.currentTimeMillis()
          }
        }

        def update(needUpdate: Map[ServiceUpdater, BuildVersion], desiredVersions: DesiredVersions): Unit = {
          needUpdate.foreach {
            case (updater, version) =>
              updater.update(version)
          }
          selfUpdater.needUpdate(desiredVersions.Versions.get(Common.UpdaterServiceName)) match {
            case Some(newUpdaterVersion) =>
              selfUpdater.update(newUpdaterVersion)
              new Thread() {
                override def run(): Unit = {
                  log.info("Exit with status 9 to update")
                  System.exit(9)
                }
              }.start()
            case None =>
              needUpdate.keySet.foreach(_.execute())
          }
        }

        def close(): Unit = {
          log.warn("Stop running services")
          serviceUpdaters.foreach(_.close())
        }
      } catch {
        case e: Exception =>
          instanceState.error("Run services error", e)
          System.exit(1)
      }
  }
}