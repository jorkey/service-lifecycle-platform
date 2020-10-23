package distribution.client.config

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.utils.IoUtils
import distribution.config.{DistributionConfig, SslConfig}
import org.slf4j.Logger
import spray.json._

case class ClientDistributionConfig(port: Int, ssl: Option[SslConfig],
                                    instanceId: InstanceId, developerDistributionUrl: URL,
                                    distributionDirectory: String, installerDirectory: String,
                                    versionsHistorySize: Int) extends DistributionConfig

object ClientDistributionConfig extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.URLJson._
  import SslConfig._

  implicit val clientDistributionConfigJson = jsonFormat7(ClientDistributionConfig.apply)

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
