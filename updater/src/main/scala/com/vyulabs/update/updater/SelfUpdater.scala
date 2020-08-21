package com.vyulabs.update.updater

import java.io.File

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.info.DesiredVersions
import com.vyulabs.update.utils.{IOUtils, Utils}
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 16.01.19.
  * Copyright FanDate, Inc.
  */
class SelfUpdater(state: ServiceStateController, clientDirectory: ClientDistributionDirectoryClient)
                 (implicit log: Logger) {
  private val scriptsVersion = IOUtils.readServiceVersion(Common.ScriptsServiceName, new File("."))

  state.serviceStarted()

  def maybeBeginSelfUpdate(desiredVersions: DesiredVersions): Boolean = {
    val updaterUpdate = maybeBeginServiceUpdate(Common.UpdaterServiceName, state.getVersion(), desiredVersions.desiredVersions.get(Common.UpdaterServiceName))
    val scriptsUpdate = maybeBeginServiceUpdate(Common.ScriptsServiceName, scriptsVersion, desiredVersions.desiredVersions.get(Common.ScriptsServiceName))
    updaterUpdate || scriptsUpdate
  }

  def stop(): Unit = {
    state.serviceStopped()
  }

  private def maybeBeginServiceUpdate(serviceName: ServiceName, fromVersion: Option[BuildVersion], toVersion: Option[BuildVersion]): Boolean = {
    Utils.isServiceNeedUpdate(serviceName, fromVersion, toVersion) match {
      case Some(toVersion) =>
        state.info(s"Service ${serviceName} is obsolete. Own version ${fromVersion} desired version ${toVersion}")
        state.beginUpdateToVersion(toVersion)
        log.info(s"Downloading ${serviceName} of version ${toVersion}")
        if (!clientDirectory.downloadVersionImage(serviceName, toVersion, new File(Common.ServiceZipName.format(serviceName)))) {
          log.error(s"Downloading ${serviceName} error")
        }
        if (!IOUtils.writeServiceVersion(new File("."), serviceName, toVersion)) {
          log.error(s"Set ${serviceName} version error")
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
