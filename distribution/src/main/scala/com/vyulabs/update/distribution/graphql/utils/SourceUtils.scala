package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{ServiceId}
import com.vyulabs.update.common.config.{DistributionConfig, ServiceSourcesConfig, SourceConfig}
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

trait SourceUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val config: DistributionConfig
  protected val collections: DatabaseCollections

  def getDeveloperServices()(implicit log: Logger): Future[Seq[ServiceId]] = {
    collections.Sources.find().map(_.map(_.service))
  }

  def getServiceSources(service: ServiceId)(implicit log: Logger): Future[Seq[SourceConfig]] = {
    val filters = Filters.eq("service", service)
    collections.Sources.find(filters).map(_.headOption.map(_.payload).getOrElse(Seq.empty))
  }

  def addServiceSources(service: ServiceId, sources: Seq[SourceConfig])
                       (implicit log: Logger): Future[Unit] = {
    log.info(s"Add sources for service ${service}")
    for {
      result <- {
        val document = ServiceSourcesConfig(service, sources)
        collections.Sources.insert(document).map(_ => ())
      }
    } yield result
  }

  def removeServiceSources(service: ServiceId)
                          (implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove sources of service ${service}")
    val filters = Filters.eq("service", service)
    collections.Sources.delete(filters).map(_ > 0)
  }

  def changeSources(service: ServiceId, sources: Seq[SourceConfig])
                   (implicit log: Logger): Future[Boolean] = {
    log.info(s"Change sources of service ${service}")
    val filters = Filters.eq("service", service)
    collections.Sources.change(filters, _ => ServiceSourcesConfig(service, sources)).map(_ > 0)
  }
}
