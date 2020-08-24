package com.vyulabs.update.updater

import java.io.File
import java.util.Date

import com.vyulabs.update.common.Common
import com.vyulabs.update.state.{ProfiledServiceName, ServiceState}
import com.vyulabs.update.utils.{IOUtils, Utils}
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 10.04.19.
  * Copyright FanDate, Inc.
  */
class ServiceStateController(profiledServiceName: ProfiledServiceName,
                             updateRepository: () => Unit)(implicit log: Logger) {
  val serviceDirectory = new File(profiledServiceName.toString)
  val currentServiceDirectory = new File(serviceDirectory, "current")
  val faultsDirectory = new File(serviceDirectory, "faults")
  val newServiceDirectory = new File(serviceDirectory, "new")
  val logHistoryDirectory = new File(serviceDirectory, "log.history")

  @volatile private var startDate = Option.empty[Date]
  @volatile private var version = Option.empty[BuildVersion]
  @volatile private var updateToVersion = Option.empty[BuildVersion]
  @volatile private var lastErrors = Option.empty[Seq[String]]
  @volatile private var lastExitCode = Option.empty[Int]
  @volatile private var failuresCount = Option.empty[Int]

  private val maxLastErrors = 25

  if (!serviceDirectory.exists() && !serviceDirectory.mkdir()) {
    Utils.error(s"Can't create directory ${serviceDirectory}")
  }

  version = if (profiledServiceName.service == Common.UpdaterServiceName) {
    Utils.getManifestBuildVersion(Common.UpdaterServiceName)
  } else {
    IOUtils.readServiceVersion(profiledServiceName.service, currentServiceDirectory)
  }

  log.info(s"Current version of service ${profiledServiceName} is ${version}")

  def getVersion() = version

  def initFromState(state: ServiceState): Unit = {
    failuresCount = state.failuresCount
    if (profiledServiceName.service == Common.UpdaterServiceName) {
      if (updateToVersion.isEmpty) {
        failuresCount = Some(failuresCount.getOrElse(0) + 1)
      }
    }
  }

  def setVersion(version: BuildVersion): Unit = synchronized {
    this.version = Some(version)
    if (updateToVersion.isDefined) {
      info(s"Updated to version ${version}")
      updateToVersion = None
    } else {
      info(s"Installed version ${version}")
    }
    updateRepository()
  }

  def serviceStarted(): Unit = synchronized {
    this.startDate = Some(new Date())
    updateRepository()
  }

  def serviceStopped(): Unit = synchronized {
    this.startDate = None
    updateRepository()
  }

  def beginUpdateToVersion(serviceVersion: BuildVersion): Unit = synchronized {
    this.updateToVersion = Some(serviceVersion)
    info(s"Begin update to version ${serviceVersion}")
    updateRepository()
  }

  def beginUpdateScriptsToVersion(serviceVersion: BuildVersion): Unit = synchronized {
    info(s"Begin update scripts to version ${serviceVersion}")
    updateRepository()
  }

  def info(message: String): Unit = {
    log.info(s"Service ${profiledServiceName}: ${message}")
  }

  def error(message: String): Unit = {
    log.error(s"Service ${profiledServiceName}: ${message}")
    addLastError(message)
    updateRepository()
  }

  def error(message: String, exception: Throwable): Unit = {
    log.error(s"Service ${profiledServiceName}: ${message}", exception)
    addLastError(message + ": " + exception.toString)
    updateRepository()
  }

  def failure(exitCode: Int): Unit = {
    log.error(s"Service ${profiledServiceName} terminated unexpectedly with code ${exitCode}")
    lastExitCode = Some(exitCode)
    failuresCount = Some(failuresCount.getOrElse(0) + 1)
    updateRepository()
  }

  def getState(): ServiceState = {
    ServiceState(new Date(), startDate, version, updateToVersion, failuresCount, lastErrors, lastExitCode)
  }

  private def addLastError(error: String): Unit = {
    lastErrors = Some(lastErrors.getOrElse(Seq.empty) :+ error)
    if (lastErrors.get.size == maxLastErrors + 1) {
      lastErrors = Some(lastErrors.get.drop(1))
    }
  }
}
