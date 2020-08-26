package com.vyulabs.update.distribution.client

import java.io.File

import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import org.slf4j.LoggerFactory

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class ClientDistributionDirectory(directory: File)(implicit filesLocker: SmartFilesLocker)
      extends DistributionDirectory(directory) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val logsDir = new File(directory, "logs")
  protected val faultsDir = new File(directory, "faults")

  if (!logsDir.exists() && !logsDir.mkdir()) {
    log.error(s"Can't create directory ${logsDir}")
  }

  if (!faultsDir.exists() && !faultsDir.mkdir()) {
    log.error(s"Can't create directory ${faultsDir}")
  }

  def getLogsDir() = logsDir

  def getFaultsDir() = faultsDir
}