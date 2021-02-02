package com.vyulabs.update.distribution.config

import java.io.File
import java.net.URL
import com.vyulabs.update.common.common.Common.{DistributionName, InstanceId}
import com.vyulabs.update.common.utils.IoUtils
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol
import DefaultJsonProtocol._
import com.vyulabs.update.common.utils.Utils.URLJson.URLJsonFormat

case class NetworkConfig(port: Int, ssl: Option[SslConfig])

object NetworkConfig {
  implicit val networkConfigJson = jsonFormat2(NetworkConfig.apply)
}

case class BuilderConfig(builderDirectory: Option[String], distributionUrl: Option[URL])

object BuilderConfig {
  implicit val builderConfigJson = jsonFormat2(BuilderConfig.apply)
}

case class VersionHistoryConfig(maxSize: Int)

object VersionHistoryConfig {
  implicit val versionHistoryJson = jsonFormat1(VersionHistoryConfig.apply)
}

case class InstanceStateConfig(expireSec: Int)

object InstanceStateConfig {
  implicit val instanceStateConfigJson = jsonFormat1(InstanceStateConfig.apply)
}

case class FaultReportsConfig(expirationPeriodMs: Long, maxFaultReportsCount: Int)

object FaultReportsConfig {
  implicit val faultReportsConfigJson = jsonFormat2(FaultReportsConfig.apply)
}

case class UploadStateConfig(distributionUrl: URL, uploadStateIntervalSec: Int)

object UploadStateConfig {
  import com.vyulabs.update.common.utils.Utils.URLJson._

  implicit val developerConfigJson = jsonFormat2(UploadStateConfig.apply)
}

case class DistributionConfig(distributionName: DistributionName, title: String, instanceId: InstanceId,
                              mongoDbConnection: String, mongoDbName: String, network: NetworkConfig,
                              distributionDirectory: String, builderConfig: BuilderConfig,
                              versionHistory: VersionHistoryConfig, instanceState: InstanceStateConfig, faultReportsConfig: FaultReportsConfig,
                              uploadStateConfigs: Option[Seq[UploadStateConfig]])

object DistributionConfig {
  implicit val distributionConfigJson = jsonFormat12((distributionName: DistributionName, title: String, instanceId: InstanceId,
                                                      mongoDbConnection: String, mongoDbName: String, distributionDirectory: String, builderConfig: BuilderConfig,
                                                      network: NetworkConfig, versionHistory: VersionHistoryConfig, instanceState: InstanceStateConfig,
                                                      faultReportsConfig: FaultReportsConfig, uploadStateConfigs: Option[Seq[UploadStateConfig]]) =>
    DistributionConfig.apply(distributionName, title, instanceId, mongoDbConnection, mongoDbName, network, distributionDirectory, builderConfig,
      versionHistory, instanceState, faultReportsConfig, uploadStateConfigs))

  def readFromFile()(implicit log: Logger): Option[DistributionConfig] = {
    val configFile = new File("distribution.json")
    if (configFile.exists()) {
      IoUtils.readFileToJson[DistributionConfig](configFile)
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}