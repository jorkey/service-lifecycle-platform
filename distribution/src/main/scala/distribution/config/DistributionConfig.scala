package distribution.config

import com.vyulabs.update.common.Common.InstanceId
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

trait DistributionConfig {
  val title: String
  val instanceId: InstanceId
  val mongoDb: String
  val distributionDirectory: String
  val network: NetworkConfig
  val versionHistory: VersionHistoryConfig
  val instanceState: InstanceStateConfig
}
