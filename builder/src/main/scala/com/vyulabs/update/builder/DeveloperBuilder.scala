package com.vyulabs.update.builder

import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.builder.config.{SourceConfig, SourcesConfig}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionName, ServiceName}
import com.vyulabs.update.common.config.InstallConfig._
import com.vyulabs.update.common.config.UpdateConfig
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.{administratorMutations, administratorQueries}
import com.vyulabs.update.common.distribution.client.{SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.distribution.server.SettingsDirectory
import com.vyulabs.update.common.info.{BuildInfo, DeveloperDesiredVersionDelta, DeveloperVersionInfo}
import com.vyulabs.update.common.lock.SmartFilesLocker
import com.vyulabs.update.common.process.ProcessUtils
import com.vyulabs.update.common.utils.IoUtils.copyFile
import com.vyulabs.update.common.utils.Utils.makeDir
import com.vyulabs.update.common.utils.{IoUtils, Utils, ZipUtils}
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import org.eclipse.jgit.transport.RefSpec
import org.slf4j.{Logger, LoggerFactory}

import java.io.File
import java.nio.file.Files
import java.util.Date

class DeveloperBuilder(builderDir: File, distributionName: DistributionName) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val builderLockFile = "builder.lock"

  private val developerDir = makeDir(new File(builderDir, "developer"))
  private val servicesDir = makeDir(new File(developerDir, "services"))

  private val settingsDirectory = new SettingsDirectory(builderDir, distributionName)

  def developerServiceDir(serviceName: ServiceName) = makeDir(new File(servicesDir, serviceName))
  def developerBuildDir(serviceName: ServiceName) = makeDir(new File(developerServiceDir(serviceName), "build"))
  def developerSourceDir(serviceName: ServiceName) = makeDir(new File(developerServiceDir(serviceName), "source"))

  def getBuildDirectory(serviceName: ServiceName, subDirectory: Option[String]): File = {
    subDirectory.map(subDirectory => new File(developerSourceDir(serviceName), subDirectory)).getOrElse {
      developerSourceDir(serviceName)
    }
  }

  def buildDeveloperVersion(distributionClient: SyncDistributionClient[SyncSource],
                            author: String, serviceName: ServiceName, newVersion: DeveloperVersion,
                            comment: Option[String], sourceBranches: Seq[String])
                           (implicit log: Logger, filesLocker: SmartFilesLocker): Boolean = {
    val newDistributionVersion = DeveloperDistributionVersion(distributionName, newVersion)
    IoUtils.synchronize[Boolean](new File(developerServiceDir(serviceName), builderLockFile), false,
      (attempt, _) => {
        if (attempt == 1) {
          log.info(s"Another builder creates version for ${serviceName} - wait ...")
        }
        Thread.sleep(5000)
        true
      },
      () => {
        log.info("Check for version exist")
        if (doesDeveloperVersionExist(distributionClient, serviceName, newDistributionVersion)) {
          log.error(s"Version ${newVersion} of service ${serviceName} already exists")
          return false
        }

        log.info(s"Prepare source directories of service ${serviceName}")
        val sourcesConfig = getSourcesConfig(serviceName)
        val sourceRepositories = prepareSourceDirectories(serviceName, sourceBranches, sourcesConfig).getOrElse {
          log.error(s"Can't pull source directories")
          return false
        }

        log.info(s"Generate version ${newVersion} of service ${serviceName}")
        val arguments = Map("version" -> newVersion.toString)
        if (!generateDeveloperVersion(serviceName, getBuildDirectory(serviceName, sourcesConfig.head.buildDirectory), arguments)) {
          log.error(s"Can't generate version")
          return false
        }

        log.info(s"Make version image ${newVersion} of service ${serviceName}")
        val imageFile = makeDeveloperVersionImage(serviceName).getOrElse {
          log.error("Can't make version image")
          return false
        }

        log.info(s"Upload version image ${newVersion} of service ${serviceName} to distribution server")
        val buildInfo = BuildInfo(author, sourceBranches, new Date(), comment)
        if (!uploadDeveloperVersion(distributionClient, serviceName, newDistributionVersion, buildInfo, imageFile)) {
          log.error("Can't upload version image")
          return false
        }

        if (!markSourceRepositories(sourceRepositories, serviceName, newDistributionVersion, comment)) {
          log.error("Can't mark source repositories with new version")
        }

        log.info(s"Version ${newVersion} is created successfully")
        true
      }).getOrElse(false)
  }

  def getSourcesConfig(serviceName: ServiceName): Seq[SourceConfig] = {
    val sourcesConfig = SourcesConfig.fromFile(settingsDirectory.getSourcesFile()).getOrElse {
      log.error("Can't get config of sources")
      return Seq.empty
    }
    sourcesConfig.sources.get(serviceName).getOrElse {
      log.error(s"Source repositories of service ${serviceName} is not specified.")
      return Seq.empty
    }
  }

  def prepareSourceDirectories(serviceName: ServiceName, sourceBranches: Seq[String],
                               sourcesConfig: Seq[SourceConfig]): Option[Seq[GitRepository]] = {
    var gitRepositories = Seq.empty[GitRepository]
    val sourceBranchIt = sourceBranches.iterator
    for (sourceConfig <- sourcesConfig) {
      sourceConfig.source match {
        case Left(directory) =>
          if (!IoUtils.copyFile(new File(directory), getBuildDirectory(serviceName, sourceConfig.buildDirectory))) {
            return None
          }
        case Right(git) =>
          val branch = if (sourceBranchIt.hasNext) {
            sourceBranchIt.next()
          } else {
            "master"
          }
          val sourceRepository =
            GitRepository.getGitRepository(git.url, branch, git.cloneSubmodules.getOrElse(true),
                getBuildDirectory(serviceName, sourceConfig.buildDirectory)).getOrElse {
              return None
            }
          gitRepositories :+= sourceRepository
      }
    }
    Some(gitRepositories)
  }

  def generateDeveloperVersion(serviceName: ServiceName, sourceDirectory: File, arguments: Map[String, String])
                              (implicit log: Logger): Boolean = {
    val directory = developerBuildDir(serviceName)

    if (!IoUtils.deleteFileRecursively(directory)) {
      log.error(s"Can't delete build directory ${directory}")
      return false
    }

    log.info("Initialize update config")
    val servicesUpdateConfig = UpdateConfig.read(sourceDirectory).getOrElse {
      return false
    }
    val updateConfig = servicesUpdateConfig.services.getOrElse(serviceName, {
      log.error(s"Can't find update config of service ${serviceName}")
      return false
    })

    log.info("Execute build commands")
    var args = arguments
    args += ("PATH" -> System.getenv("PATH"))
    for (command <- updateConfig.build.buildCommands.getOrElse(Seq.empty)) {
      if (!ProcessUtils.runProcess(command, args, sourceDirectory, ProcessUtils.Logging.Realtime)) {
        return false
      }
    }

    log.info(s"Copy files to build directory ${directory}")
    for (copyCommand <- updateConfig.build.copyFiles) {
      val sourceFile = Utils.extendMacro(copyCommand.sourceFile, args)
      val in = if (sourceFile.startsWith("/")) {
        new File(sourceFile)
      } else {
        new File(sourceDirectory, sourceFile)
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
          copyCommand.settings.getOrElse(Map.empty).mapValues(Utils.extendMacro(_, args)))) {
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

  def makeDeveloperVersionImage(serviceName: ServiceName): Option[File] = {
    val directory = developerBuildDir(serviceName)
    val file = Files.createTempFile(s"${serviceName}-version", "zip").toFile
    file.deleteOnExit()
    if (ZipUtils.zip(file, directory)) {
      Some(file)
    } else {
      None
    }
  }

  def uploadDeveloperVersion(distributionClient: SyncDistributionClient[SyncSource],
                             serviceName: ServiceName, version: DeveloperDistributionVersion, author: String): Boolean = {
    val buildInfo = BuildInfo(author, Seq.empty, new Date(), Some("Initial version"))
    ZipUtils.zipAndSend(developerBuildDir(serviceName), file => {
      uploadDeveloperVersion(distributionClient, serviceName, version, buildInfo, file)
    })
  }

  def uploadDeveloperVersion(distributionClient: SyncDistributionClient[SyncSource], serviceName: ServiceName,
                             version: DeveloperDistributionVersion, buildInfo: BuildInfo, imageFile: File): Boolean = {
    if (!distributionClient.uploadDeveloperVersionImage(serviceName, version, imageFile)) {
      log.error("Uploading version image error")
      return false
    }
    if (!distributionClient.graphqlRequest(
        administratorMutations.addDeveloperVersionInfo(DeveloperVersionInfo.from(serviceName, version, buildInfo))).getOrElse(false)) {
      log.error("Adding version info error")
      return false
    }
    true
  }

  def setDesiredVersions(distributionClient: SyncDistributionClient[SyncSource], versions: Seq[DeveloperDesiredVersionDelta]): Boolean = {
    distributionClient.graphqlRequest(administratorMutations.setDeveloperDesiredVersions(versions)).getOrElse(false)
  }

  private def doesDeveloperVersionExist(distributionClient: SyncDistributionClient[SyncSource], serviceName: ServiceName, version: DeveloperDistributionVersion): Boolean = {
    distributionClient.graphqlRequest(administratorQueries.getDeveloperVersionsInfo(serviceName, Some(version.distributionName), Some(version.version))).map(_.size != 0).getOrElse(false)
  }

  private def markSourceRepositories(sourceRepositories: Seq[GitRepository], serviceName: ServiceName,
                                      version: DeveloperDistributionVersion, comment: Option[String]): Boolean = {
    for (repository <- sourceRepositories) {
      log.info(s"Mark source repository with version ${version}")
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
}
