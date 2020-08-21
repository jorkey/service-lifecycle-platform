package distribution.developer.config

import java.io.File

import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol

case class DeveloperDistributionConfig(name: String, instanceId: InstanceId, port: Int)

object DeveloperDistributionConfig extends DefaultJsonProtocol {
  implicit val developerDistributionConfigJson = jsonFormat3(DeveloperDistributionConfig.apply)

  def apply()(implicit log: Logger): Option[DeveloperDistributionConfig] = {
    val configFile = new File("distribution.json")
    if (configFile.exists()) {
      IOUtils.readFileToJson(configFile).map(_.convertTo[DeveloperDistributionConfig])
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}