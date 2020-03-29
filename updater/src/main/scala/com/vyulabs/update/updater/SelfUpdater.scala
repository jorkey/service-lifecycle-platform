package com.vyulabs.update.updater

import java.io.File

import com.vyulabs.update.common.{Common}
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 16.01.19.
  * Copyright FanDate, Inc.
  */
class SelfUpdater(state: ServiceStateController, clientDirectory: ClientDistributionDirectoryClient)
                 (implicit log: Logger) {
  state.serviceStarted()

  def needUpdate(desiredVersion: Option[BuildVersion]): Option[BuildVersion] = {
    state.getVersion() match {
      case Some(version) =>
        if (!version.isEmpty) {
          desiredVersion match {
            case Some(newVersion) if (!BuildVersion.ordering.equiv(version, newVersion)) =>
              state.info(s"Is obsolete. Own version ${version} desired version ${newVersion}")
              Some(newVersion)
            case Some(_) =>
              state.info("Up to date")
              None
            case None =>
              state.info(s"No desired version for updater")
              None
          }
        } else {
          None
        }
      case None =>
        None
    }
  }

  def update(toVersion: BuildVersion): Unit = {
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
}

object SelfUpdater {
  def apply(state: ServiceStateController, clientDirectory: ClientDistributionDirectoryClient)
           (implicit log: Logger): SelfUpdater = {
    new SelfUpdater(state, clientDirectory)
  }
}
