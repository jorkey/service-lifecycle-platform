package distribution.client.config

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger
import spray.json._

case class ClientDistributionConfig(port: Int, instanceId: InstanceId, distributionDirectory: String, developerDistributionUrl: URL)

object ClientDistributionConfig extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.URLJson._

  implicit val clientDistributionConfigJson = jsonFormat4(ClientDistributionConfig.apply)

  def apply()(implicit log: Logger): Option[ClientDistributionConfig] = {
    val configFile = new File("distribution.json")
    if (configFile.exists()) {
      IOUtils.readFileToJson(configFile).map(_.convertTo[ClientDistributionConfig])
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}

