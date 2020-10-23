package com.vyulabs.update.distribution

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.vyulabs.update.common.Common
import com.vyulabs.update.distribution.developer.DeveloperDistributionWebPaths
import com.vyulabs.update.info.{DesiredVersions}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.{IoUtils, Utils}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 9.12.19.
  * Copyright FanDate, Inc.
  */
class SelfUpdater(dir: DistributionDirectory)
                 (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext, filesLocker: SmartFilesLocker)
    extends Thread with DeveloperDistributionWebPaths { self =>
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val scriptsVersion = IoUtils.readServiceVersion(Common.ScriptsServiceName, new File("."))
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
        val desiredVersions = IoUtils.readFileToJsonWithLock[DesiredVersions](dir.getDesiredVersionsFile()) match {
          case Some(versions) =>
            try {
              versions.toMap
            } catch {
              case e: Exception =>
                log.error("Can't init desired versions", e)
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
                Utils.restartToUpdate("Restart to update")
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
