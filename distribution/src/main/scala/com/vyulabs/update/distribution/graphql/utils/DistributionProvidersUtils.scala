package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionName, ServiceName, TaskId}
import com.vyulabs.update.common.config.{DistributionProviderConfig, DistributionProviderInfo}
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.DistributionGraphqlCoder.distributionQueries
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.DeveloperDesiredVersion
import com.vyulabs.update.common.version.DeveloperDistributionVersion
import com.vyulabs.update.distribution.client.AkkaHttpClient
import com.vyulabs.update.distribution.client.AkkaHttpClient.AkkaSource
import com.vyulabs.update.distribution.graphql.NotFoundException
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.task.TaskManager
import org.bson.BsonDocument
import org.slf4j.{Logger, LoggerFactory}

import java.io.{File, IOException}
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait DistributionProvidersUtils extends DeveloperVersionUtils with SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  protected val taskManager: TaskManager

  implicit val log = LoggerFactory.getLogger(this.getClass)

  def getProviderDeveloperDesiredVersions(distributionProviderName: DistributionName)(implicit log: Logger): Future[Seq[DeveloperDesiredVersion]] = {
    for {
      distributionProviderClient <- getDistributionProviderClient(distributionProviderName)
      desiredVersions <- distributionProviderClient.graphqlRequest(distributionQueries.getDeveloperDesiredVersions())
    } yield desiredVersions
  }

  def installProviderDeveloperVersion(distributionProviderName: DistributionName, serviceName: ServiceName, version: DeveloperDistributionVersion)
                                     (implicit log: Logger): TaskId = {
    val task = taskManager.create(s"Download and install developer version ${version} of service ${serviceName}",
      (taskId, logger) => {
        implicit val log = logger
        val future = for {
          distributionProviderClient <- getDistributionProviderClient(distributionProviderName)
          versionExists <- getDeveloperVersionsInfo(
            serviceName, Some(version.distributionName), Some(version.version)).map(!_.isEmpty)
          _ <-
            if (!versionExists) {
              log.info(s"Download developer version ${version}")
              val imageFile = File.createTempFile("version", "image")
              for {
                _ <- distributionProviderClient.downloadDeveloperVersionImage(serviceName, version, imageFile)
                  .andThen {
                    case Success(_) =>
                      imageFile.renameTo(directory.getDeveloperVersionImageFile(serviceName, version))
                    case _ =>
                  }.andThen { case _ => imageFile.delete() }
                versionInfo <- distributionProviderClient.graphqlRequest(
                  distributionQueries.getVersionsInfo(serviceName, None, Some(version))).map(_.headOption)
                _ <- versionInfo match {
                  case Some(versionInfo) =>
                    addDeveloperVersionInfo(versionInfo)
                  case None =>
                    Future()
                }
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

  def getDistributionProvidersInfo(distributionName: Option[DistributionName] = None)(implicit log: Logger): Future[Seq[DistributionProviderInfo]] = {
    val distributionArg = distributionName.map(Filters.eq("distributionName", _))
    val args = distributionArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Distribution_ProvidersInfo.find(filters)
  }

  def getDistributionProviderConfig(distributionName: DistributionName)(implicit log: Logger): Future[DistributionProviderConfig] = {
    getDistributionProvidersInfo(Some(distributionName)).map(_.headOption.map(_.config).getOrElse(throw NotFoundException(s"No distribution provider ${distributionName} config")))
  }

  private def getDistributionProviderClient(distributionName: DistributionName): Future[DistributionClient[AkkaSource]] = {
    getDistributionProvidersInfo().map(_.find(_.distributionName == distributionName).headOption.map(
      info => new DistributionClient(new AkkaHttpClient(info.config.distributionUrl))).getOrElse(
        throw new IOException(s"Distribution provider server ${distributionName} is not defined")))
  }
}
