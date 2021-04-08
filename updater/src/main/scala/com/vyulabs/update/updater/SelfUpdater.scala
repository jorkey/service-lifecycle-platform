package com.vyulabs.update.updater

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.ServiceName
import com.vyulabs.update.common.distribution.client.{DistributionClient, SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.utils.{IoUtils, Utils}
import com.vyulabs.update.common.version.ClientDistributionVersion
import org.slf4j.Logger

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 16.01.19.
  * Copyright FanDate, Inc.
  */
class SelfUpdater(state: ServiceStateController, distributionClient: DistributionClient[SyncSource])
                 (implicit executionContext: ExecutionContext, log: Logger) {
  private val syncDistributionClient = new SyncDistributionClient[SyncSource](distributionClient, FiniteDuration(60, TimeUnit.SECONDS))

  private val scriptsVersion = IoUtils.readServiceVersion(Common.ScriptsServiceName, new File("."))
  private val updaterVersion = IoUtils.readServiceVersion(Common.UpdaterServiceName, new File("."))

  state.serviceStarted()

  def needUpdate(service: ServiceName, desiredVersion: Option[ClientDistributionVersion]): Option[ClientDistributionVersion] = {
    Utils.isServiceNeedUpdate(service, getVersion(service), desiredVersion)
  }

  def stop(): Unit = {
    state.serviceStopped()
  }

  def beginServiceUpdate(service: ServiceName, toVersion: ClientDistributionVersion): Boolean = {
    log.info(s"Service ${service} is obsolete. Own version ${getVersion(service)} desired version ${toVersion}")
    state.beginUpdateToVersion(toVersion)
    log.info(s"Downloading ${service} of version ${toVersion}")
    if (!syncDistributionClient.downloadClientVersionImage(service, toVersion, new File(Common.ServiceZipName.format(service)))) {
      state.updateError(false, s"Downloading ${service} error")
      return false
    }
    if (!IoUtils.writeServiceVersion(new File("."), service, toVersion)) {
      state.updateError(true, s"Set ${service} version error")
      return false
    }
    true
  }

  private def getVersion(service: ServiceName): Option[ClientDistributionVersion] = {
    if (service == Common.UpdaterServiceName) {
      updaterVersion
    } else if (service == Common.ScriptsServiceName) {
      scriptsVersion
    } else {
      None
    }
  }
}