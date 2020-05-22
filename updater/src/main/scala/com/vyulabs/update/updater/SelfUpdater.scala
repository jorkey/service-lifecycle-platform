package com.vyulabs.update.updater

import java.io.File

import com.vyulabs.update.common.Common
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 16.01.19.
  * Copyright FanDate, Inc.
  */
class SelfUpdater(state: ServiceStateController, clientDirectory: ClientDistributionDirectoryClient)
                 (implicit log: Logger) {
  private val scriptsVersion = Utils.getScriptsVersion()

  state.serviceStarted()

  def updaterNeedUpdate(desiredVersion: Option[BuildVersion]): Option[BuildVersion] = {
    Utils.isServiceNeedUpdate(Common.UpdaterServiceName, state.getVersion(), desiredVersion)
  }

  def scriptsNeedUpdate(desiredVersion: Option[BuildVersion]): Option[BuildVersion] = {
    Utils.isServiceNeedUpdate(Common.ScriptsServiceName, scriptsVersion, desiredVersion)
  }

  def beginUpdate(toVersion: BuildVersion): Unit = {
    state.info(s"Updater is obsolete. Own version ${state.getVersion()} desired version ${toVersion}")
    state.setUpdateToVersion(toVersion)
    log.info(s"Downloading updater of version ${toVersion}")
    for (fromVersion <- state.getVersion()) {
      new File(Common.UpdaterJarName.format(fromVersion.toString)).deleteOnExit()
    }
    if (!clientDirectory.downloadVersion(Common.UpdaterServiceName, toVersion, new File("."))) {
      log.error("Downloading updater error")
    }
    state.serviceStopped()
  }

  def beginScriptsUpdate(toVersion: BuildVersion): Unit = {
    state.setScriptsUpdateToVersion(toVersion)
  }
}

object SelfUpdater {
  def apply(state: ServiceStateController, clientDirectory: ClientDistributionDirectoryClient)
           (implicit log: Logger): SelfUpdater = {
    new SelfUpdater(state, clientDirectory)
  }
}
