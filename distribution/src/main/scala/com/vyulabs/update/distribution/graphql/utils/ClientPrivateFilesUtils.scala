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

trait ClientPrivateFilesUtils extends SprayJsonSupport {

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val config: DistributionConfig

  protected implicit val executionContext: ExecutionContext

  def addClientPrivateFile(service: ServiceId, file: String)(implicit log: Logger): Future[Unit] = {
    log.info(s"Add client private file ${file} for service ${service}")
    collections.Client_PrivateFiles.insert(PrivateFile(service, file)).map(_ => Unit)
  }

  def getClientPrivateFiles(service: ServiceId)(implicit log: Logger): Future[Seq[PrivateFile]] = {
    val filters = Filters.and(Filters.eq("service", service))
    collections.Client_PrivateFiles.find(filters)
  }

  def removeClientPrivateFiles(service: ServiceId)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove client private files of service ${service}")
    IoUtils.deleteDirectoryContents(directory.getClientPrivateDir(config.distribution, service))
    val filters = Filters.and(Filters.eq("service", service))
    collections.Client_PrivateFiles.delete(filters).map(_ > 0)
  }

  def removeClientPrivateFile(service: ServiceId, file: String)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove client private file ${file} of service ${service}")
    val filters = Filters.and(
      Filters.eq("service", service),
      Filters.eq("file", file))
    directory.getClientPrivateFile(config.distribution, service, file).delete()
    collections.Client_PrivateFiles.delete(filters).map(_ > 0)
  }
}