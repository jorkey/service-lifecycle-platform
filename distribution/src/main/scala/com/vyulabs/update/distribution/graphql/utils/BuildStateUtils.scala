package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info._
import com.vyulabs.update.distribution.mongo._
import org.bson.BsonDocument

import java.io.IOException
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait BuildStateUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  protected val config: DistributionConfig

  clearBuildDeveloperStates()
  clearBuildClientStates()

  def setBuildDeveloperState(state: BuildDeveloperServiceState): Future[Unit] = {
    val filters = Filters.eq("service", state.service)
    collections.State_DeveloperBuild.update(filters, _ => Some(
      ServerBuildDeveloperServiceState(state.service, state.author, state.version, state.comment,
        state.task, state.status.toString))).map(_ => Unit)
  }

  def getDeveloperBuilds(service: Option[ServiceId]): Future[Seq[TimedBuildDeveloperServiceState]] = {
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val args = serviceArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_DeveloperBuild.findSequenced(filters).map(_.map(
      s => TimedBuildDeveloperServiceState(s.modifyTime.getOrElse(throw new IOException("No modifyTime in document")),
        s.document.service, s.document.author, s.document.version,
        s.document.comment, s.document.task, BuildStatus.withName(s.document.status))))
  }

  def getDeveloperBuildsHistory(service: Option[ServiceId], limit: Int): Future[Seq[TimedBuildDeveloperServiceState]] = {
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val args = serviceArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_DeveloperBuild.history(filters, Some(limit)).map(_.map(
      s => TimedBuildDeveloperServiceState(s.modifyTime.getOrElse(throw new IOException("No modifyTime in document")),
        s.document.service, s.document.author, s.document.version,
        s.document.comment, s.document.task, BuildStatus.withName(s.document.status))))
  }

  def setBuildClientState(state: BuildClientServiceState): Future[Unit] = {
    val filters = Filters.eq("service", state.service)
    collections.State_ClientBuild.update(filters, _ => Some(
      ServerBuildClientServiceState(state.service, state.author, state.version,
        state.task, state.status.toString))).map(_ => Unit)
  }

  def getClientBuilds(service: Option[ServiceId]): Future[Seq[TimedBuildClientServiceState]] = {
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val args = serviceArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_ClientBuild.findSequenced(filters).map(_.map(
      s => TimedBuildClientServiceState(s.modifyTime.getOrElse(throw new IOException("No modifyTime in document")),
        s.document.service, s.document.author, s.document.version, s.document.task, BuildStatus.withName(s.document.status))))
  }

  def getClientBuildsHistory(service: Option[ServiceId], limit: Int): Future[Seq[TimedBuildClientServiceState]] = {
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val args = serviceArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_ClientBuild.history(filters, Some(limit))
      .map(_.map(s => TimedBuildClientServiceState(s.modifyTime.getOrElse(throw new IOException("No modifyTime in document")),
        s.document.service, s.document.author, s.document.version, s.document.task, BuildStatus.withName(s.document.status))))
  }

  private def clearBuildDeveloperStates(): Future[Unit] = {
    val filters = Filters.eq("state", BuildStatus.InProcess)
    collections.State_DeveloperBuild.update(filters, _ match {
      case Some(state) => Some(state.copy(status = BuildStatus.Failure.toString))
      case None => None
    }).map(_ => Unit)
  }

  private def clearBuildClientStates(): Future[Unit] = {
    val filters = Filters.eq("state", BuildStatus.InProcess)
    collections.State_ClientBuild.update(filters, _ match {
      case Some(state) => Some(state.copy(status = BuildStatus.Failure.toString))
      case None => None
    }).map(_ => Unit)
  }
}
