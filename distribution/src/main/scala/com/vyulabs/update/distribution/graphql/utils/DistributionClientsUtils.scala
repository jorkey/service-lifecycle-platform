package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionName, ProfileName}
import com.vyulabs.update.common.config.{DistributionClientConfig, DistributionClientInfo, DistributionClientProfile}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.distribution.graphql.NotFoundException
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.bson.BsonDocument
import org.slf4j.Logger

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait DistributionClientsUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  def getDistributionClientsInfo(distributionName: Option[DistributionName] = None)(implicit log: Logger): Future[Seq[DistributionClientInfo]] = {
    val clientArg = distributionName.map(Filters.eq("distributionName", _))
    val args = clientArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Developer_DistributionClientsInfo.find(filters)
  }

  def getDistributionClientConfig(distributionName: DistributionName)(implicit log: Logger): Future[DistributionClientConfig] = {
    getDistributionClientsInfo(Some(distributionName)).map(_.headOption.map(_.clientConfig).getOrElse(throw NotFoundException(s"No distribution client ${distributionName} config")))
  }

  def getDistributionClientInstallProfile(distributionName: DistributionName)(implicit log: Logger): Future[DistributionClientProfile] = {
    for {
      clientConfig <- getDistributionClientConfig(distributionName)
      installProfile <- getInstallProfile(clientConfig.installProfile)
    } yield installProfile
  }

  def getInstallProfile(profileName: ProfileName)(implicit log: Logger): Future[DistributionClientProfile] = {
    val profileArg = Filters.eq("profileName", profileName)
    collections.Developer_DistributionClientsProfiles.find(profileArg).map(_.headOption
      .getOrElse(throw NotFoundException(s"No install profile ${profileName}")))
  }
}
