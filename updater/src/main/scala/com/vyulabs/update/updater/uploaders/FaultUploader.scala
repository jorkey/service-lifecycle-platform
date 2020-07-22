package com.vyulabs.update.updater.uploaders

import java.io.File
import java.nio.file.Files
import java.util.Date

import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.state.ServiceState
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

import scala.collection.immutable.Queue

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 19.12.19.
  * Copyright FanDate, Inc.
  */
case class FaultReport(instanceId: InstanceId, state: ServiceState, reportFilesTmpDir: Option[File], logTail: Queue[String])

class FaultUploader(archiveDir: File, clientDirectory: ClientDistributionDirectoryClient)
                   (implicit log: Logger) extends Thread { self =>
  private var faults = Queue.empty[FaultReport]
  private val maxServiceDirectoryCapacity = 1000 * 1024 * 1024
  private var stopping = false

  if (!archiveDir.exists() && !archiveDir.mkdir()) {
    Utils.error(s"Can't create directory ${archiveDir}")
  }

  def addFaultReport(fault: FaultReport): Unit = {
    self.synchronized {
      faults = faults.enqueue(fault)
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
      val archivedFileName = s"${fault.state.serviceInstanceName.serviceName}_${fault.state.version.getOrElse(BuildVersion.empty)}_${fault.instanceId}_${Utils.serializeISO8601Date(new Date())}_fault.zip"
      val archiveFile = new File(serviceDir, archivedFileName)
      val tmpDirectory = Files.createTempDirectory(s"fault-${fault.state.serviceInstanceName.serviceName}").toFile
      val stateInfoFile = new File(tmpDirectory, "state.json")
      val logTailFile = new File(tmpDirectory, s"${fault.state.serviceInstanceName.serviceName}.log")
      try {
        if (!Utils.writeConfigFile(stateInfoFile, fault.state.toConfig())) {
          log.error(s"Can't write file with state")
          return false
        }
        val logs = fault.logTail.foldLeft(new String) { (sum, line) => sum + '\n' + line }
        if (!Utils.writeFileFromBytes(logTailFile, logs.getBytes("utf8"))) {
          log.error(s"Can't write file with tail of logs")
          return false
        }
        val filesToZip = fault.reportFilesTmpDir.toSeq :+ stateInfoFile :+ logTailFile
        if (!Utils.zip(archiveFile, filesToZip)) {
          log.error(s"Can't zip ${filesToZip} to ${archiveFile}")
          return false
        }
      } finally {
        Utils.deleteFileRecursively(tmpDirectory)
      }
      fault.reportFilesTmpDir.foreach(Utils.deleteFileRecursively(_))
      if (!clientDirectory.uploadServiceFault(fault.state.serviceInstanceName.serviceName, archiveFile)) {
        log.error(s"Can't upload service fault file")
        return false
      }
      Utils.maybeFreeSpace(serviceDir, maxServiceDirectoryCapacity, Set(archiveFile))
      true
    } catch {
      case ex: Exception =>
        log.error("Uploading fault report exception", ex)
        false
    }
  }
}
