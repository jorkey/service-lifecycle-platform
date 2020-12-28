package com.vyulabs.update.distribution.mongo

import com.vyulabs.update.common.common.Common.DistributionName
import com.vyulabs.update.common.config.{DistributionClientInfo, DistributionClientProfile}
import com.vyulabs.update.common.info.{ClientDesiredVersion, ClientVersionInfo, DeveloperDesiredVersion, DeveloperVersionInfo, DistributionFaultReport, DistributionServiceState, ServiceLogLine, TestedDesiredVersions}
import com.vyulabs.update.distribution.users.ServerUserInfo

case class SequenceDocument(name: String, sequence: Long)

case class UserInfoDocument(content: ServerUserInfo)

case class DistributionClientInfoDocument(content: DistributionClientInfo)
case class DistributionClientProfileDocument(content: DistributionClientProfile)

case class DeveloperVersionInfoDocument(_id: Long, content: DeveloperVersionInfo)
case class ClientVersionInfoDocument(_id: Long, content: ClientVersionInfo)

case class DeveloperDesiredVersionsDocument(content: Seq[DeveloperDesiredVersion], _id: Long = 0)
case class ClientDesiredVersionsDocument(content: Seq[ClientDesiredVersion], _id: Long = 0)

case class InstalledDesiredVersionsDocument(distributionName: DistributionName, versions: Seq[ClientDesiredVersion])
case class TestedDesiredVersionsDocument(content: TestedDesiredVersions)

case class ServiceStateDocument(sequence: Long, content: DistributionServiceState)
case class ServiceLogLineDocument(_id: Long, content: ServiceLogLine)
case class FaultReportDocument(_id: Long, content: DistributionFaultReport)

case class UploadStatus(lastUploadSequence: Option[Long], lastError: Option[String])
case class UploadStatusDocument(component: String, status: UploadStatus)
