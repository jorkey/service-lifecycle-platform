package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{ConsumerProfileName, ServiceName}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.DistributionConsumerProfile
import com.vyulabs.update.distribution.mongo.DatabaseCollections

import scala.concurrent.{ExecutionContext, Future}

trait ConsumerProfilesUtils extends DeveloperVersionUtils with SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  def addDistributionConsumerProfile(profileName: ConsumerProfileName, services: Set[ServiceName]): Future[Unit] = {
    collections.Distribution_ConsumerProfiles.update(Filters.eq("profileName", profileName),
      _ => Some(DistributionConsumerProfile(profileName, services))).map(_ => ())
  }

  def removeDistributionConsumerProfile(profileName: ConsumerProfileName): Future[Unit] = {
    collections.Distribution_ConsumerProfiles.delete(Filters.eq("profileName", profileName)).map(_ => ())
  }
}
