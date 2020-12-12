package com.vyulabs.update.builder

import java.io.File
import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.distribution.GitRepositoryUtils
import com.vyulabs.update.builder.config.SourcesConfig
import com.vyulabs.update.utils.{IoUtils, ProcessUtils, Utils, ZipUtils}
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.common.Common
import com.vyulabs.update.config.UpdateConfig
import com.vyulabs.update.info.{BuildInfo, DeveloperDesiredVersion, DeveloperVersionInfo}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.IoUtils.copyFile
import com.vyulabs.update.version.{DeveloperDistributionVersion, DeveloperVersion}
import org.eclipse.jgit.transport.RefSpec
import org.slf4j.{Logger, LoggerFactory}
import com.vyulabs.update.config.InstallConfig._
import com.vyulabs.update.distribution.client.SyncDistributionClient
import com.vyulabs.update.distribution.client.graphql.AdministratorGraphqlCoder._
import com.vyulabs.update.utils.Utils.makeDir

import java.util.Date

object DeveloperBuilder {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val builderLockFile = "builder.lock"

  private val developerDir = makeDir(new File("developer"))
  private val servicesDir = makeDir(new File(developerDir, "services"))

  private def serviceDir(serviceName: ServiceName) = makeDir(new File(servicesDir, serviceName))
  private def buildDir(serviceName: ServiceName) = makeDir(new File(serviceDir(serviceName), "build"))
  private def sourceDir(serviceName: ServiceName) = makeDir(new File(serviceDir(serviceName), "source"))

  def buildDeveloperVersion(distributionClient: SyncDistributionClient, adminDirectory: File,
                            author: String, serviceName: ServiceName, comment: Option[String],
                            newVersion: Option[DeveloperVersion], sourceBranches: Seq[String])
                           (implicit log: Logger, filesLocker: SmartFilesLocker): Option[DeveloperVersion] = {
    IoUtils.synchronize[Option[DeveloperVersion]](new File(serviceDir(serviceName), builderLockFile), false,
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
            if (doesVersionExist(distributionClient, serviceName, version)) {
              log.error(s"Version ${version} already exists")
              return None
            }
            version
          case None =>
            log.error(s"Generate new version number")
            generateNewVersionNumber(distributionClient, serviceName)
        }

        log.info(s"Pull source repositories")
        val sourceRepositories = pullSourceDirectories(adminDirectory, serviceName, sourceBranches)
        if (sourceRepositories.isEmpty) {
          log.error(s"Can't pull source directories")
          return None
        }

        log.info(s"Generate version ${version}")
        if (!generateDeveloperVersion(serviceName, version, sourceRepositories.map(_.getDirectory()))) {
          log.error(s"Can't generate version")
          return None
        }

        log.info(s"Upload version image ${version} to distribution server")
        val buildInfo = BuildInfo(author, sourceBranches, new Date(), comment)
        if (!ZipUtils.zipAndSend(buildDir(serviceName), file => uploadVersionImage(distributionClient, serviceName, version, buildInfo, file))) {
          log.error("Can't upload version image")
          return None
        }

        log.info(s"Mark source repositories with version ${version}")
        if (!markSourceRepositories(sourceRepositories, serviceName,
            DeveloperDistributionVersion(distributionClient.distributionName, version), comment)) {
          log.error("Can't mark source repositories with new version")
        }

        log.info(s"Version ${version} is created successfully")
        Some(version)
      }).flatten
  }

  def doesVersionExist(distributionClient: SyncDistributionClient, serviceName: ServiceName, version: DeveloperVersion): Boolean = {
    if (distributionClient.graphqlRequest(administratorQueries.getDeveloperVersionsInfo(serviceName, Some(distributionClient.distributionName),
        Some(DeveloperDistributionVersion(distributionClient.distributionName, version)))).size != 0) {
      return true
    }
    false
  }

  def generateNewVersionNumber(distributionClient: SyncDistributionClient, serviceName: ServiceName): DeveloperVersion = {
    log.info("Get existing versions")
    distributionClient.graphqlRequest(administratorQueries.getDeveloperVersionsInfo(serviceName, Some(distributionClient.distributionName))) match {
      case Some(versions) if !versions.isEmpty =>
        val lastVersion = versions.map(_.version.version).sorted(DeveloperVersion.ordering).last
        log.info(s"Last version is ${lastVersion}")
        lastVersion.next()
      case _ =>
        log.error("No existing versions")
        DeveloperVersion(Seq(1, 0, 0))
    }
  }

  def generateDeveloperVersion(serviceName: ServiceName, version: DeveloperVersion, sourceDirectories: Seq[File])
                              (implicit log: Logger): Boolean = {
    val directory = buildDir(serviceName)

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
    var args = Map.empty[String, String]
    args += ("version" -> version.toString)
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

  def pullSourceDirectories(adminDirectory: File, serviceName: ServiceName, sourceBranches: Seq[String]): Seq[GitRepository] = {
    val sourcesConfig = SourcesConfig.fromFile(adminDirectory).getOrElse {
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
          new File(sourceDir(serviceName), dir)
        case None =>
          sourceDir(serviceName)
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

  def markSourceRepositories(sourceRepositories: Seq[GitRepository], serviceName: ServiceName,
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

  def getDesiredVersions(distributionClient: SyncDistributionClient): Option[Map[ServiceName, DeveloperDistributionVersion]] = {
    distributionClient.graphqlRequest(administratorQueries.getDeveloperDesiredVersions())
      .map(_.foldLeft(Map.empty[ServiceName, DeveloperDistributionVersion])((map, version) => { map + (version.serviceName -> version.version) }))
  }

  def setDesiredVersions(distributionClient: SyncDistributionClient,
                         servicesVersions: Map[ServiceName, Option[DeveloperDistributionVersion]])
                        (implicit log: Logger, filesLocker: SmartFilesLocker): Boolean = {
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
        var desiredVersionsMap = getDesiredVersions(distributionClient).getOrElse(Map.empty)
        servicesVersions.foreach {
          case (serviceName, Some(version)) =>
            desiredVersionsMap += (serviceName -> version)
          case (serviceName, None) =>
            desiredVersionsMap -= serviceName
        }
        val desiredVersionsList = desiredVersionsMap.foldLeft(Seq.empty[DeveloperDesiredVersion])(
          (list, entry) => list :+ DeveloperDesiredVersion(entry._1, entry._2)).sortBy(_.serviceName)
        if (!distributionClient.graphqlRequest(administratorMutations.setDeveloperDesiredVersions(desiredVersionsList)).getOrElse(false)) {
          log.error("Can't update desired versions")
        }
        log.info(s"Desired versions are successfully uploaded")
        true
      }).getOrElse(false)
  }

  def uploadVersionImage(distributionClient: SyncDistributionClient, serviceName: ServiceName,
                         version: DeveloperVersion, buildInfo: BuildInfo, imageFile: File): Boolean = {
    val developerDistributionVersion = DeveloperDistributionVersion(distributionClient.distributionName, version)
    if (!distributionClient.uploadDeveloperVersionImage(serviceName, developerDistributionVersion, imageFile)) {
      log.error("Uploading version image error")
      return false
    }
    if (!distributionClient.graphqlRequest(
      administratorMutations.addDeveloperVersionInfo(DeveloperVersionInfo(serviceName, developerDistributionVersion, buildInfo))).getOrElse(false)) {
      log.error("Adding version info error")
      return false
    }
    true
  }
}
