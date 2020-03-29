package com.vyulabs.update.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.vyulabs.update.common.Common
import com.vyulabs.update.distribution.developer.DeveloperDistributionWebPaths
import com.vyulabs.update.info.DesiredVersions
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.UpdateUtils
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
        for (ownVersion <- UpdateUtils.getManifestBuildVersion(Common.DistributionServiceName)) {
          UpdateUtils.parseConfigFileWithLock(dir.getDesiredVersionsFile()) match {
            case Some(config) =>
              val versions = try {
                DesiredVersions(config).Versions
              } catch {
                case e: Exception =>
                  log.error("Can't init desired versions")
                  return
              }
              for (desiredVersion <- versions.get(Common.DistributionServiceName)) {
                if (ownVersion != desiredVersion) {
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
            case None =>
              log.error(s"Can't parse ${dir.getDesiredVersionsFile()}")
          }
        }
      }
    } catch {
      case ex: Exception =>
        log.error(s"Self updater thread is failed", ex)
    }
  }
}
