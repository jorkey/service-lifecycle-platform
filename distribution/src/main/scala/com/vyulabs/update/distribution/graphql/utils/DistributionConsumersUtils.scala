package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionName, ProfileName}
import com.vyulabs.update.common.config.{DistributionConsumerConfig, DistributionConsumerInfo, DistributionConsumerProfile}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.distribution.graphql.NotFoundException
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.bson.BsonDocument
import org.slf4j.Logger

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait DistributionConsumersUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  def getDistributionConsumersInfo(distributionName: Option[DistributionName] = None)(implicit log: Logger): Future[Seq[DistributionConsumerInfo]] = {
    val distributionArg = distributionName.map(Filters.eq("distributionName", _))
    val args = distributionArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Distribution_ConsumersInfo.find(filters)
  }

  def getDistributionConsumerConfig(distributionName: DistributionName)(implicit log: Logger): Future[DistributionConsumerConfig] = {
    getDistributionConsumersInfo(Some(distributionName)).map(_.headOption.map(_.config).getOrElse(throw NotFoundException(s"No distribution consumer ${distributionName} config")))
  }

  def getDistributionConsumerInstallProfile(distributionName: DistributionName)(implicit log: Logger): Future[DistributionConsumerProfile] = {
    for {
      consumerConfig <- getDistributionConsumerConfig(distributionName)
      installProfile <- getInstallProfile(consumerConfig.installProfile)
    } yield installProfile
  }

  def getInstallProfile(profileName: ProfileName)(implicit log: Logger): Future[DistributionConsumerProfile] = {
    val profileArg = Filters.eq("profileName", profileName)
    collections.Distribution_ConsumersProfiles.find(profileArg).map(_.headOption
      .getOrElse(throw NotFoundException(s"No install profile ${profileName}")))
  }
}
