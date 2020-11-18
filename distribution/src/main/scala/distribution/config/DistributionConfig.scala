package distribution.config

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common.{InstanceId}
import com.vyulabs.update.utils.IoUtils
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol

case class NetworkConfig(port: Int, ssl: Option[SslConfig])

object NetworkConfig extends DefaultJsonProtocol {
  implicit val networkConfigJson = jsonFormat2(NetworkConfig.apply)
}

case class VersionHistoryConfig(maxSize: Int)

object VersionHistoryConfig extends DefaultJsonProtocol {
  implicit val versionHistoryJson = jsonFormat1(VersionHistoryConfig.apply)
}

case class InstanceStateConfig(expireSec: Int)

object InstanceStateConfig extends DefaultJsonProtocol {
  implicit val instanceStateConfigJson = jsonFormat1(InstanceStateConfig.apply)
}

case class DeveloperConfig(builderDirectory: String)

object DeveloperConfig extends DefaultJsonProtocol {
  implicit val developerConfigJson = jsonFormat1(DeveloperConfig.apply)
}

case class ClientConfig(developerDistributionUrl: URL, installerDirectory: String, uploadStateIntervalSec: Int)

object ClientConfig extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.URLJson._

  implicit val developerConfigJson = jsonFormat3(ClientConfig.apply)
}

case class DistributionConfig(title: String,
                              instanceId: InstanceId,
                              mongoDb: String,
                              distributionDirectory: String,
                              network: NetworkConfig,
                              versionHistory: VersionHistoryConfig,
                              instanceState: InstanceStateConfig,
                              developer: Option[DeveloperConfig],
                              client: Option[ClientConfig])

object DistributionConfig extends DefaultJsonProtocol {
  implicit val distributionConfigJson = jsonFormat9(DistributionConfig.apply)

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