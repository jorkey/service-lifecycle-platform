package com.vyulabs.update.distribution.graphql.utils

import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId, TaskId}
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.{ClientDesiredVersion, ClientDesiredVersionDelta, ClientDesiredVersions, ClientVersionInfo}
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion}
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.task.TaskManager
import org.bson.BsonDocument
import org.slf4j.Logger

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait ClientVersionUtils extends DeveloperVersionUtils with DistributionConsumersUtils with RunBuilderUtils {
  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val config: DistributionConfig
  protected val taskManager: TaskManager

  protected implicit val executionContext: ExecutionContext

  /*
  def buildClientVersions(services: Seq[ServiceName], author: String)(implicit log: Logger): TaskId = {
    taskManager.create(s"Build client versions of services ${services} by ${author}", (task, logger) => {
      implicit val log = logger
      @volatile var cancels = Seq.empty[() => Unit]
      val future = for {
        developerDesiredVersions <- getDeveloperDesiredVersions(services.toSet).map(DeveloperDesiredVersions.toMap(_))
        clientDesiredVersions <- {
          Future.sequence(developerDesiredVersions.map {
            case (service, developerVersion) =>
              for {
                existingVersions <- getClientVersionsInfo(service).map(_.map(_.version)
                  .filter(_.original() == developerVersion))
                result <- {
                  val clientVersion =
                    if (!existingVersions.isEmpty) {
                      existingVersions.sorted(ClientDistributionVersion.ordering).last.next()
                    } else {
                      ClientDistributionVersion(config.distribution, ClientVersion(developerVersion.version))
                    }
                  val task = buildClientVersion(service, developerVersion, clientVersion, author)
                  cancels ++= task.cancel
                  task.future.map(_ => (service -> clientVersion))
                }
              } yield result
          }).map(_.foldLeft(Map.empty[ServiceName, ClientDistributionVersion])((map, entry) => map + entry))
        }
        result <- setClientDesiredVersions(ClientDesiredVersions.fromMap(clientDesiredVersions))
      } yield result
      (future, Some(() => cancels.foreach { _() }))
    }).task
  }*/

  def buildClientVersion(service: ServiceId,
                         developerVersion: DeveloperDistributionVersion, clientVersion: ClientDistributionVersion, author: String)
                        (implicit log: Logger): TaskId = {
    val task = taskManager.create(s"Build client version ${developerVersion} of service ${service}",
      (task, logger) => {
        implicit val log = logger
        val arguments = Seq("buildClientVersion",
          s"distribution=${config.distribution}", s"service=${service}",
          s"developerVersion=${developerVersion.toString}", s"clientVersion=${clientVersion.toString}",
          s"author=${author}")
        runBuilder(task, arguments)
      })
    task.task
  }

  def addClientVersionInfo(versionInfo: ClientVersionInfo)(implicit log: Logger): Future[Unit] = {
    log.info(s"Add client version info ${versionInfo}")
    for {
      result <- collections.Client_Versions.insert(versionInfo).map(_ => ())
      _ <- removeObsoleteVersions(versionInfo.version.distribution, versionInfo.service)
    } yield result
  }

  def getClientVersionsInfo(service: Option[ServiceId], distribution: Option[DistributionId] = None,
                            version: Option[ClientVersion] = None)(implicit log: Logger): Future[Seq[ClientVersionInfo]] = {
    val serviceArg = service.map { service => Filters.eq("service", service) }
    val distributionArg = distribution.map { distribution => Filters.eq("version.distribution", distribution ) }
    val versionArg = version.map { version => Filters.and(
      Filters.eq("version.developerBuild", version.developerBuild),
      Filters.eq("version.clientBuild", version.clientBuild)) }
    val filters = Filters.and((serviceArg ++ distributionArg ++ versionArg).asJava)
    collections.Client_Versions.find(filters)
  }

  private def removeObsoleteVersions(distribution: DistributionId, service: ServiceId)(implicit log: Logger): Future[Unit] = {
    for {
      versions <- getClientVersionsInfo(Some(service), distribution = Some(distribution))
      busyVersions <- getBusyVersions(distribution, service)
      _ <- {
        val notUsedVersions = versions.filterNot(info => busyVersions.contains(info.version.clientVersion))
          .sortBy(_.buildInfo.time.getTime).map(_.version)
        if (notUsedVersions.size > config.versions.maxHistorySize) {
          Future.sequence(notUsedVersions.take(notUsedVersions.size - config.versions.maxHistorySize).map { version =>
            removeClientVersion(service, version)
          })
        } else {
          Future()
        }
      }
    } yield {}
  }

  def removeClientVersion(service: ServiceId, version: ClientDistributionVersion)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove client version ${version} of service ${service}")
    val filters = Filters.and(
      Filters.eq("service", service),
      Filters.eq("version", version))
    directory.getClientVersionImageFile(service, version).delete()
    collections.Client_Versions.delete(filters).map(_ > 0)
  }

  def setClientDesiredVersions(deltas: Seq[ClientDesiredVersionDelta])(implicit log: Logger): Future[Unit] = {
    log.info(s"Upload client desired versions ${deltas}")
    collections.Client_DesiredVersions.update(new BsonDocument(), { desiredVersions =>
      val desiredVersionsMap = ClientDesiredVersions.toMap(desiredVersions.map(_.versions).getOrElse(Seq.empty))
      val newVersions =
        deltas.foldLeft(desiredVersionsMap) {
          (map, entry) => entry.version match {
            case Some(version) =>
              map + (entry.service -> version)
            case None =>
              map - entry.service
          }}
      Some(ClientDesiredVersions(ClientDesiredVersions.fromMap(newVersions)))
    }).map(_ => ())
  }

  def getClientDesiredVersions(services: Set[ServiceId] = Set.empty)
                              (implicit log: Logger): Future[Seq[ClientDesiredVersion]] = {
    collections.Client_DesiredVersions.find(new BsonDocument()).map(_.map(_.versions).headOption.getOrElse(Seq.empty[ClientDesiredVersion])
      .filter(v => services.isEmpty || services.contains(v.service)).sortBy(_.service))
  }

  def getClientDesiredVersion(service: ServiceId)(implicit log: Logger): Future[Option[ClientDistributionVersion]] = {
    getClientDesiredVersions(Set(service)).map(_.headOption.map(_.version))
  }

  def getDistributionClientDesiredVersions(distribution: DistributionId)(implicit log: Logger): Future[Seq[ClientDesiredVersion]] = {
    val filters = Filters.eq("distribution", distribution)
    collections.Client_DesiredVersions.find(filters).map(_.map(_.versions).headOption.getOrElse(Seq.empty[ClientDesiredVersion]))
  }

  private def getBusyVersions(distribution: DistributionId, service: ServiceId)(implicit log: Logger): Future[Set[ClientVersion]] = {
    getClientDesiredVersion(service).map(_.toSet.filter(_.distribution == distribution).map(_.clientVersion))
  }
}
