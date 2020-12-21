package com.vyulabs.update.builder

import java.io.File
import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.builder.config.SourcesConfig
import com.vyulabs.update.common.utils.{IoUtils, Utils, ZipUtils}
import com.vyulabs.update.common.common.Common.ServiceName
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.config.UpdateConfig
import com.vyulabs.update.common.info.{BuildInfo, DeveloperDesiredVersion, DeveloperVersionInfo}
import com.vyulabs.update.common.lock.SmartFilesLocker
import com.vyulabs.update.common.utils.IoUtils.copyFile
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import org.eclipse.jgit.transport.RefSpec
import org.slf4j.{Logger, LoggerFactory}
import com.vyulabs.update.common.config.InstallConfig._
import com.vyulabs.update.common.distribution.client.SyncDistributionClient
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.{administratorMutations, administratorQueries}
import com.vyulabs.update.common.process.ProcessUtils
import com.vyulabs.update.common.utils.Utils.makeDir
import com.vyulabs.update.distribution.{GitRepositoryUtils, SettingsDirectory}

import java.util.Date

object DeveloperBuilder {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val builderLockFile = "builder.lock"

  private val developerDir = makeDir(new File("developer"))
  private val servicesDir = makeDir(new File(developerDir, "services"))

  def developerServiceDir(serviceName: ServiceName) = makeDir(new File(servicesDir, serviceName))
  def developerBuildDir(serviceName: ServiceName) = makeDir(new File(developerServiceDir(serviceName), "build"))
  def developerSourceDir(serviceName: ServiceName) = makeDir(new File(developerServiceDir(serviceName), "source"))

  def buildDeveloperVersion(distribution: SyncDistributionClient, settingsDirectory: SettingsDirectory,
                            author: String, serviceName: ServiceName, newVersion: Option[DeveloperVersion],
                            comment: Option[String], sourceBranches: Seq[String])
                           (implicit log: Logger, filesLocker: SmartFilesLocker): Option[DeveloperDistributionVersion] = {
    IoUtils.synchronize[Option[DeveloperDistributionVersion]](new File(developerServiceDir(serviceName), builderLockFile), false,
      (attempt, _) => {
        if (attempt == 1) {
          log.info(s"Another builder creates version for ${serviceName} - wait ...")
        }
        Thread.sleep(5000)
        true
      },
      () => {
        val version = newVersion match {
          case Some(version) =>
            log.info("Check for version exist")
            val developerDistributionVersion = DeveloperDistributionVersion(distribution.distributionName, version)
            if (doesDeveloperVersionExist(distribution, serviceName, developerDistributionVersion)) {
              log.error(s"Version ${version} already exists")
              return None
            }
            developerDistributionVersion
          case None =>
            log.error(s"Generate new version number")
            generateNewVersionNumber(distribution, serviceName)
        }

        log.info(s"Pull source repositories")
        val sourceRepositories = pullSourceDirectories(settingsDirectory, serviceName, sourceBranches)
        if (sourceRepositories.isEmpty) {
          log.error(s"Can't pull source directories")
          return None
        }

        log.info(s"Generate version ${version}")
        val arguments = Map("version" -> version.toString)
        if (!generateDeveloperVersion(serviceName, sourceRepositories.map(_.getDirectory()), arguments)) {
          log.error(s"Can't generate version")
          return None
        }

        log.info(s"Upload version image ${version} to distribution server")
        val buildInfo = BuildInfo(author, sourceBranches, new Date(), comment)
        if (!ZipUtils.zipAndSend(developerBuildDir(serviceName), file => uploadDeveloperVersionImage(distribution, serviceName, version, buildInfo, file))) {
          log.error("Can't upload version image")
          return None
        }

        log.info(s"Mark source repositories with version ${version}")
        if (!markSourceRepositories(sourceRepositories, serviceName, version, comment)) {
          log.error("Can't mark source repositories with new version")
        }

        log.info(s"Version ${version} is created successfully")
        Some(version)
      }).flatten
  }

  def setDeveloperDesiredVersions(distributionClient: SyncDistributionClient,
                                  servicesVersions: Map[ServiceName, Option[DeveloperDistributionVersion]])
                                 (implicit log: Logger, filesLocker: SmartFilesLocker): Boolean = {
    log.info(s"Upload developer desired versions ${servicesVersions}")
    var desiredVersionsMap = getDeveloperDesiredVersions(distributionClient).getOrElse(Map.empty)
    servicesVersions.foreach {
      case (serviceName, Some(version)) =>
        desiredVersionsMap += (serviceName -> version)
      case (serviceName, None) =>
        desiredVersionsMap -= serviceName
    }
    val desiredVersionsList = desiredVersionsMap.foldLeft(Seq.empty[DeveloperDesiredVersion])(
      (list, entry) => list :+ DeveloperDesiredVersion(entry._1, entry._2)).sortBy(_.serviceName)
    if (!distributionClient.graphqlRequest(administratorMutations.setDeveloperDesiredVersions(desiredVersionsList)).getOrElse(false)) {
      log.error("Can't update developer desired versions")
      return false
    }
    log.info(s"Developer desired versions are successfully uploaded")
    true
  }

  def pullSourceDirectories(settingsDirectory: SettingsDirectory, serviceName: ServiceName, sourceBranches: Seq[String]): Seq[GitRepository] = {
    val sourcesConfig = SourcesConfig.fromFile(settingsDirectory.getSourcesFile()).getOrElse {
      log.error("Can't get config of sources")
      return Seq.empty
    }
    val sourceRepositoriesConf = sourcesConfig.sources.get(serviceName).getOrElse {
      log.error(s"Source repositories of service ${serviceName} is not specified.")
      return Seq.empty
    }

    var sourceRepositories = Seq.empty[GitRepository]
    val sourceBranchIt = sourceBranches.iterator
    for (repositoryConf <- sourceRepositoriesConf) {
      val directory = repositoryConf.directory match {
        case Some(dir) =>
          new File(developerSourceDir(serviceName), dir)
        case None =>
          developerSourceDir(serviceName)
      }
      val branch = if (sourceBranchIt.hasNext) {
        sourceBranchIt.next()
      } else {
        "master"
      }
      val sourceRepository =
        GitRepositoryUtils.getGitRepository(repositoryConf.url, branch, repositoryConf.cloneSubmodules.getOrElse(true), directory).getOrElse {
          return Seq.empty
        }
      sourceRepositories :+= sourceRepository
    }
    sourceRepositories
  }

  def generateDeveloperVersion(serviceName: ServiceName, sourceDirectories: Seq[File], arguments: Map[String, String])
                              (implicit log: Logger): Boolean = {
    val directory = developerBuildDir(serviceName)

    if (!IoUtils.deleteFileRecursively(directory)) {
      log.error(s"Can't delete build directory ${directory}")
      return false
    }

    val mainSourceDirectory = sourceDirectories.head

    log.info("Initialize update config")
    val servicesUpdateConfig = UpdateConfig.read(mainSourceDirectory).getOrElse {
      return false
    }
    val updateConfig = servicesUpdateConfig.services.getOrElse(serviceName, {
      log.error(s"Can't find update config for service ${serviceName}")
      return false
    })

    log.info("Execute build commands")
    var args = arguments
    args += ("PATH" -> System.getenv("PATH"))
    for (command <- updateConfig.build.buildCommands.getOrElse(Seq.empty)) {
      if (!ProcessUtils.runProcess(command, args, mainSourceDirectory, ProcessUtils.Logging.Realtime)) {
        return false
      }
    }

    log.info(s"Copy files to build directory ${directory}")
    for (copyCommand <- updateConfig.build.copyFiles) {
      val sourceFile = Utils.extendMacro(copyCommand.sourceFile, args)
      val in = if (sourceFile.startsWith("/")) {
        new File(sourceFile)
      } else {
        new File(mainSourceDirectory, sourceFile)
      }
      val out = new File(directory, Utils.extendMacro(copyCommand.destinationFile, args))
      val outDir = out.getParentFile
      if (outDir != null) {
        if (!outDir.exists() && !outDir.mkdirs()) {
          log.error(s"Can't make directory ${outDir}")
          return false
        }
      }
      if (!copyFile(in, out, file => !copyCommand.except.getOrElse(Set.empty).contains(in.toPath.relativize(file.toPath).toString),
        copyCommand.settings.getOrElse(Map.empty))) {
        return false
      }
    }

    for (installConfig <- updateConfig.install) {
      log.info("Create install configuration file")
      val configFile = new File(directory, Common.InstallConfigFileName)
      if (configFile.exists()) {
        log.error(s"Build repository already contains file ${configFile}")
        return false
      }
      if (!IoUtils.writeJsonToFile(configFile, installConfig)) {
        return false
      }
    }
    true
  }

  def doesDeveloperVersionExist(distributionClient: SyncDistributionClient, serviceName: ServiceName, version: DeveloperDistributionVersion): Boolean = {
    distributionClient.graphqlRequest(administratorQueries.getDeveloperVersionsInfo(serviceName, Some(distributionClient.distributionName), Some(version))).size != 0
  }

  private def generateNewVersionNumber(distributionClient: SyncDistributionClient, serviceName: ServiceName): DeveloperDistributionVersion = {
    log.info("Get existing versions")
    distributionClient.graphqlRequest(administratorQueries.getDeveloperVersionsInfo(serviceName, Some(distributionClient.distributionName))) match {
      case Some(versions) if !versions.isEmpty =>
        val lastVersion = versions.map(_.version).sorted(DeveloperDistributionVersion.ordering).last
        log.info(s"Last version is ${lastVersion}")
        lastVersion.next()
      case _ =>
        log.error("No existing versions")
        DeveloperDistributionVersion(distributionClient.distributionName, DeveloperVersion(Seq(1, 0, 0)))
    }
  }

  private def markSourceRepositories(sourceRepositories: Seq[GitRepository], serviceName: ServiceName,
                             version: DeveloperDistributionVersion, comment: Option[String]): Boolean = {
    for (repository <- sourceRepositories) {
      val tag = serviceName + "-" + version.toString
      if (!repository.setTag(tag, comment)) {
        return false
      }
      if (!repository.push(Seq(new RefSpec(tag)))) {
        return false
      }
    }
    true
  }

 def getDeveloperDesiredVersions(distributionClient: SyncDistributionClient): Option[Map[ServiceName, DeveloperDistributionVersion]] = {
    distributionClient.graphqlRequest(administratorQueries.getDeveloperDesiredVersions())
      .map(_.foldLeft(Map.empty[ServiceName, DeveloperDistributionVersion])((map, version) => { map + (version.serviceName -> version.version) }))
  }

  def uploadDeveloperVersionImage(distributionClient: SyncDistributionClient, serviceName: ServiceName,
                                  version: DeveloperDistributionVersion, buildInfo: BuildInfo, imageFile: File): Boolean = {
    if (!distributionClient.uploadDeveloperVersionImage(serviceName, version, imageFile)) {
      log.error("Uploading version image error")
      return false
    }
    if (!distributionClient.graphqlRequest(
      administratorMutations.addDeveloperVersionInfo(DeveloperVersionInfo(serviceName, version, buildInfo))).getOrElse(false)) {
      log.error("Adding version info error")
      return false
    }
    true
  }
}
