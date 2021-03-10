package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{ConsumerProfileName, ServiceName}
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

  def addDistributionConsumerProfile(profileName: ConsumerProfileName, services: Seq[ServiceName]): Future[Unit] = {
    collections.Distribution_ConsumerProfiles.update(Filters.eq("profileName", profileName),
      _ => Some(DistributionConsumerProfile(profileName, services))).map(_ => ())
  }

  def getDistributionConsumerProfiles(profileName: Option[ConsumerProfileName]): Future[Seq[DistributionConsumerProfile]] = {
    val profileArg = profileName.map(Filters.eq("profileName", _))
    val args = profileArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Distribution_ConsumerProfiles.find(filters)
  }

  def getDistributionConsumerProfile(profileName: ConsumerProfileName): Future[DistributionConsumerProfile] = {
    val filters = Filters.eq("profileName", profileName)
    collections.Distribution_ConsumerProfiles.find(filters)map(_.headOption.getOrElse {
      throw NotFoundException(s"No install profile ${profileName}")
    })
  }

  def removeDistributionConsumerProfile(profileName: ConsumerProfileName): Future[Unit] = {
    collections.Distribution_ConsumerProfiles.delete(Filters.eq("profileName", profileName)).map(_ => ())
  }
}
