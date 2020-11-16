package com.vyulabs.update.updater.uploaders

import java.io.File
import java.nio.file.Files
import java.util.Date

import com.vyulabs.update.common.Common
import com.vyulabs.update.distribution.DistributionDirectoryClient
import com.vyulabs.update.info.{FaultInfo, ProfiledServiceName, ServiceState}
import com.vyulabs.update.info.FaultInfo._
import com.vyulabs.update.utils.{IoUtils, Utils, ZipUtils}
import com.vyulabs.update.version.{DeveloperVersion, DeveloperDistributionVersion}

import scala.collection.immutable.Queue
import org.slf4j.Logger
import spray.json.enrichAny

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 19.12.19.
  * Copyright FanDate, Inc.
  */
class FaultUploader(archiveDir: File, clientDirectory: DistributionDirectoryClient)
                   (implicit log: Logger) extends Thread { self =>
  private case class FaultReport(info: FaultInfo, reportFilesTmpDir: Option[File])

  private var faults = Queue.empty[FaultReport]
  private val maxServiceDirectoryCapacity = 1000 * 1024 * 1024
  private var stopping = false

  if (!archiveDir.exists() && !archiveDir.mkdir()) {
    Utils.error(s"Can't create directory ${archiveDir}")
  }

  def addFaultReport(info: FaultInfo, reportFilesTmpDir: Option[File]): Unit = {
    self.synchronized {
      faults = faults.enqueue(FaultReport(info, reportFilesTmpDir))
      self.notify()
    }
  }

  def close(): Unit = {
    self.synchronized {
      stopping = true
      notify()
    }
    join()
  }

  override def run(): Unit = {
    while (true) {
      val fault = self.synchronized {
        while (!stopping && faults.isEmpty) {
          self.wait()
        }
        if (stopping) {
          return
        }
        val ret = faults.dequeue
        faults = ret._2
        ret._1
      }
      if (!uploadFault(fault)) {
        log.error(s"Can't upload fault report ${fault}")
      }
    }
  }

  private def uploadFault(fault: FaultReport): Boolean = {
    try {
      val serviceDir = new File(archiveDir, Utils.serializeISO8601Date(new Date))
      if (!serviceDir.mkdir()) {
        log.error(s"Can't create directory ${serviceDir}")
        return false
      }
      val profiledServiceName = ProfiledServiceName(fault.info.serviceName, fault.info.serviceProfile)
      val archivedFileName = s"${profiledServiceName}_${fault.info.state.version.getOrElse(DeveloperDistributionVersion("???", DeveloperVersion.empty))}_${fault.info.instanceId}_${Utils.serializeISO8601Date(fault.info.date)}_fault.zip"
      val archiveFile = new File(serviceDir, archivedFileName)
      val tmpDirectory = Files.createTempDirectory(s"fault-${profiledServiceName}").toFile
      val faultInfoFile = new File(tmpDirectory, Common.FaultInfoFileName)
      val logTailFile = new File(tmpDirectory, s"${profiledServiceName}.log")
      try {
        if (!IoUtils.writeJsonToFile(faultInfoFile, fault.info.toJson)) {
          log.error(s"Can't write file with state")
          return false
        }
        val logs = fault.info.logTail.foldLeft(new String) { (sum, line) => sum + '\n' + line }
        if (!IoUtils.writeBytesToFile(logTailFile, logs.getBytes("utf8"))) {
          log.error(s"Can't write file with tail of logs")
          return false
        }
        val filesToZip = fault.reportFilesTmpDir.toSeq :+ faultInfoFile :+ logTailFile
        if (!ZipUtils.zip(archiveFile, filesToZip)) {
          log.error(s"Can't zip ${filesToZip} to ${archiveFile}")
          return false
        }
      } finally {
        IoUtils.deleteFileRecursively(tmpDirectory)
      }
      fault.reportFilesTmpDir.foreach(IoUtils.deleteFileRecursively(_))
      if (!clientDirectory.uploadServiceFault(profiledServiceName.name, archiveFile)) {
        log.error(s"Can't upload service fault file")
        return false
      }
      IoUtils.maybeFreeSpace(serviceDir, maxServiceDirectoryCapacity, Set(archiveFile))
      true
    } catch {
      case ex: Exception =>
        log.error("Uploading fault report exception", ex)
        false
    }
  }
}
