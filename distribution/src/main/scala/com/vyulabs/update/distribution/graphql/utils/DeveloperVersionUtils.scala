package com.vyulabs.update.distribution.graphql.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId, TaskId, UserId}
import com.vyulabs.update.common.config.{DistributionConfig, SourceConfig}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.graphql.NotFoundException
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.task.TaskManager
import org.bson.BsonDocument
import org.slf4j.Logger

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}
import spray.json._

import java.io.IOException
import java.util.Date

trait DeveloperVersionUtils extends DistributionConsumersUtils with ServiceProfilesUtils
    with StateUtils with RunBuilderUtils with SprayJsonSupport {

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val config: DistributionConfig
  protected val taskManager: TaskManager

  protected implicit val executionContext: ExecutionContext

  private var versionsInProcess = Seq.empty[DeveloperVersionInProcessInfo]

  def buildDeveloperVersion(service: ServiceId, version: DeveloperVersion, author: UserId,
                            sources: Seq[SourceConfig], comment: String)(implicit log: Logger): TaskId = {
    synchronized {
      if (versionsInProcess.exists(_.service == service)) {
        throw new IOException(s"Build developer version of service ${service} is already in process")
      }
      val task = taskManager.create(s"Build developer version ${version} of service ${service}",
        (task, logger) => {
          implicit val log = logger
          val arguments = Seq("buildDeveloperVersion",
            s"distribution=${config.distribution}", s"service=${service}", s"version=${version.toString}", s"author=${author}",
            s"sources=${sources.toJson.compactPrint}", s"comment=${comment}")
          runBuilder(task, arguments)
        })
      versionsInProcess = versionsInProcess.filter(_.service != service) :+ DeveloperVersionInProcessInfo(service, version, author, sources, comment,
        task.task, new Date())
      task.future.andThen { case _ => synchronized {
        versionsInProcess = versionsInProcess.filter(_.service != service) } }
      task.task
    }
  }

  def getDeveloperVersionsInProcess(service: Option[ServiceId]): Seq[DeveloperVersionInProcessInfo] = {
    synchronized {
      versionsInProcess.filter(version => { service.isEmpty || version.service == service.get } )
    }
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
          .sortBy(_.buildInfo.date.getTime).map(_.version)
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
    val filters = Filters.and((serviceArg ++ distributionArg ++ versionArg).asJava)
    collections.Developer_Versions.find(filters)
  }

  def setDeveloperDesiredVersions(deltas: Seq[DeveloperDesiredVersionDelta])(implicit log: Logger): Future[Unit] = {
    log.info(s"Upload developer desired versions ${deltas}")
    collections.Developer_DesiredVersions.update(new BsonDocument(), { desiredVersions =>
      val desiredVersionsMap = DeveloperDesiredVersions.toMap(desiredVersions.map(_.versions).getOrElse(Seq.empty))
      val newVersions =
        deltas.foldLeft(desiredVersionsMap) {
          (map, entry) => entry.version match {
            case Some(version) =>
              map + (entry.service -> version)
            case None =>
              map - entry.service
          }}
      Some(DeveloperDesiredVersions(DeveloperDesiredVersions.fromMap(newVersions)))
    }).map(_ => ())
  }

  def getDeveloperDesiredVersions(services: Set[ServiceId])(implicit log: Logger): Future[Seq[DeveloperDesiredVersion]] = {
    for {
      profile <- collections.Developer_DesiredVersions.find(new BsonDocument()).map(_.map(_.versions).headOption.getOrElse(Seq.empty[DeveloperDesiredVersion])
        .filter(v => services.isEmpty || services.contains(v.service)))
    } yield profile
  }

  def getDeveloperDesiredVersion(service: ServiceId)(implicit log: Logger): Future[Option[DeveloperDistributionVersion]] = {
    getDeveloperDesiredVersions(Set(service)).map(_.headOption.map(_.version))
  }

  def filterDesiredVersionsByProfile(distribution: DistributionId, future: Future[Seq[DeveloperDesiredVersion]])(implicit log: Logger)
      : Future[Seq[DeveloperDesiredVersion]] = {
    for {
      desiredVersions <- future
      consumerConfig <- getDistributionConsumerInfo(distribution)
      servicesProfile <- getServicesProfile(consumerConfig.profile)
      versions <- Future(desiredVersions.filter(version => servicesProfile.services.contains(version.service)))
    } yield versions
  }

  def getDeveloperDesiredVersions(distribution: DistributionId, services: Set[ServiceId])(implicit log: Logger)
      : Future[Seq[DeveloperDesiredVersion]] = {
    for {
      distributionConsumerInfo <- getDistributionConsumerInfo(distribution)
      developerVersions <- distributionConsumerInfo.testConsumer match {
        case Some(testDistributionConsumer) =>
          for {
            testedVersions <- getTestedVersions(distributionConsumerInfo.profile).map(testedVersions => {
              testedVersions match {
                case Some(testedVersions) =>
                  val testCondition = testedVersions.signatures.exists(signature => signature.distribution == testDistributionConsumer)
                  if (testCondition) {
                    testedVersions.versions
                  } else {
                    throw NotFoundException(s"Desired versions for profile ${distributionConsumerInfo.profile} are not tested by clients ${testDistributionConsumer}")
                  }
                case None =>
                  throw NotFoundException(s"Desired versions for profile ${distributionConsumerInfo.profile} are not tested by anyone")
              }
            })
          } yield testedVersions
        case None =>
          getDeveloperDesiredVersions(services)
      }
    } yield developerVersions
  }

  private def getBusyVersions(distribution: DistributionId, service: ServiceId)(implicit log: Logger): Future[Set[DeveloperVersion]] = {
    for {
      desiredVersion <- getDeveloperDesiredVersion(service)
      clientsInfo <- getConsumersInfo()
      installedVersions <- Future.sequence(clientsInfo.map(client => getInstalledDesiredVersion(client.distribution, service))).map(
        _.flatten.map(_.version.original))
      testedVersions <- Future.sequence(clientsInfo.map(client => getTestedVersions(client.profile))).map(
        _.flatten.map(_.versions.find(_.service == service).map(_.version)).flatten)
    } yield {
      (desiredVersion.toSet ++ installedVersions ++ testedVersions).filter(_.distribution == distribution).map(_.developerVersion)
    }
  }
}