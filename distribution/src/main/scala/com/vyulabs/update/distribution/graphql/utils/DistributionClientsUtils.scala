package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionName, ProfileName}
import com.vyulabs.update.common.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.distribution.graphql.NotFoundException
import com.vyulabs.update.distribution.mongo.{DatabaseCollections, DistributionClientProfileDocument}
import org.bson.BsonDocument
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait DistributionClientsUtils extends SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections

  def getDistributionClientsInfo(distributionName: Option[DistributionName] = None): Future[Seq[DistributionClientInfo]] = {
    val clientArg = distributionName.map(Filters.eq("distributionName", _))
    val args = clientArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    for {
      collection <- collections.Developer_DistributionClientsInfo
      info <- collection.find(filters).map(_.map(_.content))
    } yield info
  }

  def getDistributionClientConfig(distributionName: DistributionName): Future[DistributionClientConfig] = {
    getDistributionClientsInfo(Some(distributionName)).map(_.headOption.map(_.clientConfig).getOrElse(throw NotFoundException(s"No distribution client ${distributionName} config")))
  }

  def getDistributionClientInstallProfile(distributionName: DistributionName): Future[DistributionClientProfileDocument] = {
    for {
      clientConfig <- getDistributionClientConfig(distributionName)
      installProfile <- getInstallProfile(clientConfig.installProfile)
    } yield installProfile
  }

  def getInstallProfile(profileName: ProfileName): Future[DistributionClientProfileDocument] = {
    val profileArg = Filters.eq("profileName", profileName)
    for {
      collection <- collections.Developer_DistributionClientsProfiles
      profile <- collection.find(profileArg).map(_.headOption
        .getOrElse(throw NotFoundException(s"No install profile ${profileName}")))
    } yield profile
  }
}
