package com.vyulabs.update.updater

import com.vyulabs.update.common.common.Common.InstanceId
import com.vyulabs.update.common.common.Timer
import com.vyulabs.update.common.config.InstallConfig
import com.vyulabs.update.common.distribution.client.{DistributionClient, SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.info.{ServiceNameWithRole, UpdateError}
import com.vyulabs.update.common.logger.LogUploader
import com.vyulabs.update.common.process.ProcessUtils
import com.vyulabs.update.common.utils.{IoUtils, ZipUtils}
import com.vyulabs.update.common.version.ClientDistributionVersion
import com.vyulabs.update.updater.uploaders.FaultUploaderImpl
import org.slf4j.Logger

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 16.01.19.
  * Copyright FanDate, Inc.
  */
class ServiceUpdater(instance: InstanceId, service: ServiceNameWithRole,
                     state: ServiceStateController, distributionClient: DistributionClient[SyncSource])
                    (implicit timer: Timer, executionContext: ExecutionContext, log: Logger) {
  private var serviceRunner = Option.empty[ServiceRunner]

  private val faultUploader = new FaultUploaderImpl(state.faultsDirectory, distributionClient)
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
        log.info(s"Service is not installed")
        Some(newVersion)
      case Some(newVersion) if (!state.getVersion.contains(newVersion)) =>
        log.info(s"Is obsolete. Current version ${state.getVersion()} desired version ${newVersion}")
        Some(newVersion)
      case Some(_) =>
        log.info(s"Up to date")
        None
      case None =>
        log.error(s"No desired version of service")
        None
    }
  }

  def beginInstall(newVersion: ClientDistributionVersion): Boolean = {
    val syncDistributionClient = new SyncDistributionClient[SyncSource](distributionClient, FiniteDuration(60, TimeUnit.SECONDS))
    try {
      log.info("Begin install")

      if (!state.serviceDirectory.exists() && !state.serviceDirectory.mkdir()) {
        state.updateError(true, s"Can't make directory ${state.serviceDirectory}")
        return false
      }

      state.beginUpdateToVersion(newVersion)

      log.info(s"Download version ${newVersion}")
      if (state.newServiceDirectory.exists() && !IoUtils.deleteFileRecursively(state.newServiceDirectory)) {
        state.updateError(true, s"Can't remove directory ${state.newServiceDirectory}")
        return false
      }
      if (!state.newServiceDirectory.mkdir()) {
        state.updateError(true, s"Can't make directory ${state.newServiceDirectory}")
        return false
      }
      if (!ZipUtils.receiveAndUnzip(file => syncDistributionClient.downloadClientVersionImage(
          service.name, newVersion, file), state.newServiceDirectory)) {
        state.updateError(false, s"Can't download ${service.name} version ${newVersion}")
        return false
      }

      log.info(s"Install service ${service}")
      var args = Map.empty[String, String]
      for (role <- service.role) {
        args += ("role" -> role)
        args += ("profile" -> role) // Backward compatibility
      }
      args += ("version" -> newVersion.original.toString)
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
        log.error(s"Install exception", e)
        false
    }
  }

  def finishInstall(newVersion: ClientDistributionVersion): Boolean = {
    try {
      log.info("Finish install")

      if (state.currentServiceDirectory.exists()) {
        for (serviceRunner <- serviceRunner) {
          log.info(s"Stop old version ${state.getVersion()}")
          if (serviceRunner.stopService()) {
            state.serviceStopped()
          } else {
            log.error(s"Can't stop service")
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

      log.info(s"Post install service")
      var args = Map.empty[String, String]
      for (role <- service.role) {
        args += ("profile" -> role)
      }
      args += ("version" -> newVersion.original.toString)
      args += ("PATH" -> System.getenv("PATH"))

      for (command <- installConfig.postInstallCommands.getOrElse(Seq.empty)) {
        if (!ProcessUtils.runProcess(command, args, state.currentServiceDirectory, ProcessUtils.Logging.Realtime)) {
          state.updateError(true, s"Install error")
          return false
        }
      }

      IoUtils.writeServiceVersion(state.currentServiceDirectory, service.name, newVersion)

      state.setVersion(newVersion)

      true
    } catch {
      case e: Exception =>
        log.error(s"Install exception", e)
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
          log.error("Can't start service because it is not installed")
          return false
        }

        val installConfig = InstallConfig.read(state.currentServiceDirectory).getOrElse {
          log.error(s"No install config in the build directory")
          return false
        }

        for (runService <- installConfig.runService) {
          log.info(s"Start service of version ${newVersion}")

          var parameters = Map.empty[String, String]
          for (role <- service.role) {
            parameters += ("profile" -> role)
          }
          parameters += ("version" -> newVersion.original.toString)

          val logUploader = if (runService.uploadLogs.getOrElse(false))
            Some(new LogUploader(service.name, None, instance, distributionClient)) else None

          val runner = new ServiceRunner(runService, parameters, instance, service, state, logUploader, faultUploader)
          if (!runner.startService()) {
            log.error(s"Can't start service")
            return false
          }
          serviceRunner = Some(runner)
        }

        state.serviceStarted()
      } else {
        log.error(s"Service is already started")
      }

      true
    } catch {
      case e: Exception =>
        log.error(s"Execute exception", e)
        false
    }
  }
}
