package com.vyulabs.update.distribution.updater

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.directives.FutureDirectives
import akka.stream.Materializer
import com.mongodb.client.model.{Filters, Sorts, Updates}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.DistributionId
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.{GraphqlArgument, GraphqlMutation}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.distribution.client.AkkaHttpClient.AkkaSource
import com.vyulabs.update.distribution.graphql.utils.{ClientVersionUtils, DeveloperVersionUtils, DistributionProvidersUtils}
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 11.11.21.
  * Copyright FanDate, Inc.
  */

class AutoUpdater(distribution: DistributionId,
                  developerVersionUtils: DeveloperVersionUtils,
                  clientVersionUtils: ClientVersionUtils,
                  distributionProvidersUtils: DistributionProvidersUtils)
                 (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext)  extends FutureDirectives with SprayJsonSupport { self =>
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  var task = Option.empty[Cancellable]

  def start(): Unit = {
    synchronized {
      task = Some(system.scheduler.scheduleOnce(FiniteDuration.apply(10, TimeUnit.SECONDS))(autoUpdate()))
      log.debug("Auto updater is scheduled")
    }
  }

  def stop(): Unit = {
    synchronized {
      for (task <- task) {
        if (task.cancel() || !task.isCancelled) {
          log.debug("Auto updater is cancelled")
        } else {
          log.debug("Auto updater failed to cancel")
        }
        this.task = None
      }
    }
  }

  private def autoUpdate(): Unit = {
    log.debug("Auto update")
    val result = for {
      developerVersions <- developerVersionUtils.getDeveloperDesiredVersions()
      providerVersions <- distributionProvidersUtils.getProviderDesiredVersions(distribution)
    } yield {
      val versionsToUpdate = providerVersions.filter(!developerVersions.contains(_))
      clientVersionUtils.buildClientVersions(versionsToUpdate, Common.AuthorDistribution)
    }

  }
}

