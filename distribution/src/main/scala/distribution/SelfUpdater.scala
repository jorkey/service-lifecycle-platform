package com.vyulabs.update.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.developer.DeveloperDistributionWebPaths
import com.vyulabs.update.info.DesiredVersions
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 9.12.19.
  * Copyright FanDate, Inc.
  */
class SelfUpdater(dir: DistributionDirectory)
                 (implicit filesLocker: SmartFilesLocker, system: ActorSystem, materializer: Materializer)
    extends Thread with DeveloperDistributionWebPaths { self =>
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val scriptsVersion = Utils.getScriptsVersion()
  private var stopping = false

  def close(): Unit = {
    self.synchronized {
      stopping = true
      notify()
    }
    join()
  }

  override def run(): Unit = {
    try {
      while (true) {
        self.synchronized {
          if (stopping) {
            return
          }
          wait(5000)
          if (stopping) {
            return
          }
        }
        val desiredVersions = Utils.parseConfigFileWithLock(dir.getDesiredVersionsFile()) match {
          case Some(config) =>
            try {
              DesiredVersions(config).Versions
            } catch {
              case e: Exception =>
                log.error("Can't init desired versions")
                return
            }
          case None =>
            log.error("Can't read desired versions")
            return
        }
        val distributionNeedUpdate = Utils.isServiceNeedUpdate(Common.DistributionServiceName,
          Utils.getManifestBuildVersion(Common.DistributionServiceName),
          desiredVersions.get(Common.DistributionServiceName)).isDefined
        val scriptsNeedUpdate = Utils.isServiceNeedUpdate(Common.ScriptsServiceName,
          scriptsVersion,
          desiredVersions.get(Common.ScriptsServiceName)).isDefined
        if (distributionNeedUpdate || scriptsNeedUpdate) {
          log.info("Shutdown HTTP server to update")
          Http().shutdownAllConnectionPools() andThen {
            case _ =>
              log.info("Terminate to update")
              system.terminate() andThen {
                sys.exit(9)
              }
          }
        }
      }
    } catch {
      case ex: Exception =>
        log.error(s"Self updater thread is failed", ex)
    }
  }
}
