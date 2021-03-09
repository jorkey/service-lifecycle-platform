package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionName, InstanceId}
import com.vyulabs.update.common.utils.IoUtils
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol._
import com.vyulabs.update.common.utils.JsonFormats._

import java.io.File
import java.net.URL
import scala.concurrent.duration.FiniteDuration

case class MongoDbConfig(connection: String, name: String, temporary: Boolean)

object MongoDbConfig {
  implicit val mongoDbConfigJson = jsonFormat3(MongoDbConfig.apply)
}

case class NetworkConfig(port: Int, ssl: Option[SslConfig])

object NetworkConfig {
  implicit val networkConfigJson = jsonFormat2(NetworkConfig.apply)
}

case class RemoteBuilderConfig(distributionUrl: URL)

object RemoteBuilderConfig {
  implicit val builderConfigJson = jsonFormat1(RemoteBuilderConfig.apply)
}

case class VersionsConfig(maxHistorySize: Int)

object VersionsConfig {
  implicit val versionHistoryJson = jsonFormat1(VersionsConfig.apply)
}

case class InstanceStateConfig(expirationTimeout: FiniteDuration)

object InstanceStateConfig {
  implicit val instanceStateConfigJson = jsonFormat1(InstanceStateConfig.apply)
}

case class FaultReportsConfig(expirationTimeout: FiniteDuration, maxReportsCount: Int)

object FaultReportsConfig {
  implicit val faultReportsConfigJson = jsonFormat2(FaultReportsConfig.apply)
}

case class DistributionConfig(distributionName: DistributionName, title: String, instanceId: InstanceId,
                              mongoDb: MongoDbConfig, network: NetworkConfig, remoteBuilder: Option[RemoteBuilderConfig],
                              versions: VersionsConfig, instanceState: InstanceStateConfig, faultReports: FaultReportsConfig)

object DistributionConfig {
  implicit val distributionConfigJson = jsonFormat9((name: DistributionName, title: String, instanceId: InstanceId,
                                                     mongoDb: MongoDbConfig, network: NetworkConfig, builder: Option[RemoteBuilderConfig],
                                                     versions: VersionsConfig, instanceState: InstanceStateConfig,
                                                     faultReports: FaultReportsConfig) =>
    DistributionConfig.apply(name, title, instanceId, mongoDb, network, builder,
      versions, instanceState, faultReports))

  def readFromFile()(implicit log: Logger): Option[DistributionConfig] = {
    val configFile = new File(Common.DistributionConfigFileName)
    readFromFile(configFile)
  }

  def readFromFile(configFile: File)(implicit log: Logger): Option[DistributionConfig] = {
    if (configFile.exists()) {
      IoUtils.readFileToJson[DistributionConfig](configFile)
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}