package com.vyulabs.update.builder

import java.io.File
import java.net.URI
import java.util.Date

import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.distribution.{AdminRepository, GitRepositoryUtils}
import com.vyulabs.update.builder.config.SourcesConfig
import com.vyulabs.update.utils.{IoUtils, ProcessUtils, Utils}
import com.vyulabs.update.common.Common.{DistributionName, ServiceName}
import com.vyulabs.update.common.Common
import com.vyulabs.update.config.UpdateConfig
import com.vyulabs.update.info.BuildInfo
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.IoUtils.copyFile
import com.vyulabs.update.version.DeveloperDistributionVersion
import org.eclipse.jgit.transport.RefSpec
import org.slf4j.Logger
import com.vyulabs.update.config.InstallConfig._
import com.vyulabs.update.distribution.client.DistributionInterface

class Builder(distribution: DistributionInterface, adminRepositoryUrl: URI)(implicit filesLocker: SmartFilesLocker) {
  private val builderLockFile = "builder.lock"

  def makeVersion(author: String, serviceName: ServiceName, comment: Option[String],
                  newVersion: Option[DeveloperDistributionVersion], sourceBranches: Seq[String])
                 (implicit log: Logger): Option[DeveloperDistributionVersion] = {
    val servicesDir = new File("services")
    val serviceDir = new File(servicesDir, serviceName)
    if (!serviceDir.exists() && !serviceDir.mkdirs()) {
      log.error(s"Can't create directory ${serviceDir}")
    }
    IoUtils.synchronize[Option[DeveloperDistributionVersion]](new File(serviceDir, builderLockFile), false,
      (attempt, _) => {
        if (attempt == 1) {
          log.info(s"Another builder creates version for ${serviceName} - wait ...")
        }
        Thread.sleep(5000)
        true
      },
      () => {
        val adminRepository = AdminRepository(adminRepositoryUrl, new File(serviceDir, "admin")).getOrElse {
          Utils.error("Init admin repository error")
        }
        val sourcesConfig = SourcesConfig.fromFile(adminRepository.getDirectory()).getOrElse {
          Utils.error("Can't get config of sources")
        }
        val sourceRepositoriesConf = sourcesConfig.sources.get(serviceName).getOrElse {
          Utils.error(s"Source repositories of service ${serviceName} is not specified.")
        }

        var generatedVersion = Option.empty[DeveloperDistributionVersion]
        try {
          val sourceDir = new File(serviceDir, "source")
          val buildDir = new File(serviceDir, "build")

          if (buildDir.exists() && !IoUtils.deleteFileRecursively(buildDir)) {
            log.error(s"Can't delete build directory ${buildDir}")
            return None
          }

          log.info("Get existing versions")
          val version = newVersion match {
            case Some(version) =>
              /* TODO graphql
              if (directory.isVersionExists(serviceName, version)) {
                log.error(s"Version ${version} already exists")
                return None
              }*/
              version
            case None =>
              /* TOFO graphql
              directory.downloadVersionsInfo(clientName, serviceName) match {
                case Some(versions) if (!versions.versions.isEmpty) =>
                  val lastVersion = versions.versions.sortBy(_.version)(BuildVersion.ordering).last
                  log.info(s"Last version is ${lastVersion}")
                  lastVersion.version.next()
                case _ =>
                  log.error("No existing versions")
                  BuildVersion(clientName, Seq(1, 0, 0))
              }
               */
              null
          }

          log.info(s"Generate version ${version}")

          log.info(s"Pull source repositories")
          var sourceRepositories = Seq.empty[GitRepository]
          var mainSourceRepository: GitRepository = null
          val sourceBranchIt = sourceBranches.iterator
          for (repositoryConf <- sourceRepositoriesConf) {
            val directory = repositoryConf.directory match {
              case Some(dir) =>
                new File(sourceDir, dir)
              case None =>
                sourceDir
            }
            val branch = if (sourceBranchIt.hasNext) {
              sourceBranchIt.next()
            } else {
              "master"
            }
            val sourceRepository =
              GitRepositoryUtils.getGitRepository(repositoryConf.url, branch, repositoryConf.cloneSubmodules.getOrElse(true), directory).getOrElse {
                log.error("Pull source repository error")
                return None
              }
            sourceRepositories :+= sourceRepository
            if (mainSourceRepository == null) {
              mainSourceRepository = sourceRepository
            }
          }

          log.info("Initialize update config")
          val servicesUpdateConfig = UpdateConfig.read(mainSourceRepository.getDirectory()).getOrElse {
            return None
          }
          val updateConfig = servicesUpdateConfig.services.getOrElse(serviceName, {
            log.error(s"Can't find update config for service ${serviceName}")
            return None
          })

          log.info("Execute build commands")
          var args = Map.empty[String, String]
          args += ("version" -> version.toString)
          args += ("PATH" -> System.getenv("PATH"))
          for (command <- updateConfig.build.buildCommands.getOrElse(Seq.empty)) {
            if (!ProcessUtils.runProcess(command, args, mainSourceRepository.getDirectory(), ProcessUtils.Logging.Realtime)) {
              return None
            }
          }

          log.info(s"Copy files to build directory ${buildDir}")
          for (copyCommand <- updateConfig.build.copyFiles) {
            val sourceFile = Utils.extendMacro(copyCommand.sourceFile, args)
            val in = if (sourceFile.startsWith("/")) {
              new File(sourceFile)
            } else {
              new File(mainSourceRepository.getDirectory(), sourceFile)
            }
            val out = new File(buildDir, Utils.extendMacro(copyCommand.destinationFile, args))
            val outDir = out.getParentFile
            if (outDir != null) {
              if (!outDir.exists() && !outDir.mkdirs()) {
                log.error(s"Can't make directory ${outDir}")
                return None
              }
            }
            if (!copyFile(in, out, file => !copyCommand.except.getOrElse(Set.empty).contains(in.toPath.relativize(file.toPath).toString),
                          copyCommand.settings.getOrElse(Map.empty))) {
              return None
            }
          }

          for (installConfig <- updateConfig.install) {
            log.info("Create install configuration file")
            val configFile = new File(buildDir, Common.InstallConfigFileName)
            if (configFile.exists()) {
              log.error(s"Build repository already contains file ${configFile}")
              return None
            }
            if (!IoUtils.writeJsonToFile(configFile, installConfig)) {
              return None
            }
          }

          log.info(s"Upload version ${version} to distribution directory")
          val buildVersionInfo = BuildInfo(author, sourceBranches, new Date(), comment)
          /* TODO graphql
          if (!directory.uploadVersion(serviceName, version, buildVersionInfo, buildDir)) {
            return None
          }*/

          log.info(s"Mark source repositories with version ${version}")
          for (repository <- sourceRepositories) {
            val tag = serviceName + "-" + version.toString
            if (!repository.setTag(tag, comment)) {
              return None
            }
            if (!repository.push(Seq(new RefSpec(tag)))) {
              return None
            }
          }

          log.info(s"Version ${version} is created successfully")
          generatedVersion = Some(version)
          generatedVersion
        } finally {
          /* TODO graphql
          adminRepository.processLogFile(!generatedVersion.isEmpty)
          if (!gitLock.unlock(DeveloperAdminRepository.makeEndOfBuildMessage(serviceName, generatedVersion))) {
            log.error("Can't unlock version generation")
          }
          adminRepository.tagServices(Seq(serviceName))
           */
        }
      }).flatten
  }

  def getDesiredVersions()(implicit log: Logger): Option[Map[ServiceName, DeveloperDistributionVersion]] = {
    // TODO graphql
    //directory.downloadDesiredVersions(clientName).map(DesiredVersions.toMap(_))
    null
  }

  def setDesiredVersions(servicesVersions: Map[ServiceName, Option[DeveloperDistributionVersion]])
                        (implicit log: Logger): Boolean = {
    log.info(s"Upload desired versions ${servicesVersions}")
    IoUtils.synchronize[Boolean](new File(".", builderLockFile), false,
      (attempt, _) => {
        if (attempt == 1) {
          log.info("Another builder is running - wait ...")
        }
        Thread.sleep(5000)
        true
      },
      () => {
        var completed = false
        try {
          // TODO graphql
/*
          var desiredVersionsMap = directory.downloadDesiredVersions(clientName).map(DesiredVersions.toMap(_)).getOrElse(Map.empty)
          servicesVersions.foreach {
            case (serviceName, Some(version)) =>
              desiredVersionsMap += (serviceName -> version)
            case (serviceName, None) =>
              desiredVersionsMap -= serviceName
          }
          if (!directory.uploadDesiredVersions(clientName, DesiredVersions.fromMap(desiredVersionsMap))) {
            log.error("Can't update desired versions")
            return false
          }
          log.info(s"Desired versions are successfully uploaded")
          completed = true
*/
        } finally {
          /* TODO graphql
          adminRepository.processLogFile(completed)
          if (!gitLock.unlock(AdminRepository.makeEndOfSettingDesiredVersionsMessage(completed))) {
            log.error("Can't unlock update of desired versions")
          }
          if (!servicesVersions.isEmpty) {
            adminRepository.tagServices(servicesVersions.map(_._1).toSeq)
          }
           */
        }
        completed
      }).getOrElse(false)
  }
}
