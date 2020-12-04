package com.vyulabs.update.updater

import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.config.InstallConfig
import com.vyulabs.update.distribution.graphql.OldDistributionInterface
import com.vyulabs.update.utils.{IoUtils, ProcessUtils}
import com.vyulabs.update.info.{ProfiledServiceName, UpdateError}
import com.vyulabs.update.updater.uploaders.FaultUploader
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 16.01.19.
  * Copyright FanDate, Inc.
  */
class ServiceUpdater(instanceId: InstanceId,
                     profiledServiceName: ProfiledServiceName,
                     state: ServiceStateController,
                     clientDirectory: OldDistributionInterface)
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

  def getUpdateError(): Option[UpdateError] = state.getUpdateError()

  def needUpdate(desiredVersion: Option[ClientDistributionVersion]): Option[ClientDistributionVersion] = {
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

  def beginInstall(newVersion: ClientDistributionVersion): Boolean = {
    try {
      state.info("Begin install")

      if (!state.serviceDirectory.exists() && !state.serviceDirectory.mkdir()) {
        state.updateError(true, s"Can't make directory ${state.serviceDirectory}")
        return false
      }

      state.beginUpdateToVersion(newVersion)

      state.info(s"Download version ${newVersion}")
      if (state.newServiceDirectory.exists() && !IoUtils.deleteFileRecursively(state.newServiceDirectory)) {
        state.updateError(true, s"Can't remove directory ${state.newServiceDirectory}")
        return false
      }
      if (!state.newServiceDirectory.mkdir()) {
        state.updateError(true, s"Can't make directory ${state.newServiceDirectory}")
        return false
      }
      if (!clientDirectory.downloadClientVersion(profiledServiceName.name, newVersion, state.newServiceDirectory)) {
        state.updateError(false, s"Can't download ${profiledServiceName.name} version ${newVersion}")
        return false
      }

      state.info(s"Install service")
      var args = Map.empty[String, String]
      args += ("profile" -> profiledServiceName.profile)
      args += ("version" -> newVersion.original().toString)
      args += ("PATH" -> System.getenv("PATH"))

      val installConfig = InstallConfig.read(state.newServiceDirectory).getOrElse {
        state.updateError(true, s"No install config in the build directory")
        return false
      }

      for (command <- installConfig.installCommands.getOrElse(Seq.empty)) {
        if (!ProcessUtils.runProcess(command, args, state.newServiceDirectory, ProcessUtils.Logging.Realtime)) {
          state.updateError(true, s"Install error")
          return false
        }
      }

      true
    } catch {
      case e: Exception =>
        state.error(s"Install exception", e)
        false
    }
  }

  def finishInstall(newVersion: ClientDistributionVersion): Boolean = {
    try {
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

        if (!IoUtils.deleteFileRecursively(state.currentServiceDirectory)) {
          state.updateError(true, s"Can't delete ${state.currentServiceDirectory}")
          return false
        }

        state.serviceRemoved()
      }

      if (!state.newServiceDirectory.renameTo(state.currentServiceDirectory)) {
        state.updateError(true, s"Can't rename ${state.newServiceDirectory} to ${state.currentServiceDirectory}")
        return false
      }

      val installConfig = InstallConfig.read(state.currentServiceDirectory).getOrElse {
        state.updateError(true, s"No install config in the build directory")
        return false
      }

      state.info(s"Post install service")
      var args = Map.empty[String, String]
      args += ("profile" -> profiledServiceName.profile)
      args += ("version" -> newVersion.original().toString)
      args += ("PATH" -> System.getenv("PATH"))

      for (command <- installConfig.postInstallCommands.getOrElse(Seq.empty)) {
        if (!ProcessUtils.runProcess(command, args, state.currentServiceDirectory, ProcessUtils.Logging.Realtime)) {
          state.updateError(true, s"Install error")
          return false
        }
      }

      IoUtils.writeServiceVersion(state.currentServiceDirectory, profiledServiceName.name, newVersion)

      state.setVersion(newVersion)

      true
    } catch {
      case e: Exception =>
        state.error(s"Install exception", e)
        false
    }
  }

  def isExecuted(): Boolean = {
    serviceRunner.isDefined
  }

  def execute(): Boolean = {
    try {
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
          args += ("profile" -> profiledServiceName.profile)
          args += ("version" -> newVersion.original().toString)

          val runner = new ServiceRunner(instanceId, profiledServiceName, state, clientDirectory, faultUploader)
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
    } catch {
      case e: Exception =>
        state.error(s"Execute exception", e)
        false
    }
  }
}
