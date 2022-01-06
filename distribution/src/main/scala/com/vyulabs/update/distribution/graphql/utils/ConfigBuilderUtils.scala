package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}
import com.vyulabs.update.common.config.{BuilderConfig, ServiceConfig, DistributionConfig, NamedStringValue, Repository}
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

  def setDeveloperBuilderConfig(distribution: DistributionId)
                               (implicit log: Logger): Future[Boolean] = {
    log.info(s"Set developer builder config")
    collections.Developer_Builder.update(new BsonDocument(),
      _ => Some(BuilderConfig(distribution))).map(_ > 0)
  }

  def setBuildDeveloperServiceConfig(service: ServiceId,
                                distribution: Option[DistributionId],
                                environment: Seq[NamedStringValue],
                                sourceRepositories: Seq[Repository], macroValues: Seq[NamedStringValue])
                               (implicit log: Logger): Future[Boolean] = {
    log.info(s"Set developer service ${service} config")
    val filters = Filters.eq("service", service)
    collections.Developer_Services.update(filters, _ =>
      Some(ServiceConfig(service, distribution, environment, sourceRepositories, macroValues))).map(_ > 0)
  }

  def removeBuildDeveloperServiceConfig(service: ServiceId)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove developer service ${service} config")
    val filters = Filters.eq("service", service)
    collections.Developer_Services.delete(filters).map(_ > 0)
  }

  def getDeveloperBuilderConfig()(implicit log: Logger): Future[BuilderConfig] = {
    collections.Developer_Builder.find().map(_.headOption.getOrElse {
      throw new IOException("Developer builder config is not defined")
    })
  }

  def getDeveloperServicesConfig(service: Option[ServiceId])
                                (implicit log: Logger): Future[Seq[ServiceConfig]] = {
    val args = service.map(Filters.eq("service", _)).toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Developer_Services.find(filters)
  }

  def getDeveloperServiceConfig(service: ServiceId)
                               (implicit log: Logger): Future[ServiceConfig] = {
    getDeveloperServicesConfig(Some(service)).map(_.headOption.getOrElse {
      throw new IOException(s"No developer service ${service} config")
    })
  }

  def setClientBuilderConfig(distribution: DistributionId)
                            (implicit log: Logger): Future[Boolean] = {
    log.info(s"Set client builder config")
    collections.Client_Builder.update(new BsonDocument(),
      _ => Some(BuilderConfig(distribution))).map(_ > 0)
  }

  def setBuildClientServiceConfig(service: ServiceId,
                             distribution: Option[DistributionId],
                             environment: Seq[NamedStringValue],
                             settings: Seq[Repository], macroValues: Seq[NamedStringValue])
                            (implicit log: Logger): Future[Boolean] = {
    log.info(s"Set client service ${service} config")
    val filters = Filters.eq("service", service)
    collections.Client_Services.update(filters, _ =>
      Some(ServiceConfig(service, distribution, environment, settings, macroValues))).map(_ > 0)
  }

  def removeBuildClientServiceConfig(service: ServiceId)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove client service ${service} config")
    val filters = Filters.eq("service", service)
    collections.Client_Services.delete(filters).map(_ > 0)
  }

  def getClientBuilderConfig()(implicit log: Logger): Future[BuilderConfig] = {
    collections.Client_Builder.find().map(_.headOption.getOrElse {
      throw new IOException("Client builder config is not defined")
    })
  }

  def getClientServiceConfig(service: ServiceId)
                             (implicit log: Logger): Future[Option[ServiceConfig]] = {
    getClientServicesConfig(Some(service)).map(_.headOption)
  }

  def getClientServicesConfig(service: Option[ServiceId])
                              (implicit log: Logger): Future[Seq[ServiceConfig]] = {
    val args = service.map(Filters.eq("service", _)).toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Client_Services.find(filters)
  }
}
