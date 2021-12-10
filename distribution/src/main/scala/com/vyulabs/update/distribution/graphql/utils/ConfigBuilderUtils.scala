package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.vyulabs.update.common.config.{ClientBuilderConfig, DeveloperBuilderConfig, DistributionConfig}
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.bson.BsonDocument
import org.slf4j.Logger

import java.io.IOException
import scala.concurrent.{ExecutionContext, Future}

trait ConfigBuilderUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val config: DistributionConfig
  protected val collections: DatabaseCollections

  def setDeveloperBuilderConfig(config: DeveloperBuilderConfig)
                               (implicit log: Logger): Future[Boolean] = {
    log.info(s"Set developer build config")
    collections.Developer_Builder.update(new BsonDocument(), _ => Some(config)).map(_ > 0)
  }

  def getDeveloperBuilderConfig()(implicit log: Logger): Future[DeveloperBuilderConfig] = {
    collections.Developer_Builder.find().map(_.headOption.getOrElse(
      throw new IOException("No developer builder config")))
  }

  def setClientBuilderConfig(config: ClientBuilderConfig)
                            (implicit log: Logger): Future[Boolean] = {
    log.info(s"Set client build config")
    collections.Client_Builder.update(new BsonDocument(), _ => Some(config)).map(_ > 0)
  }

  def getClientBuilderConfig()(implicit log: Logger): Future[ClientBuilderConfig] = {
    collections.Client_Builder.find().map(_.headOption.getOrElse(
      throw new IOException("No client builder config")))
  }
}
