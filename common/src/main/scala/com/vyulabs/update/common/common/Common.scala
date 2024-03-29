package com.vyulabs.update.common.common

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.03.19.
  * Copyright FanDate, Inc.
  */
object Common {
  type DistributionId = String
  type ServiceId = String
  type AccountId = String
  type InstanceId = String
  type ProcessId = String
  type TaskId = String
  type TaskType = String
  type ServicesProfileId = String
  type ServiceDirectory = String
  type FaultId = String
  type ServiceRole = String

  val Pm2DescFileName = "pm2_desc.json"

  val AdminAccount = "admin"

  val ScriptsServiceName = "scripts"
  val DistributionServiceName = "distribution"
  val BuilderServiceName = "builder"
  val UpdaterServiceName = "updater"

  val BuilderJarName = "builder-%s.jar"
  val InstallerJarName = "installer-%s.jar"
  val UpdaterJarName = "updater-%s.jar"

  val ServiceZipName = ".%s.zip"

  val DesiredVersionMarkFile = ".%s.desired-version.json"
  val VersionMarkFile = ".%s.version.json"

  val DistributionConfigFileName = "distribution.json"
  val UpdateConfigFileName = "update.json"
  val InstallConfigFileName = "install.json"
  val UpdaterConfigFileName = "updater.json"

  val FaultInfoFileName = "fault.json"

  val ProfileFileNamePattern = "(profile-)(.*?)(.json)"
  val ProfileFileNameMatch = "profile-x.json"

  val CommonConsumerProfile = "common"
  val SelfConsumerProfile = "self"

  val BuilderSh = "builder.sh"
  val UpdateSh = ".update.sh"

  val AuthorBuilder = "builder"
  val AuthorDistribution = "distribution"

  def isUpdateService(service: ServiceId): Boolean = {
    service == ScriptsServiceName ||
    service == DistributionServiceName ||
    service == BuilderServiceName ||
    service == UpdaterServiceName
  }
}
