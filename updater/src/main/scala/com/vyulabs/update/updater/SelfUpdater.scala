package com.vyulabs.update.updater

import java.io.File

import com.vyulabs.update.common.Common
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.info.DesiredVersions
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

  def maybeBeginSelfUpdate(desiredVersions: DesiredVersions): Boolean = {
    val updaterUpdate = maybeBeginUpdaterUpdate(desiredVersions.Versions.get(Common.UpdaterServiceName))
    val scriptsUpdate = maybeBeginScriptsUpdate(desiredVersions.Versions.get(Common.ScriptsServiceName))
    updaterUpdate || scriptsUpdate
  }

  def stop(): Unit = {
    state.serviceStopped()
  }

  private def maybeBeginUpdaterUpdate(desiredVersion: Option[BuildVersion]): Boolean = {
    Utils.isServiceNeedUpdate(Common.UpdaterServiceName, state.getVersion(), desiredVersion) match {
      case Some(toVersion) =>
        state.info(s"Updater is obsolete. Own version ${state.getVersion()} desired version ${toVersion}")
        state.setUpdateToVersion(toVersion)
        log.info(s"Downloading updater of version ${toVersion}")
        for (fromVersion <- state.getVersion()) {
          new File(Common.UpdaterJarName.format(fromVersion.toString)).deleteOnExit()
        }
        if (!clientDirectory.downloadVersion(Common.UpdaterServiceName, toVersion, new File("."))) {
          log.error("Downloading updater error")
        }
        true
      case None =>
        false
    }
  }

  private def maybeBeginScriptsUpdate(desiredVersion: Option[BuildVersion]): Boolean = {
    Utils.isServiceNeedUpdate(Common.ScriptsServiceName, scriptsVersion, desiredVersion) match {
      case Some(toVersion) =>
        state.info(s"Scripts is obsolete. Own version ${scriptsVersion} desired version ${toVersion}")
        state.setScriptsUpdateToVersion(toVersion)
        log.info(s"Downloading scripts of version ${toVersion}")
        if (!clientDirectory.downloadVersionImage(Common.ScriptsServiceName, toVersion, new File(Common.ScriptsZipName))) {
          log.error("Downloading scripts error")
        }
        if (!Utils.writeServiceVersion(new File("."), Common.ScriptsServiceName, toVersion)) {
          log.error("Set scripts version error")
        }
        true
      case None =>
        false
    }
  }
}

object SelfUpdater {
  def apply(state: ServiceStateController, clientDirectory: ClientDistributionDirectoryClient)
           (implicit log: Logger): SelfUpdater = {
    new SelfUpdater(state, clientDirectory)
  }
}
