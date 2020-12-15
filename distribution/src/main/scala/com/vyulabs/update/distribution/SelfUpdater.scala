package com.vyulabs.update.distribution

import java.io.{File, IOException}
import java.util.concurrent.TimeUnit
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.common.info.{ClientDesiredVersions, DeveloperDesiredVersions}
import com.vyulabs.update.common.utils.{IoUtils, Utils}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 9.12.19.
  * Copyright FanDate, Inc.
  */
class SelfUpdater(collections: DatabaseCollections)
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
      distributionNeedUpdate <- Future(Utils.isServiceNeedUpdate(Common.DistributionServiceName,
        distributionVersion, ClientDesiredVersions.toMap(desiredVersions.versions).get(Common.DistributionServiceName)).isDefined)
      scriptsNeedUpdate <- Future(Utils.isServiceNeedUpdate(Common.ScriptsServiceName,
        scriptsVersion, ClientDesiredVersions.toMap(desiredVersions.versions).get(Common.ScriptsServiceName)).isDefined)
    } yield {
      if (distributionNeedUpdate || scriptsNeedUpdate) {
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
}
