package distribution.config

import com.vyulabs.update.common.Common.InstanceId

trait DistributionConfig {
  val port: Int
  val ssl: Option[SslConfig]
  val instanceId: InstanceId
  val distributionDirectory: String
  val versionsHistorySize: Int
}
