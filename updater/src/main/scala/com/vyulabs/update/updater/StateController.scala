package com.vyulabs.update.updater

import java.io.File
import java.util.Date

import com.vyulabs.update.common.Common
import com.vyulabs.update.info.UpdateError
import com.vyulabs.update.info.{ProfiledServiceName, ServiceState}
import com.vyulabs.update.utils.{IoUtils, Utils}
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 10.04.19.
  * Copyright FanDate, Inc.
  */
class ServiceStateController(profiledServiceName: ProfiledServiceName, updateRepository: () => Unit)(implicit log: Logger) {
  val serviceDirectory = new File(if (profiledServiceName.name == Common.UpdaterServiceName) "." else profiledServiceName.toString)
  val currentServiceDirectory = new File(serviceDirectory, "current")
  val faultsDirectory = new File(serviceDirectory, "faults")
  val newServiceDirectory = new File(serviceDirectory, "new")
  val logHistoryDirectory = new File(serviceDirectory, "log.history")

  @volatile private var installDate = Option.empty[Date]
  @volatile private var startDate = Option.empty[Date]
  @volatile private var version = Option.empty[ClientDistributionVersion]
  @volatile private var updateToVersion = Option.empty[ClientDistributionVersion]
  @volatile private var updateError: Option[UpdateError] = None
  @volatile private var lastExitCode = Option.empty[Int]
  @volatile private var failuresCount = Option.empty[Int]

  private val maxLastErrors = 25

  if (!serviceDirectory.exists() && !serviceDirectory.mkdir()) {
    Utils.error(s"Can't create directory ${serviceDirectory}")
  }

  version = IoUtils.readServiceVersion(profiledServiceName.name, currentServiceDirectory)

  log.info(s"Current version of service ${profiledServiceName} is ${version}")

  def getVersion() = version

  def getUpdateError() = updateError

  def initFromState(state: ServiceState): Unit = {
    failuresCount = state.failuresCount
    if (profiledServiceName.name == Common.UpdaterServiceName) {
      if (updateToVersion.isEmpty) {
        failuresCount = Some(failuresCount.getOrElse(0) + 1)
      }
    }
  }

  def setVersion(version: ClientDistributionVersion): Unit = synchronized {
    this.installDate = Some(new Date())
    this.version = Some(version)
    if (updateToVersion.isDefined) {
      info(s"Updated to version ${version}")
      updateToVersion = None
      updateError = None
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

  def serviceRemoved(): Unit = synchronized {
    this.startDate = None
    this.version = None
    updateRepository()
  }

  def beginUpdateToVersion(serviceVersion: ClientDistributionVersion): Unit = synchronized {
    this.updateToVersion = Some(serviceVersion)
    this.updateError = None
    info(s"Begin update to version ${serviceVersion}")
    updateRepository()
  }

  def beginUpdateScriptsToVersion(serviceVersion: ClientDistributionVersion): Unit = synchronized {
    info(s"Begin update scripts to version ${serviceVersion}")
    updateRepository()
  }

  def updateError(critical: Boolean, msg: String): Unit = synchronized {
    this.updateError = Some(UpdateError(critical, msg))
    error(s"Update ${if (critical) "fatal " else ""}error: ${msg}")
    updateRepository()
  }

  def info(message: String): Unit = {
    log.info(s"Service ${profiledServiceName}: ${message}")
  }

  def error(message: String): Unit = {
    log.error(s"Service ${profiledServiceName}: ${message}")
    updateRepository()
  }

  def error(message: String, exception: Throwable): Unit = {
    log.error(s"Service ${profiledServiceName}: ${message}", exception)
    updateRepository()
  }

  def failure(exitCode: Int): Unit = {
    log.error(s"Service ${profiledServiceName} terminated unexpectedly with code ${exitCode}")
    lastExitCode = Some(exitCode)
    failuresCount = Some(failuresCount.getOrElse(0) + 1)
    updateRepository()
  }

  def getState(): ServiceState = {
    ServiceState(new Date(), installDate, startDate, version, updateToVersion, updateError, failuresCount, lastExitCode)
  }
}
