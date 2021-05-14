package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId, TaskId}
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.DistributionGraphqlCoder.distributionQueries
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.{DeveloperDesiredVersionDelta, DeveloperDesiredVersion, DistributionProviderInfo}
import com.vyulabs.update.common.version.DeveloperDistributionVersion
import com.vyulabs.update.distribution.client.AkkaHttpClient
import com.vyulabs.update.distribution.client.AkkaHttpClient.AkkaSource
import com.vyulabs.update.distribution.graphql.NotFoundException
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.bson.BsonDocument
import org.slf4j.{Logger, LoggerFactory}

import java.io.{File, IOException}
import java.net.URL
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait DistributionProvidersUtils extends DeveloperVersionUtils with SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  private implicit val log = LoggerFactory.getLogger(this.getClass)

  def addProvider(distribution: DistributionId, distributionUrl: URL, uploadStateInterval: Option[FiniteDuration]): Future[Unit] = {
    collections.Client_ProvidersInfo.add(Filters.eq("distribution", distribution),
      DistributionProviderInfo(distribution, distributionUrl, uploadStateInterval)).map(_ => ())
  }

  def changeProvider(distribution: DistributionId, distributionUrl: URL, uploadStateInterval: Option[FiniteDuration]): Future[Unit] = {
    collections.Client_ProvidersInfo.change(Filters.eq("distribution", distribution),
      (_) => DistributionProviderInfo(distribution, distributionUrl, uploadStateInterval)).map(_ => ())
  }

  def removeProvider(distribution: DistributionId): Future[Unit] = {
    collections.Client_ProvidersInfo.delete(Filters.eq("distribution", distribution)).map(_ => ())
  }

  def getProviderDesiredVersions(distribution: DistributionId)(implicit log: Logger): Future[Seq[DeveloperDesiredVersion]] = {
    for {
      distributionProviderClient <- getDistributionProviderClient(distribution)
      desiredVersions <- distributionProviderClient.graphqlRequest(distributionQueries.getDeveloperDesiredVersions())
    } yield desiredVersions
  }

  def installProviderVersion(distributionProvider: DistributionId, service: ServiceId, version: DeveloperDistributionVersion)
                            (implicit log: Logger): TaskId = {
    val task = taskManager.create(s"Download and install developer version ${version} of service ${service}",
      (taskId, logger) => {
        implicit val log = logger
        val future = for {
          distributionProviderClient <- getDistributionProviderClient(distributionProvider)
          versionExists <- getDeveloperVersionsInfo(
            service, Some(version.distribution), Some(version.developerVersion)).map(!_.isEmpty)
          _ <-
            if (!versionExists) {
              log.info(s"Download provider version ${version}")
              val imageFile = File.createTempFile("version", "image")
              for {
                _ <- distributionProviderClient.downloadDeveloperVersionImage(service, version, imageFile)
                  .andThen {
                    case Success(_) =>
                      imageFile.renameTo(directory.getDeveloperVersionImageFile(service, version))
                    case _ =>
                  }.andThen { case _ => imageFile.delete() }
                versionInfo <- distributionProviderClient.graphqlRequest(
                  distributionQueries.getDeveloperVersionsInfo(service, Some(version.distribution), Some(version.developerVersion))).map(_.headOption)
                _ <- versionInfo match {
                  case Some(versionInfo) =>
                    addDeveloperVersionInfo(versionInfo)
                  case None =>
                    Future()
                }
                _ <- setDeveloperDesiredVersions(Seq(DeveloperDesiredVersionDelta(service, Some(version))))
              } yield {}
            } else {
              log.info(s"Version ${version} already exists")
              Future()
            }
        } yield {}
        (future, None)
      })
    task.taskId
  }

  def getProvidersInfo(distribution: Option[DistributionId] = None)(implicit log: Logger): Future[Seq[DistributionProviderInfo]] = {
    val distributionArg = distribution.map(Filters.eq("distribution", _))
    val args = distributionArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Client_ProvidersInfo.find(filters)
  }

  def getDistributionProviderInfo(distribution: DistributionId)(implicit log: Logger): Future[DistributionProviderInfo] = {
    getProvidersInfo(Some(distribution)).map(_.headOption.getOrElse(throw NotFoundException(s"No distribution provider ${distribution} config")))
  }

  private def getDistributionProviderClient(distribution: DistributionId): Future[DistributionClient[AkkaSource]] = {
    getProvidersInfo().map(_.find(_.distribution == distribution).headOption.map(
      info => new DistributionClient(new AkkaHttpClient(info.url))).getOrElse(
        throw new IOException(s"Distribution provider server ${distribution} is not defined")))
  }
}
