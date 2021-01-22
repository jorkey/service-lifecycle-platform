package com.vyulabs.update.distribution.mongo

import com.vyulabs.update.common.common.Common.DistributionName
import com.vyulabs.update.common.info._

case class SequenceDocument(name: String, sequence: Long)

case class InstalledDesiredVersions(distributionName: DistributionName, versions: Seq[ClientDesiredVersion])

case class UploadStatus(lastUploadSequence: Option[Long], lastError: Option[String])
case class UploadStatusDocument(component: String, status: UploadStatus)
