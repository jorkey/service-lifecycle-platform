package distribution.mongo

import java.util.Date

import com.vyulabs.update.common.Common.{ClientName, InstanceId, ProfileName, ServiceDirectory, ServiceName}
import com.vyulabs.update.config.ClientInfo
import com.vyulabs.update.info.{ClientFaultReport, ClientServiceState, DesiredVersion, DeveloperVersionInfo, InstalledVersionInfo, LogLine}

case class ClientInfoDocument(info: ClientInfo)
case class ClientProfileDocument(profileName: ProfileName, services: Set[ServiceName])

case class DesiredVersionsDocument(versions: Seq[DesiredVersion])
case class PersonalDesiredVersionsDocument(clientName: ClientName, versions: Seq[DesiredVersion])
case class InstalledDesiredVersionsDocument(clientName: ClientName, versions: Seq[DesiredVersion])

case class DeveloperVersionInfoDocument(versionInfo: DeveloperVersionInfo)
case class FaultReportDocument(report: ClientFaultReport)
case class InstalledVersionInfoDocument(versionInfo: InstalledVersionInfo)
case class ServiceLogLineDocument(clientName: ClientName, serviceName: ServiceName, instanceId: InstanceId, directory: ServiceDirectory, logLine: LogLine)
case class ServiceStateDocument(state: ClientServiceState)

case class TestSignature(clientName: ClientName, date: Date)
case class TestedDesiredVersionsDocument(profileName: ProfileName, versions: Seq[DesiredVersion], signatures: Seq[TestSignature])

case class UploadStatus(lastUploadSequence: Long, lastError: Option[String])
case class UploadStatusDocument(component: String, uploadStatus: UploadStatus)
