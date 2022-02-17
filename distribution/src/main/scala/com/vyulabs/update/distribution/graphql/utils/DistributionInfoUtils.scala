package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.info.DistributionInfo
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.ClientDistributionVersion
import org.slf4j.Logger

import java.io.File
import scala.concurrent.ExecutionContext

trait DistributionInfoUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val config: DistributionConfig

  def getDistributionInfo()(implicit log: Logger): DistributionInfo = {
    DistributionInfo(config.distribution, config.title,
      IoUtils.readServiceVersion(Common.DistributionServiceName, new File("."))
        .getOrElse(ClientDistributionVersion(config.distribution, Seq(0), 0)))
  }
}
