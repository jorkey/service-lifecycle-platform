package distribution.developer.config

import java.io.File

import com.vyulabs.update.common.Common.{ClientName, InstanceId}
import com.vyulabs.update.utils.IOUtils
import distribution.config.HttpsConfig
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol

case class DeveloperDistributionConfig(name: String, instanceId: InstanceId,
                                       port: Option[Int], https: Option[HttpsConfig],
                                       distributionDirectory: String,
                                       selfDistributionClient: Option[ClientName],
                                       builderDirectory: String)

object DeveloperDistributionConfig extends DefaultJsonProtocol {
  import HttpsConfig._

  implicit val developerDistributionConfigJson = jsonFormat7(DeveloperDistributionConfig.apply)

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