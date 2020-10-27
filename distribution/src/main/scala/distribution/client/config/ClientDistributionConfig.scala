package distribution.client.config

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.utils.IoUtils
import distribution.config.{DistributionConfig, InstanceStateConfig, NetworkConfig, SslConfig, VersionHistoryConfig}
import org.slf4j.Logger
import spray.json._

case class ClientDistributionConfig(title: String,
                                    instanceId: InstanceId,
                                    mongoDb: String,
                                    distributionDirectory: String,
                                    network: NetworkConfig,
                                    versionHistory: VersionHistoryConfig,
                                    instanceState: InstanceStateConfig,
                                    developerDistributionUrl: URL,
                                    installerDirectory: String) extends DistributionConfig

object ClientDistributionConfig extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.URLJson._
  implicit val clientDistributionConfigJson = jsonFormat9(ClientDistributionConfig.apply)

  def apply()(implicit log: Logger): Option[ClientDistributionConfig] = {
    val configFile = new File("distribution.json")
    if (configFile.exists()) {
      IoUtils.readFileToJson[ClientDistributionConfig](configFile)
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}
