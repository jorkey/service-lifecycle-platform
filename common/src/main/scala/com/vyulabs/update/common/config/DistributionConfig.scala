package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionId, InstanceId}
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.utils.JsonFormats._
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol._

import java.io.File
import scala.concurrent.duration.FiniteDuration

case class MongoDbConfig(connection: String, name: String, temporary: Option[Boolean])

object MongoDbConfig {
  implicit val mongoDbConfigJson = jsonFormat3(MongoDbConfig.apply)
}

case class NetworkConfig(host: String, port: Int, ssl: Option[SslConfig])

object NetworkConfig {
  implicit val networkConfigJson = jsonFormat3(NetworkConfig.apply)
}

case class VersionsConfig(maxHistorySize: Int)

object VersionsConfig {
  implicit val versionsConfigJson = jsonFormat1(VersionsConfig.apply)
}

case class ServiceStatesConfig(expirationTimeout: FiniteDuration)

object ServiceStatesConfig {
  implicit val instanceStateConfigJson = jsonFormat1(ServiceStatesConfig.apply)
}

case class LogsConfig(serviceLogExpirationTimeout: FiniteDuration, taskLogExpirationTimeout: FiniteDuration)

object LogsConfig {
  implicit val logsConfigJson = jsonFormat2(LogsConfig.apply)
}

case class FaultReportsConfig(expirationTimeout: FiniteDuration, maxReportsCount: Int)

object FaultReportsConfig {
  implicit val faultReportsConfigJson = jsonFormat2(FaultReportsConfig.apply)
}

case class DistributionConfig(distribution: DistributionId, title: String, instance: InstanceId, jwtSecret: String,
                              mongoDb: MongoDbConfig, network: NetworkConfig,
                              versions: VersionsConfig, serviceStates: ServiceStatesConfig,
                              logs: LogsConfig, faultReports: FaultReportsConfig)

object DistributionConfig {
  implicit val distributionConfigJson = jsonFormat10((distribution: DistributionId, title: String, instance: InstanceId, jwtSecret: String,
                                                      mongoDb: MongoDbConfig, network: NetworkConfig,
                                                      versions: VersionsConfig, instanceState: ServiceStatesConfig,
                                                      logs: LogsConfig, faultReports: FaultReportsConfig) =>
    DistributionConfig.apply(distribution, title, instance, jwtSecret, mongoDb, network,
      versions, instanceState, logs, faultReports))

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