package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}
import com.vyulabs.update.common.config.{BuildServiceConfig, DistributionConfig, NamedStringValue, Repository}
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.bson.BsonDocument
import org.slf4j.Logger

import java.io.IOException
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters.asJavaIterableConverter

trait ConfigBuilderUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val config: DistributionConfig
  protected val collections: DatabaseCollections

  def setBuildDeveloperServiceConfig(service: Option[ServiceId],
                                     distribution: Option[DistributionId],
                                     environment: Seq[NamedStringValue],
                                     sourceRepositories: Seq[Repository], macroValues: Seq[NamedStringValue])
                                    (implicit log: Logger): Future[Boolean] = {
    log.info(s"Set developer service ${service} config")
    val filters = Filters.eq("service", service)
    collections.Developer_BuildServices.update(filters, _ =>
      Some(BuildServiceConfig(service, distribution, environment, sourceRepositories, macroValues))).map(_ > 0)
  }

  def removeBuildDeveloperServiceConfig(service: ServiceId)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove developer service ${service} config")
    val filters = Filters.eq("service", service)
    collections.Developer_BuildServices.delete(filters).map(_ > 0)
  }

  def getDeveloperServicesConfig(service: Option[ServiceId])
                                (implicit log: Logger): Future[Seq[BuildServiceConfig]] = {
    val args = service.map(Filters.eq("service", _)).toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Developer_BuildServices.find(filters)
  }

  def getDeveloperServiceConfig(service: ServiceId)
                               (implicit log: Logger): Future[BuildServiceConfig] = {
    getDeveloperServicesConfig(Some(service)).map(_.headOption.getOrElse {
      throw new IOException(s"No developer service ${service} config")
    })
  }

  def setBuildClientServiceConfig(service: Option[ServiceId],
                                  distribution: Option[DistributionId],
                                  environment: Seq[NamedStringValue],
                                  settings: Seq[Repository], macroValues: Seq[NamedStringValue])
                                  (implicit log: Logger): Future[Boolean] = {
    log.info(s"Set client service ${service} config")
    val filters = Filters.eq("service", service)
    collections.Client_BuildServices.update(filters, _ =>
      Some(BuildServiceConfig(service, distribution, environment, settings, macroValues))).map(_ > 0)
  }

  def removeBuildClientServiceConfig(service: ServiceId)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove client service ${service} config")
    val filters = Filters.eq("service", service)
    collections.Client_BuildServices.delete(filters).map(_ > 0)
  }

  def getClientServiceConfig(service: ServiceId)
                             (implicit log: Logger): Future[Option[BuildServiceConfig]] = {
    getClientServicesConfig(Some(service)).map(_.headOption)
  }

  def getClientServicesConfig(service: Option[ServiceId])
                              (implicit log: Logger): Future[Seq[BuildServiceConfig]] = {
    val args = service.map(Filters.eq("service", _)).toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Client_BuildServices.find(filters)
  }
}
