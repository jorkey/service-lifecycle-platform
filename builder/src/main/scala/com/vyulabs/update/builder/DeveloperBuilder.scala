package com.vyulabs.update.builder

import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}
import com.vyulabs.update.common.config.InstallConfig._
import com.vyulabs.update.common.config.{SourceConfig, UpdateConfig}
import com.vyulabs.update.common.distribution.client.graphql.BuilderGraphqlCoder.builderMutations
import com.vyulabs.update.common.distribution.client.{SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.distribution.server.InstallSettingsDirectory
import com.vyulabs.update.common.info.{BuildInfo, DeveloperVersionInfo}
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

class DeveloperBuilder(builderDir: File, distribution: DistributionId) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val builderLockFile = "builder.lock"

  makeDir(builderDir)

  private val developerDir = makeDir(new File(builderDir, "developer"))
  private val servicesDir = makeDir(new File(developerDir, "services"))

  private val settingsDirectory = new InstallSettingsDirectory(builderDir)

  def developerServiceDir(service: ServiceId) = makeDir(new File(servicesDir, service))
  def developerBuildDir(service: ServiceId) = makeDir(new File(developerServiceDir(service), "build"))
  def developerSourceDir(service: ServiceId) = makeDir(new File(developerServiceDir(service), "source"))

  def getBuildDirectory(service: ServiceId, sourceName: String): File = {
    new File(developerSourceDir(service), sourceName)
  }

  def buildDeveloperVersion(distributionClient: SyncDistributionClient[SyncSource],
                            author: String, service: ServiceId, newVersion: DeveloperVersion, comment: String,
                            sourcesConfig: Seq[SourceConfig])(implicit log: Logger, filesLocker: SmartFilesLocker): Boolean = {
    val newDistributionVersion = DeveloperDistributionVersion.from(distribution, newVersion)
    IoUtils.synchronize[Boolean](new File(developerServiceDir(service), builderLockFile), false,
      (attempt, _) => {
        if (attempt == 1) {
          log.info(s"Another builder creates version for ${service} - wait ...")
        }
        Thread.sleep(5000)
        true
      },
      () => {
        log.info(s"Prepare source directories of service ${service}")
        val sourceRepositories = prepareSourceDirectories(service, sourcesConfig).getOrElse {
          log.error(s"Can't pull source directories")
          return false
        }

        log.info(s"Generate version ${newDistributionVersion} of service ${service}")
        val arguments = Map("version" -> newDistributionVersion.toString)
        if (!generateDeveloperVersion(service, getBuildDirectory(service, sourcesConfig.head.name), arguments)) {
          log.error(s"Can't generate version")
          return false
        }

        log.info(s"Make version image ${newVersion} of service ${service}")
        val imageFile = makeDeveloperVersionImage(service).getOrElse {
          log.error("Can't make version image")
          return false
        }

        log.info(s"Upload version image ${newVersion} of service ${service} to distribution server")
        val buildInfo = BuildInfo(author, sourcesConfig, new Date(), comment)
        if (!uploadDeveloperVersion(distributionClient, service, newDistributionVersion, buildInfo, imageFile)) {
          log.error("Can't upload version image")
          return false
        }

        if (!markSourceRepositories(sourceRepositories, service, newDistributionVersion, comment)) {
          log.error("Can't mark source repositories with new version")
        }

        log.info(s"Version ${newVersion} is created successfully")
        true
      }).getOrElse(false)
  }

  def prepareSourceDirectories(service: ServiceId, sourcesConfig: Seq[SourceConfig]): Option[Seq[GitRepository]] = {
    var gitRepositories = Seq.empty[GitRepository]
    for (sourceConfig <- sourcesConfig) {
      val branch = sourceConfig.git.branch
      val sourceRepository =
        GitRepository.getGitRepository(sourceConfig.git.url, branch, sourceConfig.git.cloneSubmodules.getOrElse(true),
          getBuildDirectory(service, sourceConfig.name)).getOrElse {
          return None
        }
      gitRepositories :+= sourceRepository
    }
    Some(gitRepositories)
  }

  def generateDeveloperVersion(service: ServiceId, sourceDirectory: File, arguments: Map[String, String])
                              (implicit log: Logger): Boolean = {
    val directory = developerBuildDir(service)

    if (!IoUtils.deleteFileRecursively(directory)) {
      log.error(s"Can't delete build directory ${directory}")
      return false
    }

    log.info("Initialize update config")
    val servicesUpdateConfig = UpdateConfig.read(sourceDirectory).getOrElse {
      return false
    }
    val updateConfig = servicesUpdateConfig.services.getOrElse(service, {
      log.error(s"Can't find update config of service ${service}")
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

  def makeDeveloperVersionImage(service: ServiceId): Option[File] = {
    val directory = developerBuildDir(service)
    val file = Files.createTempFile(s"${service}-version", "zip").toFile
    file.deleteOnExit()
    if (ZipUtils.zip(file, directory)) {
      Some(file)
    } else {
      None
    }
  }

  def uploadDeveloperVersion(distributionClient: SyncDistributionClient[SyncSource],
                             service: ServiceId, version: DeveloperDistributionVersion, author: String): Boolean = {
    val buildInfo = BuildInfo(author, Seq.empty, new Date(), "Initial version")
    ZipUtils.zipAndSend(developerBuildDir(service), file => {
      uploadDeveloperVersion(distributionClient, service, version, buildInfo, file)
    })
  }

  def uploadDeveloperVersion(distributionClient: SyncDistributionClient[SyncSource], service: ServiceId,
                             version: DeveloperDistributionVersion, buildInfo: BuildInfo, imageFile: File): Boolean = {
    if (!distributionClient.uploadDeveloperVersionImage(service, version, imageFile)) {
      log.error("Uploading version image error")
      return false
    }
    if (!distributionClient.graphqlRequest(
        builderMutations.addDeveloperVersionInfo(DeveloperVersionInfo.from(service, version, buildInfo))).getOrElse(false)) {
      log.error("Adding version info error")
      return false
    }
    true
  }

  private def markSourceRepositories(sourceRepositories: Seq[GitRepository], service: ServiceId,
                                     version: DeveloperDistributionVersion, comment: String): Boolean = {
    for (repository <- sourceRepositories) {
      log.info(s"Mark source repository with version ${version}")
      val tag = service + "-" + version.toString
      repository.setTag(tag, Some(comment))
      if (!repository.push(Seq(new RefSpec(tag)))) {
        return false
      }
    }
    true
  }
}
