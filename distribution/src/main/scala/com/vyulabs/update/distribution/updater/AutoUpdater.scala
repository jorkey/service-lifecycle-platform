package com.vyulabs.update.distribution.updater

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.directives.FutureDirectives
import akka.stream.Materializer
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.DistributionId
import com.vyulabs.update.common.info.DeveloperDesiredVersion
import com.vyulabs.update.distribution.graphql.utils.{ClientVersionUtils, DeveloperVersionUtils, DistributionProvidersUtils}
import com.vyulabs.update.distribution.task.TaskManager
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 11.11.21.
  * Copyright FanDate, Inc.
  */

class AutoUpdater(distribution: DistributionId,
                  developerVersionUtils: DeveloperVersionUtils,
                  clientVersionUtils: ClientVersionUtils,
                  distributionProvidersUtils: DistributionProvidersUtils,
                  taskManager: TaskManager)
                 (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext)  extends FutureDirectives with SprayJsonSupport { self =>
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  var updateTask = Option.empty[Cancellable]
  val autoUpdateInterval = FiniteDuration.apply(10, TimeUnit.SECONDS)

  def start(): Unit = {
    schedule()
  }

  def stop(): Unit = {
    synchronized {
      for (task <- updateTask) {
        if (task.cancel() || !task.isCancelled) {
          log.debug("Auto updater is cancelled")
        } else {
          log.debug("Auto updater failed to cancel")
        }
        this.updateTask = None
      }
    }
  }

  private def schedule(): Unit = {
    synchronized {
      val reschedule = updateTask.isDefined
      updateTask = Some(system.scheduler.scheduleOnce(autoUpdateInterval)(autoUpdate()))
      log.debug(s"Auto updater is ${if (reschedule) "rescheduled again" else "scheduled"}")
    }
  }

  private def autoUpdate(): Unit = {
    log.debug("Started auto update")
    val result = for {
      developerVersions <- developerVersionUtils.getDeveloperVersionsInfo()
        .map(_.map(v => DeveloperDesiredVersion(v.service, v.version)).filter(_.version.distribution == distribution))
      providerVersions <- distributionProvidersUtils.getProviderDesiredVersions(distribution)
      taskId <- {
        val versionsToUpdate = providerVersions.filter(!developerVersions.contains(_))
        log.debug(s"Versions to update ${versionsToUpdate}")
        if (!versionsToUpdate.isEmpty) {
          clientVersionUtils.buildClientVersions(versionsToUpdate, Common.AuthorDistribution).map(Some(_))
        } else {
          Future(None)
        }
      }
    } yield {
      taskId.map(taskManager.getTask(_)).flatten
    }
    result.andThen { case result =>
      synchronized {
        if (result.isSuccess) {
          result.get match {
            case Some(task) =>
              task.future.andThen { case result =>
                if (result.isSuccess) {
                  log.debug(s"Auto update is successfully finished")
                } else {
                  log.error(s"Auto update is failed: ${result.failed.get}")
                }
                schedule()
              }
            case None =>
              schedule()
          }
        } else {
          log.error(s"Auto update error", result.failed.get)
          schedule()
        }
      }
    }
  }
}

object AutoUpdater {
  var autoUpdaters = Map.empty[DistributionId, AutoUpdater]

  def start(distribution: DistributionId,
            developerVersionUtils: DeveloperVersionUtils,
            clientVersionUtils: ClientVersionUtils,
            distributionProvidersUtils: DistributionProvidersUtils,
            taskManager: TaskManager)
           (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext) = {
    synchronized {
      val updater = new AutoUpdater(distribution,
        developerVersionUtils, clientVersionUtils, distributionProvidersUtils, taskManager)
      updater.start()
      autoUpdaters += distribution -> updater
    }
  }

  def stop(distribution: DistributionId): Unit = {
    synchronized {
      autoUpdaters.get(distribution).foreach { updater =>
        updater.stop()
        autoUpdaters -= distribution
      }
    }
  }
}