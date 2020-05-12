package com.vyulabs.update.common

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.03.19.
  * Copyright FanDate, Inc.
  */
object Common {
  type ServiceName = String
  type ServiceProfile = String
  type ClientName = String
  type UserName = String
  type InstanceId = String
  type ProcessId = String
  type UpdaterDirectory = String
  type InstallProfileName = String

  case class UpdaterInstanceId(instanceId: InstanceId, director: UpdaterDirectory)

  val Pm2DescFileName = "pm2_desc.json"

  val ScriptsServiceName = "scripts"
  val DistributionServiceName = "distribution"
  val BuilderServiceName = "builder"
  val InstallerServiceName = "installer"
  val UpdaterServiceName = "updater"

  val BuilderJarName = "builder-%s.jar"
  val InstallerJarName = "installer-%s.jar"
  val UpdaterJarName = "updater-%s.jar"

  val VersionMarkFile = "version"

  val UpdateConfigFileName = "update.json"
  val InstallConfigFileName = "install.json"

  val CommonProfile = "common"

  def isUpdateService(serviceName: ServiceName): Boolean = {
    serviceName == ScriptsServiceName ||
    serviceName == DistributionServiceName ||
    serviceName == BuilderServiceName ||
    serviceName == InstallerServiceName ||
    serviceName == UpdaterServiceName
  }
}
