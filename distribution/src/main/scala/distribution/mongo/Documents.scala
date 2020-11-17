package distribution.mongo

import com.vyulabs.update.common.Common.ClientName
import com.vyulabs.update.config.{ClientInfo, ClientProfile}
import com.vyulabs.update.info.{ClientDesiredVersion, ClientFaultReport, ClientServiceLogLine, ClientServiceState, ClientVersionInfo, DeveloperDesiredVersion, DeveloperVersionInfo, TestedDesiredVersions}

case class SequenceDocument(name: String, sequence: Long)

case class ClientInfoDocument(info: ClientInfo)
case class ClientProfileDocument(profile: ClientProfile)

case class DeveloperVersionInfoDocument(_id: Long, info: DeveloperVersionInfo)
case class ClientVersionInfoDocument(_id: Long, info: ClientVersionInfo)

case class DeveloperDesiredVersionsDocument(versions: Seq[DeveloperDesiredVersion], _id: Long = 0)
case class ClientDesiredVersionsDocument(versions: Seq[ClientDesiredVersion], _id: Long = 0)

case class InstalledDesiredVersionsDocument(clientName: ClientName, versions: Seq[ClientDesiredVersion])
case class TestedDesiredVersionsDocument(versions: TestedDesiredVersions)

case class ServiceStateDocument(sequence: Long, state: ClientServiceState)
case class ServiceLogLineDocument(_id: Long, log: ClientServiceLogLine)
case class FaultReportDocument(_id: Long, fault: ClientFaultReport)

case class UploadStatus(lastUploadSequence: Option[Long], lastError: Option[String])
case class UploadStatusDocument(component: String, status: UploadStatus)
