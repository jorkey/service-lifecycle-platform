package distribution.mongo

import com.vyulabs.update.common.Common.{ClientName}
import com.vyulabs.update.config.{ClientInfo, ClientProfile}
import com.vyulabs.update.info.{ClientFaultReport, ClientServiceLogLine, ClientServiceState, DesiredVersion, DeveloperVersionInfo, InstalledVersionInfo, TestedDesiredVersions}

case class ClientInfoDocument(info: ClientInfo)
case class ClientProfileDocument(profile: ClientProfile)

case class DeveloperVersionInfoDocument(_id: Long, info: DeveloperVersionInfo)
case class InstalledVersionInfoDocument(_id: Long, info: InstalledVersionInfo)

case class DesiredVersionsDocument(versions: Seq[DesiredVersion], _id: Long = 0)
case class PersonalDesiredVersionsDocument(clientName: ClientName, versions: Seq[DesiredVersion])
case class InstalledDesiredVersionsDocument(clientName: ClientName, versions: Seq[DesiredVersion])
case class TestedDesiredVersionsDocument(versions: TestedDesiredVersions)

case class ServiceStateDocument(_id: Long, state: ClientServiceState)
case class ServiceLogLineDocument(_id: Long, log: ClientServiceLogLine)
case class FaultReportDocument(_id: Long, report: ClientFaultReport)

case class UploadStatus(lastUploadSequence: Long, lastError: Option[String])
case class UploadStatusDocument(component: String, uploadStatus: UploadStatus)
