package distribution.client.config

import java.io.File
import java.net.URL

import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger

case class ClientDistributionConfig(port: Int, developerDistributionUrl: URL)

object ClientDistributionConfig {
  def apply()(implicit log: Logger): Option[ClientDistributionConfig] = {
    val configFile = new File("distribution.json")
    if (configFile.exists()) {
      val config = IOUtils.parseConfigFile(configFile).getOrElse{ return None }
      val port = config.getInt("port")
      val developerDistributionUrl = new URL(config.getString("developerDistributionUrl"))
      Some(ClientDistributionConfig(port, developerDistributionUrl))
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}


