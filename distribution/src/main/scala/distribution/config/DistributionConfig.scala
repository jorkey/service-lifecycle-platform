package distribution.config

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common.{DistributionName, InstanceId}
import com.vyulabs.update.utils.IoUtils
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol
import DefaultJsonProtocol._

case class NetworkConfig(port: Int, ssl: Option[SslConfig])

object NetworkConfig {
  implicit val networkConfigJson = jsonFormat2(NetworkConfig.apply)
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
  import com.vyulabs.update.utils.Utils.URLJson._

  implicit val developerConfigJson = jsonFormat2(UploadStateConfig.apply)
}

case class DistributionConfig(distributionName: DistributionName, title: String, instanceId: InstanceId,
                              mongoDb: String, distributionDirectory: String, network: NetworkConfig,
                              builderDirectory: Option[String], installerDirectory: String,
                              versionHistory: VersionHistoryConfig, instanceState: InstanceStateConfig,
                              faultReportsConfig: FaultReportsConfig, uploadStateConfigs: Option[Seq[UploadStateConfig]])

object DistributionConfig {
  implicit val distributionConfigJson = jsonFormat12(DistributionConfig.apply)

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