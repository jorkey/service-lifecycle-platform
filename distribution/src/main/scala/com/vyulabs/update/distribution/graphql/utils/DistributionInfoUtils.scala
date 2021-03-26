package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.info.DistributionInfo

import scala.concurrent.ExecutionContext

trait DistributionInfoUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val config: DistributionConfig

  def getDistributionInfo(): DistributionInfo = {
    DistributionInfo(config.distributionName, config.title)
  }
}
