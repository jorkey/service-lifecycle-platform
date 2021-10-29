package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.{Materializer}
import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info._
import com.vyulabs.update.distribution.mongo._
import org.bson.BsonDocument
import org.slf4j.Logger

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait StateUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  protected val config: DistributionConfig

  def setSelfServiceStates(states: Seq[DirectoryServiceState])(implicit log: Logger): Future[Unit] = {
    setServiceStates(config.distribution, states.map(state => InstanceServiceState(config.instance, state.service, state.directory, state.state)))
  }

  def setServiceStates(distribution: DistributionId, instanceStates: Seq[InstanceServiceState])(implicit log: Logger): Future[Unit] = {
    val documents = instanceStates.foldLeft(Seq.empty[DistributionServiceState])((seq, state) => seq :+ DistributionServiceState(distribution, state))
    Future.sequence(documents.map(doc => {
      val filters = Filters.and(
        Filters.eq("distribution", distribution),
        Filters.eq("payload.service", doc.payload.service),
        Filters.eq("payload.instance", doc.payload.instance),
        Filters.eq("payload.directory", doc.payload.directory))
      collections.State_ServiceStates.update(filters, _ => Some(doc))
    })).map(_ => ())
  }

  def getServicesState(distribution: Option[DistributionId], service: Option[ServiceId],
                       instance: Option[InstanceId], directory: Option[ServiceDirectory])(implicit log: Logger): Future[Seq[DistributionServiceState]] = {
    val distributionArg = distribution.map { distribution => Filters.eq("distribution", distribution) }
    val serviceArg = service.map { service => Filters.eq("payload.service", service) }
    val instanceArg = instance.map { instance => Filters.eq("payload.instance", instance) }
    val directoryArg = directory.map { directory => Filters.eq("payload.directory", directory) }
    val args = distributionArg ++ serviceArg ++ instanceArg ++ directoryArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_ServiceStates.find(filters)
  }

  def getInstanceServiceStates(distribution: Option[DistributionId], service: Option[ServiceId],
                               instance: Option[InstanceId], directory: Option[ServiceDirectory])(implicit log: Logger): Future[Seq[InstanceServiceState]] = {
    getServicesState(distribution, service, instance, directory).map(_.map(_.payload))
  }
}
