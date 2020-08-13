package com.vyulabs.update.updater

import com.vyulabs.update.common.Common.VmInstanceId
import com.vyulabs.update.common.{Common, ServiceInstanceName}
import com.vyulabs.update.config.InstallConfig
import com.vyulabs.update.utils.{IOUtils, ProcessUtils}
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.updater.uploaders.{FaultUploader, LogUploader}
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 16.01.19.
  * Copyright FanDate, Inc.
  */
class ServiceUpdater(instanceId: VmInstanceId,
                     val serviceInstanceName: ServiceInstanceName,
                     state: ServiceStateController,
                     clientDirectory: ClientDistributionDirectoryClient)
                    (implicit log: Logger) {
  private var serviceRunner = Option.empty[ServiceRunner]

  private val faultUploader = new FaultUploader(state.faultsDirectory, clientDirectory)

  faultUploader.start()

  def close(): Unit = {
    for (serviceRunner <- serviceRunner) {
      serviceRunner.stopService()
      this.serviceRunner = None
    }
    faultUploader.close()
  }

  def needUpdate(desiredVersion: Option[BuildVersion]): Option[BuildVersion] = {
    desiredVersion match {
      case Some(newVersion) if (state.getVersion.isEmpty) =>
        state.info(s"Service is not installed")
        Some(newVersion)
      case Some(newVersion) if (!state.getVersion.contains(newVersion)) =>
        state.info(s"Is obsolete. Current version ${state.getVersion()} desired version ${newVersion}")
        Some(newVersion)
      case Some(_) =>
        state.info(s"Up to date")
        None
      case None =>
        state.error(s"No desired version for service")
        None
    }
  }

  def beginInstall(newVersion: BuildVersion): Boolean = {
    state.info("Begin install")

    if (!state.serviceDirectory.exists() && !state.serviceDirectory.mkdir()) {
      state.error(s"Can't make directory ${state.serviceDirectory}")
      return false
    }

    state.beginUpdateToVersion(newVersion)

    state.info(s"Download version ${newVersion}")
    if (state.newServiceDirectory.exists() && !IOUtils.deleteFileRecursively(state.newServiceDirectory)) {
      state.error(s"Can't remove directory ${state.newServiceDirectory}")
      return false
    }
    if (!state.newServiceDirectory.mkdir()) {
      state.error(s"Can't make directory ${state.newServiceDirectory}")
      return false
    }
    if (!clientDirectory.downloadVersion(serviceInstanceName.serviceName, newVersion, state.newServiceDirectory)) {
      state.error(s"Can't download ${serviceInstanceName.serviceName} version ${newVersion}")
      return false
    }

    state.info(s"Install service")
    var args = Map.empty[String, String]
    args += ("profile" -> serviceInstanceName.serviceProfile)
    args += ("version" -> newVersion.original().toString)
    args += ("PATH" -> System.getenv("PATH"))

    val installConfig = InstallConfig.read(state.newServiceDirectory).getOrElse {
      state.error(s"No install config in the build directory")
      return false
    }

    for (command <- installConfig.installCommands.getOrElse(Seq.empty)) {
      if (!ProcessUtils.runProcess(command, args, state.newServiceDirectory, ProcessUtils.Logging.Realtime)) {
        state.error(s"Install error")
        return false
      }
    }

    true
  }

  def finishInstall(newVersion: BuildVersion): Boolean = {
    state.info("Finish install")

    if (state.currentServiceDirectory.exists()) {
      for (serviceRunner <- serviceRunner) {
        state.info(s"Stop old version ${state.getVersion()}")
        if (serviceRunner.stopService()) {
          state.serviceStopped()
        } else {
          state.error(s"Can't stop service")
        }
        serviceRunner.saveLogs(false)
        this.serviceRunner = None
      }

      if (!IOUtils.deleteFileRecursively(state.currentServiceDirectory)) {
        state.error(s"Can't delete ${state.currentServiceDirectory}")
        return false
      }
    }

    if (!state.newServiceDirectory.renameTo(state.currentServiceDirectory)) {
      state.error(s"Can't rename ${state.newServiceDirectory} to ${state.currentServiceDirectory}")
      return false
    }

    val installConfig = InstallConfig.read(state.currentServiceDirectory).getOrElse {
      state.error(s"No install config in the build directory")
      return false
    }

    state.info(s"Post install service")
    var args = Map.empty[String, String]
    args += ("profile" -> serviceInstanceName.serviceProfile)
    args += ("version" -> newVersion.original().toString)
    args += ("PATH" -> System.getenv("PATH"))

    for (command <- installConfig.postInstallCommands.getOrElse(Seq.empty)) {
      if (!ProcessUtils.runProcess(command, args, state.currentServiceDirectory, ProcessUtils.Logging.Realtime)) {
        state.error(s"Install error")
        return false
      }
    }

    IOUtils.writeServiceVersion(state.currentServiceDirectory, serviceInstanceName.serviceName, newVersion)

    state.setVersion(newVersion)

    true
  }

  def isExecuted(): Boolean = {
    serviceRunner.isDefined
  }

  def execute(): Boolean = {
    if (serviceRunner.isEmpty) {
      val newVersion = state.getVersion().getOrElse {
        state.error("Can't start service because it is not installed")
        return false
      }

      val installConfig = InstallConfig.read(state.currentServiceDirectory).getOrElse {
        state.error(s"No install config in the build directory")
        return false
      }

      for (runService <- installConfig.runService) {
        state.info(s"Start service of version ${newVersion}")

        var args = Map.empty[String, String]
        args += ("profile" -> serviceInstanceName.serviceProfile)
        args += ("version" -> newVersion.original().toString)

        val runner = new ServiceRunner(instanceId, serviceInstanceName, state, clientDirectory, faultUploader)
        if (!runner.startService(runService, args, state.currentServiceDirectory)) {
          state.error(s"Can't start service")
          return false
        }
        serviceRunner = Some(runner)
      }

      state.serviceStarted()
    } else {
      state.error(s"Service is already started")
    }

    true
  }
}
