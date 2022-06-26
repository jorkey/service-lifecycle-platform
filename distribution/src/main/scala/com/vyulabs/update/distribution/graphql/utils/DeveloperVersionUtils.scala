package com.vyulabs.update.distribution.graphql.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import org.bson.BsonDocument
import org.slf4j.Logger

import java.io.IOException
import java.util.Date
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait DeveloperVersionUtils extends ClientVersionUtils with SprayJsonSupport {

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val config: DistributionConfig

  protected val clientVersionUtils: ClientVersionUtils
  protected val serviceProfilesUtils: ServiceProfilesUtils
  protected val tasksUtils: TasksUtils
  protected val runBuilderUtils: RunBuilderUtils
  protected val buildStateUtils: BuildStateUtils
  protected val logUtils: LogUtils

  protected implicit val executionContext: ExecutionContext

  def buildDeveloperVersion(service: ServiceId, version: DeveloperVersion, author: AccountId,
                            comment: String, buildClientVersion: Boolean)
                           (implicit log: Logger): Future[TaskId] = {
    tasksUtils.createTask(
      "BuildDeveloperVersion",
      Seq(TaskParameter("service", service),
          TaskParameter("version", version.toString),
          TaskParameter("author", author),
          TaskParameter("comment", comment),
          TaskParameter("buildClientVersion", buildClientVersion.toString)
      ),
      Seq(service),
      (task, logger) => {
        implicit val log = logger
        @volatile var cancel = Option.empty[() => Unit]
        val arguments = Seq("buildDeveloperVersion",
          s"distribution=${config.distribution}", s"service=${service}", s"version=${version.toString}",
          s"author=${author}", s"comment=${comment}")
        val (builderFuture, cancelBuilder) =
          runBuilderUtils.runDeveloperBuilder(task, service, arguments)
        cancel = cancelBuilder
        val developerState = BuildServiceState(service,  BuildTarget.DeveloperVersion,
          author, version.toString, comment, task, BuildStatus.InProcess)
        val clientState = BuildServiceState(service,  BuildTarget.ClientVersion,
          author, ClientDistributionVersion.from(config.distribution, version, 0).toString, "", task, BuildStatus.InProcess)
        val future = buildStateUtils.setBuildState(developerState)
          .flatMap(_ => builderFuture)
          .flatMap(_ => setDeveloperDesiredVersions(Seq(DeveloperDesiredVersionDelta(service,
            Some(DeveloperDistributionVersion(config.distribution, version.build)))), author))
          .andThen {
            case Success(_) =>
              (if (buildClientVersion) {
                buildStateUtils.setBuildState(clientState)
              } else  {
                Future()
              }).flatMap { _ =>
                buildStateUtils.setBuildState(developerState.copy(status = BuildStatus.Success))
              }
            case Failure(_) =>
              buildStateUtils.setBuildState(developerState.copy(status = BuildStatus.Failure))
          }
          .flatMap { _ =>
            cancel = None
            if (buildClientVersion) {
              val (future, newCancel) = clientVersionUtils.buildClientVersion(task, service,
                ClientDistributionVersion(config.distribution, version.build, 0), author)
              cancel = newCancel
              future
                .flatMap(_ => buildStateUtils.setBuildState(clientState))
                .flatMap(_ => setClientDesiredVersions(Seq(ClientDesiredVersionDelta(service,
                  Some(ClientDistributionVersion(config.distribution, version.build, 0)))), author))
                .andThen {
                  case Success(_) =>
                    buildStateUtils.setBuildState(clientState.copy(status = BuildStatus.Success))
                  case Failure(_) =>
                    buildStateUtils.setBuildState(clientState.copy(status = BuildStatus.Failure))
                }
            } else {
              Future()
            }
          }
        (future, Some(() => cancel.foreach(_.apply())))
      }).map(_.taskId)
  }

  def addDeveloperVersionInfo(versionInfo: DeveloperVersionInfo)(implicit log: Logger): Future[Unit] = {
    log.info(s"Add developer version info ${versionInfo}")
    for {
      _ <- collections.Developer_Versions.insert(versionInfo)
      _ <- removeObsoleteVersions(versionInfo.version.distribution, versionInfo.service)
    } yield {}
  }

  private def removeObsoleteVersions(distribution: DistributionId, service: ServiceId)(implicit log: Logger): Future[Unit] = {
    for {
      versions <- getDeveloperVersionsInfo(service = Some(service), distribution = Some(distribution))
      busyVersions <- getBusyVersions(distribution, service)
      complete <- {
        val notUsedVersions = versions.filterNot(info => busyVersions.contains(info.version.developerVersion))
          .sortBy(_.buildInfo.time.getTime).map(_.version)
        if (notUsedVersions.size > config.versions.maxHistorySize) {
          Future.sequence(notUsedVersions.take(notUsedVersions.size - config.versions.maxHistorySize).map { version =>
            removeDeveloperVersion(service, version)
          })
        } else {
          Future()
        }
      }
    } yield {}
  }

  def removeDeveloperVersion(service: ServiceId, version: DeveloperDistributionVersion)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove developer version ${version} of service ${service}")
    val filters = Filters.and(
      Filters.eq("service", service),
      Filters.eq("version", version))
    directory.getDeveloperVersionImageFile(service, version).delete()
    for {
      profile <- {
        collections.Developer_Versions.delete(filters).map(_ > 0)
      }
    } yield profile
  }

  def getDeveloperVersionsInfo(service: Option[ServiceId] = None, distribution: Option[DistributionId] = None,
                               version: Option[DeveloperVersion] = None)(implicit log: Logger): Future[Seq[DeveloperVersionInfo]] = {
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val distributionArg = distribution.map { distribution => Filters.eq("version.distribution", distribution ) }
    val versionArg = version.map { version => Filters.eq("version.build", version.build) }
    val args = serviceArg ++ distributionArg ++ versionArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Developer_Versions.find(filters)
  }

  def setDeveloperDesiredVersions(deltas: Seq[DeveloperDesiredVersionDelta],
                                  author: AccountId)(implicit log: Logger): Future[Unit] = {
    if (!deltas.isEmpty) {
      log.info(s"Set developer desired versions ${deltas}")
      collections.Developer_DesiredVersions.update(new BsonDocument(), { desiredVersions =>
        val desiredVersionsMap = DeveloperDesiredVersions.toMap(desiredVersions.map(_.versions).getOrElse(Seq.empty))
        val newVersions =
          deltas.foldLeft(desiredVersionsMap) {
            (map, entry) =>
              entry.version match {
                case Some(version) =>
                  map + (entry.service -> version)
                case None =>
                  map - entry.service
              }
          }
        Some(DeveloperDesiredVersions(author, DeveloperDesiredVersions.fromMap(newVersions)))
      }).map(_ => ())
    } else {
      Future()
    }
  }

  def getDeveloperDesiredVersions(services: Set[ServiceId] = Set.empty)
                                 (implicit log: Logger): Future[Seq[DeveloperDesiredVersion]] = {
    for {
      versions <- collections.Developer_DesiredVersions.find(new BsonDocument()).map(_.map(_.versions).headOption.getOrElse(Seq.empty[DeveloperDesiredVersion])
        .filter(v => services.isEmpty || services.contains(v.service)))
    } yield versions
  }

  def getDeveloperDesiredVersionsHistory(limit: Int)
                                        (implicit log: Logger): Future[Seq[TimedDeveloperDesiredVersions]] = {
    for {
      history <- collections.Developer_DesiredVersions.history(new BsonDocument(), Some(limit))
        .map(_.map(v => TimedDeveloperDesiredVersions(v.modifyTime.getOrElse(throw new IOException("No modifyTime in document")),
          v.document.author, v.document.versions)))
    } yield history
  }

  def getDeveloperDesiredVersion(service: ServiceId)
                                (implicit log: Logger): Future[Option[DeveloperDistributionVersion]] = {
    getDeveloperDesiredVersions(Set(service)).map(_.headOption.map(_.version))
  }

  def getDeveloperDesiredVersionsByConsumer(profile: ServicesProfileId,
                                            testConsumer: Option[String], services: Set[ServiceId])(implicit log: Logger)
      : Future[Seq[DeveloperDesiredVersion]] = {
    (testConsumer match {
      case Some(testDistributionConsumer) =>
        getTestedVersions(Some(testDistributionConsumer))
      case None =>
        getDeveloperDesiredVersions(services)
    }).flatMap(versions => filterDesiredVersionsByProfile(profile, versions))
  }

  private def filterDesiredVersionsByProfile(profile: ServicesProfileId,
                                             desiredVersions: Seq[DeveloperDesiredVersion])(implicit log: Logger)
      : Future[Seq[DeveloperDesiredVersion]] = {
    for {
      profile <- serviceProfilesUtils.getServicesProfile(profile)
    } yield {
      desiredVersions.filter(version => profile.services.contains(version.service))
    }
  }

  def setTestedVersions(consumerDistribution: DistributionId, profile: ServicesProfileId,
                        desiredVersions: Seq[DeveloperDesiredVersion])(implicit log: Logger): Future[Unit] = {
    for {
      result <- {
        val newTestedVersions = TestedVersions(profile, consumerDistribution, desiredVersions, new Date())
        val distributionArg = Filters.eq("consumerDistribution", consumerDistribution)
        collections.Developer_TestedVersions.update(distributionArg, _ =>
          Some(newTestedVersions)).map(_ => ())
      }
    } yield result
  }

  def getTestedVersions(consumerDistribution: Option[DistributionId])
                       (implicit log: Logger): Future[Seq[DeveloperDesiredVersion]] = {
    val distributionArg = consumerDistribution.map(Filters.eq("consumerDistribution", _))
    val filters = distributionArg.getOrElse(new BsonDocument())
    collections.Developer_TestedVersions.find(filters).map(_.headOption.map(_.versions).getOrElse(Seq.empty))
  }

  private def getBusyVersions(distribution: DistributionId, service: ServiceId)(implicit log: Logger): Future[Set[DeveloperVersion]] = {
    for {
      desiredVersion <- getDeveloperDesiredVersion(service)
      profiles <- serviceProfilesUtils.getServiceProfiles(None)
      testedVersions <- Future.sequence(profiles.map(profile =>
          getTestedVersions(Some(profile.profile)))).map(
        _.flatten.find(_.service == service).map(_.version))
    } yield {
      (desiredVersion.toSet ++ testedVersions).filter(_.distribution == distribution).map(_.developerVersion)
    }
  }

  def getLastCommitComment(service: ServiceId)(implicit log: Logger): Future[TaskId] = {
    tasksUtils.createTask(
      "GetLastCommitComment",
      Seq(TaskParameter("service", service)),
      Seq(service),
      (taskId, logger) => {
        implicit val log = logger
        val arguments = Seq("lastCommitComment", s"distribution=${config.distribution}", s"service=${service}")
        runBuilderUtils.runDeveloperBuilder(taskId, service, arguments)
      }).map(_.taskId)
  }
}