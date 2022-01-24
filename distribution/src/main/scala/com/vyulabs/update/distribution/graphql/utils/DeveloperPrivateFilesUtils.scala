package com.vyulabs.update.distribution.graphql.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

trait DeveloperPrivateFilesUtils extends SprayJsonSupport {

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val config: DistributionConfig

  protected implicit val executionContext: ExecutionContext

  def addDeveloperPrivateFile(service: ServiceId, file: String)(implicit log: Logger): Future[Unit] = {
    log.info(s"Add developer private file ${file} for service ${service}")
    collections.Developer_PrivateFiles.insert(PrivateFile(service, file)).map(_ => Unit)
  }

  def getDeveloperPrivateFiles(service: ServiceId)(implicit log: Logger): Future[Seq[PrivateFile]] = {
    val filters = Filters.and(Filters.eq("service", service))
    collections.Developer_PrivateFiles.find(filters)
  }

  def removeDeveloperPrivateFiles(service: ServiceId)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove developer private files of service ${service}")
    IoUtils.deleteDirectoryContents(directory.getDeveloperPrivateDir(config.distribution, service))
    val filters = Filters.and(Filters.eq("service", service))
    collections.Developer_PrivateFiles.delete(filters).map(_ > 0)
  }

  def removeDeveloperPrivateFile(service: ServiceId, file: String)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove developer private file ${file} of service ${service}")
    val filters = Filters.and(
      Filters.eq("service", service),
      Filters.eq("file", file))
    directory.getDeveloperPrivateFile(config.distribution, service, file).delete()
    collections.Developer_PrivateFiles.delete(filters).map(_ > 0)
  }
}