package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.BuildState.BuildState
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import com.vyulabs.update.distribution.mongo._
import org.bson.BsonDocument
import org.slf4j.Logger

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait BuildStateUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  protected val config: DistributionConfig

  def setBuildDeveloperState(author: AccountId, service: ServiceId,
                             version: DeveloperDistributionVersion, comment: String,
                             taskId: TaskId, state: BuildState)
                            (implicit log: Logger): Future[Unit] = {
    val filters = Filters.eq("service", service)
    collections.State_DeveloperBuild.update(filters,
      _ => Some(BuildDeveloperServiceState(service, author, version, comment, taskId, state))).map(_ => Unit)
  }

  def getBuildDeveloperStates(service: Option[ServiceId])
                             (implicit log: Logger): Future[Seq[BuildDeveloperServiceState]] = {
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val args = serviceArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_DeveloperBuild.find(filters)
  }

  def getBuildDeveloperHistory(service: Option[ServiceId], limit: Int)
                               (implicit log: Logger): Future[Seq[TimedBuildDeveloperServiceState]] = {
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val args = serviceArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_DeveloperBuild.history(filters, Some(limit))
      .map(_.map(s => TimedBuildDeveloperServiceState(s.time, s.document.service, s.document.author,
        s.document.version, s.document.taskId, s.document.state)))
  }

  def setBuildClientState(author: AccountId, service: ServiceId, version: ClientDistributionVersion,
                          taskId: TaskId, state: BuildState)
                         (implicit log: Logger): Future[Unit] = {
    val filters = Filters.eq("service", service)
    collections.State_ClientBuild.update(filters,
      _ => Some(BuildClientServiceState(service, author, version, taskId, state))).map(_ => Unit)
  }

  def getBuildClientStates(service: Option[ServiceId])
                          (implicit log: Logger): Future[Seq[BuildClientServiceState]] = {
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val args = serviceArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_ClientBuild.find(filters)
  }

  def getBuildClientHistory(service: Option[ServiceId], limit: Int)
                           (implicit log: Logger): Future[Seq[TimedBuildClientServiceState]] = {
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val args = serviceArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_ClientBuild.history(filters, Some(limit))
      .map(_.map(s => TimedBuildClientServiceState(s.time, s.document.service, s.document.author,
        s.document.version, s.document.taskId, s.document.state)))
  }
}
