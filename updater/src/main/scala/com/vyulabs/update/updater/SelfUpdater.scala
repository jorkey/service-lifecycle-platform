package com.vyulabs.update.updater

import java.io.File

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.client.DistributionInterface
import com.vyulabs.update.utils.{IoUtils, Utils}
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 16.01.19.
  * Copyright FanDate, Inc.
  */
class SelfUpdater(state: ServiceStateController, clientDirectory: DistributionInterface)
                 (implicit log: Logger) {
  private val scriptsVersion = IoUtils.readServiceVersion(Common.ScriptsServiceName, new File("."))

  state.serviceStarted()

  def needUpdate(serviceName: ServiceName, desiredVersion: Option[ClientDistributionVersion]): Option[ClientDistributionVersion] = {
    Utils.isServiceNeedUpdate(serviceName, getVersion(serviceName), desiredVersion)
  }

  def stop(): Unit = {
    state.serviceStopped()
  }

  def beginServiceUpdate(serviceName: ServiceName, toVersion: ClientDistributionVersion): Boolean = {
    state.info(s"Service ${serviceName} is obsolete. Own version ${getVersion(serviceName)} desired version ${toVersion}")
    state.beginUpdateToVersion(toVersion)
    log.info(s"Downloading ${serviceName} of version ${toVersion}")
    if (!clientDirectory.downloadClientVersionImage(serviceName, toVersion, new File(Common.ServiceZipName.format(serviceName)))) {
      state.updateError(false, s"Downloading ${serviceName} error")
      return false
    }
    if (!IoUtils.writeServiceVersion(new File("."), serviceName, toVersion)) {
      state.updateError(true, s"Set ${serviceName} version error")
      return false
    }
    true
  }

  private def getVersion(serviceName: ServiceName): Option[ClientDistributionVersion] = {
    if (serviceName == Common.UpdaterServiceName) {
      state.getVersion()
    } else if (serviceName == Common.ScriptsServiceName) {
      scriptsVersion
    } else {
      None
    }
  }
}

object SelfUpdater {
  def apply(state: ServiceStateController, clientDirectory: DistributionInterface)
           (implicit log: Logger): SelfUpdater = {
    new SelfUpdater(state, clientDirectory)
  }
}
