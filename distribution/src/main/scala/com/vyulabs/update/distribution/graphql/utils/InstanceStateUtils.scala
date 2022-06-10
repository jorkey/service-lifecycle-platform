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
import org.slf4j.Logger

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait InstanceStateUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  protected val config: DistributionConfig

  def setSelfInstanceStates(states: Seq[DirectoryServiceState])(implicit log: Logger): Future[Unit] = {
    setInstanceStates(config.distribution, states.map(state => InstanceState(config.instance, state.service, state.directory, state.state)))
  }

  def setInstanceStates(distribution: DistributionId, instanceStates: Seq[InstanceState])(implicit log: Logger): Future[Unit] = {
    val documents = instanceStates.foldLeft(Seq.empty[DistributionInstanceState])((seq, state) => seq :+
      DistributionInstanceState(distribution = distribution, instance = state.instance, service = state.service, directory = state.directory, state.state))
    Future.sequence(documents.map(doc => {
      val filters = Filters.and(
        Filters.eq("distribution", distribution),
        Filters.eq("service", doc.service),
        Filters.eq("instance", doc.instance),
        Filters.eq("directory", doc.directory))
      collections.State_Instances.update(filters, _ => Some(doc))
    })).map(_ => ())
  }

  def getInstancesState(distribution: Option[DistributionId], service: Option[ServiceId],
                        instance: Option[InstanceId], directory: Option[ServiceDirectory])(implicit log: Logger): Future[Seq[DistributionInstanceState]] = {
    val distributionArg = distribution.map { distribution => Filters.eq("distribution", distribution) }
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val instanceArg = instance.map { instance => Filters.eq("instance", instance) }
    val directoryArg = directory.map { directory => Filters.eq("directory", directory) }
    val args = distributionArg ++ serviceArg ++ instanceArg ++ directoryArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_Instances.find(filters)
  }

  def getInstanceStates(distribution: Option[DistributionId], service: Option[ServiceId],
                        instance: Option[InstanceId], directory: Option[ServiceDirectory])(implicit log: Logger): Future[Seq[InstanceState]] = {
    getInstancesState(distribution, service, instance, directory).map(_.map(s =>
      InstanceState(instance = s.instance, service = s.service, directory = s.directory, state = s.state)))
  }
}
