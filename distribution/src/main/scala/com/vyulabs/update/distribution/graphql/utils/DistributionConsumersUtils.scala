package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{ConsumerProfile, DistributionId}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.DistributionConsumerInfo
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.bson.BsonDocument
import org.slf4j.Logger

import java.io.IOException
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait DistributionConsumersUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  def addDistributionConsumer(distribution: DistributionId, consumerProfile: ConsumerProfile, testDistributionMatch: Option[String]): Future[Unit] = {
    collections.Developer_ConsumersInfo.update(Filters.eq("distribution", distribution),
      _ => Some(DistributionConsumerInfo(distribution, consumerProfile, testDistributionMatch))).map(_ => ())
  }

  def removeDistributionConsumer(distribution: DistributionId): Future[Unit] = {
    collections.Developer_ConsumersInfo.delete(Filters.eq("distribution", distribution)).map(_ => ())
  }

  def getDistributionConsumersInfo(distribution: Option[DistributionId] = None)(implicit log: Logger): Future[Seq[DistributionConsumerInfo]] = {
    val distributionArg = distribution.map(Filters.eq("distribution", _))
    val args = distributionArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Developer_ConsumersInfo.find(filters)
  }

  def getDistributionConsumerInfo(distribution: DistributionId)(implicit log: Logger): Future[DistributionConsumerInfo] = {
    getDistributionConsumersInfo(Some(distribution))
      .map(_.headOption.getOrElse(throw new IOException(s"No distribution ${distribution} consumer info")))
  }
}
