package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.ConsumerGraphqlCoder.{distributionMutations, distributionQueries}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.{DeveloperDesiredVersion, DeveloperDesiredVersionDelta, DistributionProviderInfo}
import com.vyulabs.update.common.version.DeveloperDistributionVersion
import com.vyulabs.update.distribution.client.AkkaHttpClient
import com.vyulabs.update.distribution.client.AkkaHttpClient.AkkaSource
import com.vyulabs.update.distribution.graphql.NotFoundException
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.task.TaskManager
import com.vyulabs.update.distribution.updater.AutoUpdater
import org.bson.BsonDocument
import org.slf4j.{Logger, LoggerFactory}
import spray.json.DefaultJsonProtocol._

import java.io.{File, IOException}
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait DistributionProvidersUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  protected val developerVersionUtils: DeveloperVersionUtils
  protected val clientVersionUtils: ClientVersionUtils

  protected val taskManager: TaskManager

  private implicit val log = LoggerFactory.getLogger(this.getClass)

  def addProvider(distribution: DistributionId, distributionUrl: String, accessKey: String, testDistributionMatch: Option[String],
                  uploadState: Option[Boolean], autoUpdate: Option[Boolean]): Future[Unit] = {
    collections.Client_ProvidersInfo.add(Filters.eq("distribution", distribution),
      DistributionProviderInfo(distribution, distributionUrl, accessKey, testDistributionMatch, uploadState, autoUpdate))
      .map(_ => {
        if (autoUpdate.getOrElse(false)) {
          AutoUpdater.start(distribution, developerVersionUtils, clientVersionUtils, this, taskManager)
        } else {
          AutoUpdater.stop(distribution)
        }
      })
  }

  def changeProvider(distribution: DistributionId, distributionUrl: String, accessKey: String, testDistributionMatch: Option[String],
                     uploadState: Option[Boolean], autoUpdate: Option[Boolean]): Future[Unit] = {
    collections.Client_ProvidersInfo.change(Filters.eq("distribution", distribution),
      (_) => DistributionProviderInfo(distribution, distributionUrl, accessKey, testDistributionMatch, uploadState, autoUpdate))
      .map(_ => {
        if (autoUpdate.getOrElse(false)) {
          AutoUpdater.start(distribution, developerVersionUtils, clientVersionUtils, this, taskManager)
        } else {
          AutoUpdater.stop(distribution)
        }
      })
  }

  def removeProvider(distribution: DistributionId): Future[Unit] = {
    collections.Client_ProvidersInfo.delete(Filters.eq("distribution", distribution))
      .map(_ => AutoUpdater.stop(distribution))
  }

  def getProviderDesiredVersions(distribution: DistributionId)(implicit log: Logger): Future[Seq[DeveloperDesiredVersion]] = {
    for {
      distributionProviderClient <- getDistributionProviderClient(distribution)
      desiredVersions <- distributionProviderClient.graphqlRequest(distributionQueries.getDeveloperDesiredVersions())
    } yield desiredVersions
  }

  def setProviderTestedVersions(distribution: DistributionId,
                                versions: Seq[DeveloperDesiredVersion])(implicit log: Logger): Future[Unit] = {
    for {
      distributionProviderClient <- getDistributionProviderClient(distribution)
      result <- distributionProviderClient.graphqlRequest(distributionMutations.setTestedVersions(versions)).map(_ => ())
    } yield result
  }

  def getProviderTestedVersions(distribution: DistributionId)(implicit log: Logger): Future[Seq[DeveloperDesiredVersion]] = {
    for {
      distributionProviderClient <- getDistributionProviderClient(distribution)
      desiredVersions <- distributionProviderClient.graphqlRequest(distributionQueries.getTestedVersions())
    } yield desiredVersions
  }

  def downloadProviderVersion(distributionProvider: DistributionId, service: ServiceId,
                              version: DeveloperDistributionVersion)(implicit log: Logger): Future[Unit] = {
    for {
      distributionProviderClient <- getDistributionProviderClient(distributionProvider)
      versionExists <- developerVersionUtils.getDeveloperVersionsInfo(
        Some(service), Some(version.distribution), Some(version.developerVersion)).map(!_.isEmpty)
      _ <-
        if (!versionExists) {
          log.info(s"Download developer version ${version} of service ${service}")
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
                developerVersionUtils.addDeveloperVersionInfo(versionInfo)
              case None =>
                Future()
            }
          } yield {}
        } else {
          log.info(s"Version ${version} of service ${service} already exists")
          Future()
        }
    } yield {}
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
      info => new DistributionClient(new AkkaHttpClient(info.url, Some(info.accessToken)))).getOrElse(
        throw new IOException(s"Distribution provider server ${distribution} is not defined")))
  }
}
