package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.BuildTarget.BuildTarget
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

  clearBuildStates()

  def setBuildState(state: BuildServiceState): Future[Unit] = {
    val serviceArg = Filters.eq("service", state.service)
    val targetsArg = Filters.or(state.targets.map(target => Filters.eq("targets", target.toString)).asJava)
    val filters = Filters.and(serviceArg, targetsArg)
    collections.State_Builds.update(filters, _ => Some(
      ServerBuildServiceState(state.service, state.targets.map(_.toString),
        state.author, state.version, state.comment, state.task, state.status.toString))).map(_ => Unit)
  }

  def getBuildStates(service: Option[ServiceId], targets: Option[Seq[BuildTarget]]): Future[Seq[TimedBuildServiceState]] = {
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val targetsArg = targets.map { targets => Filters.or(targets.map(target => Filters.eq("targets", target.toString)).asJava) }
    val args = serviceArg.toSeq ++ targetsArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_Builds.findSequenced(filters).map(_.map(
      s => TimedBuildServiceState(s.modifyTime.getOrElse(throw new IOException("No modifyTime in document")),
        s.document.service, s.document.targets.map(BuildTarget.withName(_)), s.document.author, s.document.version,
        s.document.comment, s.document.task, BuildStatus.withName(s.document.status))))
  }

  def getBuildStatesHistory(service: Option[ServiceId], targets: Option[Seq[BuildTarget]], limit: Int)
      : Future[Seq[TimedBuildServiceState]] = {
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val targetsArg = targets.map { targets => Filters.or(targets.map(target => Filters.eq("targets", target.toString)).asJava) }
    val args = serviceArg.toSeq ++ targetsArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_Builds.history(filters, Some(limit)).map(_.map(
      s => TimedBuildServiceState(s.modifyTime.getOrElse(throw new IOException("No modifyTime in document")),
        s.document.service, s.document.targets.map(BuildTarget.withName(_)), s.document.author, s.document.version,
        s.document.comment, s.document.task, BuildStatus.withName(s.document.status))))
  }

  private def clearBuildStates(): Future[Unit] = {
    val filters = Filters.eq("state", BuildStatus.InProcess)
    collections.State_Builds.update(filters, _ match {
      case Some(state) => Some(state.copy(status = BuildStatus.Failure.toString))
      case None => None
    }).map(_ => Unit)
  }
}
