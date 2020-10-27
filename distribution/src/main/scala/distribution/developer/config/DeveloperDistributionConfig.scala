package distribution.developer.config

import java.io.File

import com.vyulabs.update.common.Common.{ClientName, InstanceId}
import com.vyulabs.update.utils.IoUtils
import distribution.config.{DistributionConfig, InstanceStateConfig, NetworkConfig, SslConfig, VersionHistoryConfig}
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol

case class DeveloperDistributionConfig(title: String,
                                       instanceId: InstanceId,
                                       mongoDb: String,
                                       distributionDirectory: String,
                                       network: NetworkConfig,
                                       versionHistory: VersionHistoryConfig,
                                       instanceState: InstanceStateConfig,
                                       selfDistributionClient: Option[ClientName],
                                       builderDirectory: String) extends DistributionConfig

object DeveloperDistributionConfig extends DefaultJsonProtocol {
  implicit val developerDistributionConfigJson = jsonFormat9(DeveloperDistributionConfig.apply)

  def apply()(implicit log: Logger): Option[DeveloperDistributionConfig] = {
    val configFile = new File("distribution.json")
    if (configFile.exists()) {
      IoUtils.readFileToJson[DeveloperDistributionConfig](configFile)
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}