package distribution.developer.config

import java.io.File

import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger

import spray.json.DefaultJsonProtocol

case class DeveloperDistributionConfig(port: Int)

object DeveloperDistributionConfigJson extends DefaultJsonProtocol {
  implicit val developerDistributionConfigJson = jsonFormat1(DeveloperDistributionConfig.apply)
}

object DeveloperDistributionConfig {
  import DeveloperDistributionConfigJson._

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
