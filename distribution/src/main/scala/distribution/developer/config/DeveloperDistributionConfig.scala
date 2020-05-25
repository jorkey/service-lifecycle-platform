package distribution.developer.config

import java.io.File

import com.vyulabs.update.utils.Utils
import org.slf4j.Logger

case class DeveloperDistributionConfig(port: Int)

object DeveloperDistributionConfig {
  def apply()(implicit log: Logger): Option[DeveloperDistributionConfig] = {
    val configFile = new File("distribution.json")
    if (configFile.exists()) {
      val config = Utils.parseConfigFile(configFile).getOrElse{ return None }
      val port = config.getInt("port")
      Some(DeveloperDistributionConfig(port))
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}
