package com.vyulabs.update.distribution.mongo

import com.vyulabs.update.common.common.Common.DistributionId
import com.vyulabs.update.common.info._

case class InstalledDesiredVersions(distribution: DistributionId, versions: Seq[ClientDesiredVersion])

case class UploadStatus(lastUploadSequence: Option[Long], lastError: Option[String])
case class UploadStatusDocument(component: String, lastUploadSequence: Option[Long], lastError: Option[String])
