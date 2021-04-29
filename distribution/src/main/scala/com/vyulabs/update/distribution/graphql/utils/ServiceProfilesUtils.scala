package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{ServicesProfileId, ServiceId}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.ServicesProfile
import com.vyulabs.update.distribution.graphql.NotFoundException
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.bson.BsonDocument

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters.asJavaIterableConverter

trait ServiceProfilesUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  def addServicesProfile(servicesProfileId: ServicesProfileId, services: Seq[ServiceId]): Future[Unit] = {
    collections.Developer_ServiceProfiles.update(Filters.eq("profile", servicesProfileId),
      _ => Some(ServicesProfile(servicesProfileId, services))).map(_ => ())
  }

  def changeServicesProfile(servicesProfileId: ServicesProfileId, services: Seq[ServiceId]): Future[Unit] = {
    collections.Developer_ServiceProfiles.update(Filters.eq("profile", servicesProfileId),
      _ => Some(ServicesProfile(servicesProfileId, services))).map(_ => ())
  }

  def getServiceProfiles(servicesProfileId: Option[ServicesProfileId]): Future[Seq[ServicesProfile]] = {
    val profileArg = servicesProfileId.map(Filters.eq("profile", _))
    val args = profileArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Developer_ServiceProfiles.find(filters)
  }

  def getServicesProfile(servicesProfileId: ServicesProfileId): Future[ServicesProfile] = {
    val filters = Filters.eq("profile", servicesProfileId)
    collections.Developer_ServiceProfiles.find(filters)map(_.headOption.getOrElse {
      throw NotFoundException(s"No consumer profile ${servicesProfileId}")
    })
  }

  def removeServicesProfile(servicesProfileId: ServicesProfileId): Future[Unit] = {
    collections.Developer_ServiceProfiles.delete(Filters.eq("profile", servicesProfileId)).map(_ => ())
  }
}
