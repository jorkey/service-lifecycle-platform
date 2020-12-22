package com.vyulabs.update.distribution

import java.io.{File, IOException}
import java.util.concurrent.TimeUnit
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{CommonServiceProfile, ServiceName}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.common.info.{ClientDesiredVersions, DeveloperDesiredVersions}
import com.vyulabs.update.common.utils.{IoUtils, Utils}
import com.vyulabs.update.common.version.ClientDistributionVersion
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 9.12.19.
  * Copyright FanDate, Inc.
  */
class SelfUpdater(collections: DatabaseCollections, directory: DistributionDirectory)
                 (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext) { self =>
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val checkUpdateTimeout = FiniteDuration(5, TimeUnit.SECONDS)

  private val scriptsVersion = IoUtils.readServiceVersion(Common.ScriptsServiceName, new File("."))
  private val distributionVersion = IoUtils.readServiceVersion(Common.DistributionServiceName, new File("."))

  def start(): Unit = {
    system.scheduler.scheduleOnce(FiniteDuration(1, TimeUnit.SECONDS))(() => maybeUpdate())
  }

  def maybeUpdate(): Unit = {
    for {
      collection <- collections.Client_DesiredVersions
      desiredVersions <- collection.find().map(_.headOption.getOrElse(throw new IOException("Can't find desired versions")))
      distributionNewVersion <- Future(Utils.isServiceNeedUpdate(Common.DistributionServiceName,
        distributionVersion, ClientDesiredVersions.toMap(desiredVersions.versions).get(Common.DistributionServiceName)))
      scriptsNewVersion <- Future(Utils.isServiceNeedUpdate(Common.ScriptsServiceName,
        scriptsVersion, ClientDesiredVersions.toMap(desiredVersions.versions).get(Common.ScriptsServiceName)))
    } yield {
      val servicesToUpdate = distributionNewVersion.map((Common.DistributionServiceName, _)) ++ scriptsNewVersion.map((Common.ScriptsServiceName, _))
      if (!servicesToUpdate.isEmpty) {
        servicesToUpdate.foreach {
          case (serviceName, version) =>
            log.error(s"Begin service ${serviceName} update to version ${version}")
            if (!beginServiceUpdate(serviceName, version)) {
              log.error(s"Can't begin service ${serviceName} update")
              return
            }
        }
        log.info("Shutdown HTTP server to update")
        Http().shutdownAllConnectionPools() andThen {
            case _ =>
              log.info("Terminate to update")
              system.terminate() andThen {
                Utils.restartToUpdate("Restart to update")
              }
          }
      } else {
        system.scheduler.scheduleOnce(checkUpdateTimeout)(() => maybeUpdate())
      }
    }
  }

  private def beginServiceUpdate(serviceName: ServiceName, toVersion: ClientDistributionVersion): Boolean = {
    log.info(s"Downloading ${serviceName} of version ${toVersion}")
    if (!IoUtils.copyFile(directory.getClientVersionImageFile(serviceName, toVersion), new File(Common.ServiceZipName.format(serviceName)))) {
      log.error(s"Downloading ${serviceName} error")
      return false
    }
    if (!IoUtils.writeDesiredServiceVersion(new File("."), serviceName, toVersion)) {
      log.error(s"Set ${serviceName} desired version error")
      return false
    }
    if (!IoUtils.writeServiceVersion(new File("."), serviceName, toVersion)) {
      log.error(s"Set ${serviceName} version error")
      return false
    }
    true
  }
}