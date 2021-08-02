package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId, TaskId}
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.DistributionGraphqlCoder.distributionQueries
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.{ClientDesiredVersion, DeveloperDesiredVersion, DeveloperDesiredVersionDelta, DistributionProviderInfo}
import com.vyulabs.update.common.version.DeveloperDistributionVersion
import com.vyulabs.update.distribution.client.AkkaHttpClient
import com.vyulabs.update.distribution.client.AkkaHttpClient.AkkaSource
import com.vyulabs.update.distribution.graphql.NotFoundException
import com.vyulabs.update.distribution.mongo.{DatabaseCollections, InstalledDesiredVersions}
import org.bson.BsonDocument
import org.slf4j.{Logger, LoggerFactory}

import java.io.{File, IOException}
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait DistributionConsumersUtils extends DeveloperVersionUtils with SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  private implicit val log = LoggerFactory.getLogger(this.getClass)

  def setConsumerInstalledDesiredVersions(distribution: DistributionId, desiredVersions: Seq[ClientDesiredVersion])(implicit log: Logger): Future[Unit] = {
    val clientArg = Filters.eq("distribution", distribution)
    collections.Consumers_InstalledDesiredVersions.update(clientArg, _ => Some(InstalledDesiredVersions(distribution, desiredVersions))).map(_ => ())
  }

  def getConsumerInstalledDesiredVersions(distribution: DistributionId, services: Set[ServiceId] = Set.empty)(implicit log: Logger): Future[Seq[ClientDesiredVersion]] = {
    val clientArg = Filters.eq("distribution", distribution)
    collections.Consumers_InstalledDesiredVersions.find(clientArg).map(_.headOption.map(_.versions).getOrElse(Seq.empty[ClientDesiredVersion]))
      .map(_.filter(v => services.isEmpty || services.contains(v.service)).sortBy(_.service))
  }

  def getConsumerInstalledDesiredVersion(distribution: DistributionId, service: ServiceId)(implicit log: Logger): Future[Option[ClientDesiredVersion]] = {
    getConsumerInstalledDesiredVersions(distribution, Set(service)).map(_.headOption)
  }
}
