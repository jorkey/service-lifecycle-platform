package com.vyulabs.update.updater

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.info.{ServiceNameWithRole, ServiceState, UpdateError}
import com.vyulabs.update.common.utils.{IoUtils, Utils}
import com.vyulabs.update.common.version.ClientDistributionVersion
import org.slf4j.Logger

import java.io.File
import java.util.Date

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 10.04.19.
  * Copyright FanDate, Inc.
  */
class ServiceStateController(directory: File, profiledServiceName: ServiceNameWithRole, updateRepository: () => Unit)
                            (implicit log: Logger) {
  val serviceDirectory = if (profiledServiceName.name == Common.UpdaterServiceName) directory else new File(directory, profiledServiceName.toString)
  val currentServiceDirectory = if (profiledServiceName.name == Common.UpdaterServiceName) serviceDirectory else new File(serviceDirectory, "current")
  val faultsDirectory = new File(serviceDirectory, "faults")
  val newServiceDirectory = new File(serviceDirectory, "new")
  val logHistoryDirectory = new File(serviceDirectory, "log.history")

  serviceDirectory.mkdirs()

  if (profiledServiceName.name != Common.UpdaterServiceName) {
    currentServiceDirectory.mkdir()
    faultsDirectory.mkdir()
    newServiceDirectory.mkdir()
    logHistoryDirectory.mkdir()
  }

  @volatile private var installTime = Option.empty[Date]
  @volatile private var startTime = Option.empty[Date]
  @volatile private var version = Option.empty[ClientDistributionVersion]
  @volatile private var updateToVersion = Option.empty[ClientDistributionVersion]
  @volatile private var updateError: Option[UpdateError] = None
  @volatile private var lastExitCode = Option.empty[Int]
  @volatile private var failuresCount = Option.empty[Int]

  if (!serviceDirectory.exists() && !serviceDirectory.mkdir()) {
    Utils.error(s"Can't create directory ${serviceDirectory}")
  }

  version = IoUtils.readServiceVersion(profiledServiceName.name, currentServiceDirectory)

  log.info(s"Current version of service ${profiledServiceName} is ${version}")

  def getVersion() = version

  def getUpdateError() = updateError

  def setVersion(version: ClientDistributionVersion): Unit = synchronized {
    this.installTime = Some(new Date())
    this.version = Some(version)
    if (updateToVersion.isDefined) {
      log.info(s"Updated to version ${version}")
      updateToVersion = None
      updateError = None
    } else {
      log.info(s"Installed version ${version}")
    }
    updateRepository()
  }

  def serviceStarted(): Unit = synchronized {
    this.startTime = Some(new Date())
    updateRepository()
  }

  def serviceStopped(): Unit = synchronized {
    this.startTime = None
    updateRepository()
  }

  def serviceRemoved(): Unit = synchronized {
    this.startTime = None
    this.version = None
    updateRepository()
  }

  def beginUpdateToVersion(serviceVersion: ClientDistributionVersion): Unit = synchronized {
    this.updateToVersion = Some(serviceVersion)
    this.updateError = None
    log.info(s"Begin update to version ${serviceVersion}")
    updateRepository()
  }

  def updateError(critical: Boolean, msg: String): Unit = synchronized {
    this.updateError = Some(UpdateError(critical, msg))
    log.error(s"Update ${if (critical) "fatal " else ""}error: ${msg}")
    updateRepository()
  }

  def failure(exitCode: Int): Unit = {
    log.error(s"Service ${profiledServiceName} terminated unexpectedly with code ${exitCode}")
    lastExitCode = Some(exitCode)
    failuresCount = Some(failuresCount.getOrElse(0) + 1)
    updateRepository()
  }

  def getState(): ServiceState = {
    ServiceState(new Date(), installTime, startTime, version, updateToVersion, updateError, failuresCount, lastExitCode)
  }
}
