package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionName, ProfileName}
import com.vyulabs.update.common.config.{DistributionConsumerInfo, DistributionConsumerProfile}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.distribution.graphql.NotFoundException
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

  def addDistributionConsumer(distributionName: DistributionName, installProfile: ProfileName, testDistributionMatch: Option[String]): Future[Unit] = {
    collections.Distribution_ConsumersInfo.update(Filters.eq("distributionName", distributionName),
      _ => Some(DistributionConsumerInfo(distributionName, installProfile, testDistributionMatch))).map(_ => ())
  }

  def getDistributionConsumersInfo(distributionName: Option[DistributionName] = None)(implicit log: Logger): Future[Seq[DistributionConsumerInfo]] = {
    val distributionArg = distributionName.map(Filters.eq("distributionName", _))
    val args = distributionArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Distribution_ConsumersInfo.find(filters)
  }

  def getDistributionConsumerInfo(distributionName: DistributionName)(implicit log: Logger): Future[DistributionConsumerInfo] = {
    getDistributionConsumersInfo(Some(distributionName))
      .map(_.headOption.getOrElse(throw new IOException(s"No distribution ${distributionName} consumer info")))
  }

  def getDistributionConsumerInstallProfile(distributionName: DistributionName)(implicit log: Logger): Future[DistributionConsumerProfile] = {
    for {
      consumerConfig <- getDistributionConsumerInfo(distributionName)
      installProfile <- getInstallProfile(consumerConfig.installProfile)
    } yield installProfile
  }

  def getInstallProfile(profileName: ProfileName)(implicit log: Logger): Future[DistributionConsumerProfile] = {
    val profileArg = Filters.eq("profileName", profileName)
    collections.Distribution_ConsumersProfiles.find(profileArg).map(_.headOption
      .getOrElse(throw NotFoundException(s"No install profile ${profileName}")))
  }
}
