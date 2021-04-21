package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{ConsumerProfile, ServiceId}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.DistributionConsumerProfile
import com.vyulabs.update.distribution.graphql.NotFoundException
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.bson.BsonDocument

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters.asJavaIterableConverter

trait DistributionConsumerProfilesUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  def addDistributionConsumerProfile(consumerProfile: ConsumerProfile, services: Seq[ServiceId]): Future[Unit] = {
    collections.Developer_Profiles.update(Filters.eq("consumerProfile", consumerProfile),
      _ => Some(DistributionConsumerProfile(consumerProfile, services))).map(_ => ())
  }

  def getDistributionConsumerProfiles(consumerProfile: Option[ConsumerProfile]): Future[Seq[DistributionConsumerProfile]] = {
    val profileArg = consumerProfile.map(Filters.eq("consumerProfile", _))
    val args = profileArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Developer_Profiles.find(filters)
  }

  def getDistributionConsumerProfile(consumerProfile: ConsumerProfile): Future[DistributionConsumerProfile] = {
    val filters = Filters.eq("consumerProfile", consumerProfile)
    collections.Developer_Profiles.find(filters)map(_.headOption.getOrElse {
      throw NotFoundException(s"No consumer profile ${consumerProfile}")
    })
  }

  def removeDistributionConsumerProfile(consumerProfile: ConsumerProfile): Future[Unit] = {
    collections.Developer_Profiles.delete(Filters.eq("consumerProfile", consumerProfile)).map(_ => ())
  }
}
