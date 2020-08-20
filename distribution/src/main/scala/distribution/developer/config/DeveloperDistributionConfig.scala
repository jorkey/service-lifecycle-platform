package distribution.developer.config

import java.io.File

import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger

import spray.json.DefaultJsonProtocol

case class DeveloperDistributionConfig(name: String, port: Int)

object DeveloperDistributionConfig extends DefaultJsonProtocol {
  implicit val developerDistributionConfigJson = jsonFormat2(DeveloperDistributionConfig.apply)

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