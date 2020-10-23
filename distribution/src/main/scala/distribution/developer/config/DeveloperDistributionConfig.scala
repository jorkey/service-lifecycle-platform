package distribution.developer.config

import java.io.File

import com.vyulabs.update.common.Common.{ClientName, InstanceId}
import com.vyulabs.update.utils.IoUtils
import distribution.config.{DistributionConfig, SslConfig}
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol

case class DeveloperDistributionConfig(name: String, instanceId: InstanceId,
                                       port: Int, ssl: Option[SslConfig],
                                       distributionDirectory: String,
                                       selfDistributionClient: Option[ClientName],
                                       builderDirectory: String,
                                       versionsHistorySize: Int) extends DistributionConfig

object DeveloperDistributionConfig extends DefaultJsonProtocol {
  import SslConfig._

  implicit val developerDistributionConfigJson = jsonFormat8(DeveloperDistributionConfig.apply)

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