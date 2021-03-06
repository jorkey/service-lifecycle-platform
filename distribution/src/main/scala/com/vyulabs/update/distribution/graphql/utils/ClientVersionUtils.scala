package com.vyulabs.update.distribution.graphql.utils

import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionName, ServiceName, TaskId}
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

trait ClientVersionUtils extends DeveloperVersionUtils with DistributionClientsUtils with RunBuilderUtils {
  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val config: DistributionConfig
  protected val taskManager: TaskManager

  protected implicit val executionContext: ExecutionContext

  /*
  def buildClientVersions(serviceNames: Seq[ServiceName], author: String)(implicit log: Logger): TaskId = {
    taskManager.create(s"Build client versions for services ${serviceNames} by ${author}", (taskId, logger) => {
      implicit val log = logger
      @volatile var cancels = Seq.empty[() => Unit]
      val future = for {
        developerDesiredVersions <- getDeveloperDesiredVersions(serviceNames.toSet).map(DeveloperDesiredVersions.toMap(_))
        clientDesiredVersions <- {
          Future.sequence(developerDesiredVersions.map {
            case (serviceName, developerVersion) =>
              for {
                existingVersions <- getClientVersionsInfo(serviceName).map(_.map(_.version)
                  .filter(_.original() == developerVersion))
                result <- {
                  val clientVersion =
                    if (!existingVersions.isEmpty) {
                      existingVersions.sorted(ClientDistributionVersion.ordering).last.next()
                    } else {
                      ClientDistributionVersion(config.distributionName, ClientVersion(developerVersion.version))
                    }
                  val task = buildClientVersion(serviceName, developerVersion, clientVersion, author)
                  cancels ++= task.cancel
                  task.future.map(_ => (serviceName -> clientVersion))
                }
              } yield result
          }).map(_.foldLeft(Map.empty[ServiceName, ClientDistributionVersion])((map, entry) => map + entry))
        }
        result <- setClientDesiredVersions(ClientDesiredVersions.fromMap(clientDesiredVersions))
      } yield result
      (future, Some(() => cancels.foreach { _() }))
    }).taskId
  }*/

  def buildClientVersion(serviceName: ServiceName,
                         developerVersion: DeveloperDistributionVersion, clientVersion: ClientDistributionVersion, author: String)
                        (implicit log: Logger): TaskId = {
    val task = taskManager.create(s"Build client version ${developerVersion} of service ${serviceName}",
      (taskId, logger) => {
        implicit val log = logger
        val arguments = Seq("buildClientVersion",
          s"distributionName=${config.distributionName}", s"service=${serviceName}",
          s"developerVersion=${developerVersion.toString}", s"clientVersion=${clientVersion.toString}",
          s"author=${author}")
        runBuilder(taskId, arguments)
      })
    task.taskId
  }

  def addClientVersionInfo(versionInfo: ClientVersionInfo)(implicit log: Logger): Future[Unit] = {
    log.info(s"Add client version info ${versionInfo}")
    for {
      result <- collections.Client_VersionsInfo.insert(versionInfo).map(_ => ())
      _ <- removeObsoleteVersions(versionInfo.version.distributionName, versionInfo.serviceName)
    } yield result
  }

  def getClientVersionsInfo(serviceName: ServiceName, distributionName: Option[DistributionName] = None,
                            version: Option[ClientDistributionVersion] = None)(implicit log: Logger): Future[Seq[ClientVersionInfo]] = {
    val serviceArg = Filters.eq("serviceName", serviceName)
    val distributionArg = distributionName.map { distributionName => Filters.eq("version.distributionName", distributionName ) }
    val versionArg = version.map { version => Filters.eq("version", version) }
    val filters = Filters.and((Seq(serviceArg) ++ distributionArg ++ versionArg).asJava)
    collections.Client_VersionsInfo.find(filters)
  }

  private def removeObsoleteVersions(distributionName: DistributionName, serviceName: ServiceName)(implicit log: Logger): Future[Unit] = {
    for {
      versions <- getClientVersionsInfo(serviceName, distributionName = Some(distributionName))
      busyVersions <- getBusyVersions(distributionName, serviceName)
      _ <- {
        val notUsedVersions = versions.filterNot(info => busyVersions.contains(info.version.version))
          .sortBy(_.buildInfo.date.getTime).map(_.version)
        if (notUsedVersions.size > config.versions.maxHistorySize) {
          Future.sequence(notUsedVersions.take(notUsedVersions.size - config.versions.maxHistorySize).map { version =>
            removeClientVersion(serviceName, version)
          })
        } else {
          Future()
        }
      }
    } yield {}
  }

  def removeClientVersion(serviceName: ServiceName, version: ClientDistributionVersion)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove client version ${version} of service ${serviceName}")
    val filters = Filters.and(
      Filters.eq("serviceName", serviceName),
      Filters.eq("version", version))
    directory.getClientVersionImageFile(serviceName, version).delete()
    collections.Client_VersionsInfo.delete(filters).map(_ > 0)
  }

  def setClientDesiredVersions(deltas: Seq[ClientDesiredVersionDelta])(implicit log: Logger): Future[Unit] = {
    log.info(s"Upload client desired versions ${deltas}")
    collections.Client_DesiredVersions.update(new BsonDocument(), { desiredVersions =>
      val desiredVersionsMap = ClientDesiredVersions.toMap(desiredVersions.map(_.versions).getOrElse(Seq.empty))
      val newVersions =
        deltas.foldLeft(desiredVersionsMap) {
          (map, entry) => entry.version match {
            case Some(version) =>
              map + (entry.serviceName -> version)
            case None =>
              map - entry.serviceName
          }}
      Some(ClientDesiredVersions(ClientDesiredVersions.fromMap(newVersions)))
    }).map(_ => ())
  }

  def getClientDesiredVersions(serviceNames: Set[ServiceName] = Set.empty)
                              (implicit log: Logger): Future[Seq[ClientDesiredVersion]] = {
    collections.Client_DesiredVersions.find(new BsonDocument()).map(_.map(_.versions).headOption.getOrElse(Seq.empty[ClientDesiredVersion])
      .filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)).sortBy(_.serviceName))
  }

  def getClientDesiredVersion(serviceName: ServiceName)(implicit log: Logger): Future[Option[ClientDistributionVersion]] = {
    getClientDesiredVersions(Set(serviceName)).map(_.headOption.map(_.version))
  }

  def getDistributionClientDesiredVersions(distributionName: DistributionName)(implicit log: Logger): Future[Seq[ClientDesiredVersion]] = {
    val filters = Filters.eq("distributionName", distributionName)
    collections.Client_DesiredVersions.find(filters).map(_.map(_.versions).headOption.getOrElse(Seq.empty[ClientDesiredVersion]))
  }

  def getClientUpdateList()(implicit log: Logger): Future[Seq[ClientDesiredVersion]] = {
    for {
      clientDesiredVersions <- getClientDesiredVersions()
        .map(ClientDesiredVersions.toMap(_))
      existingVersions <- Future.sequence(clientDesiredVersions.map { case (serviceName, version) =>
        getClientVersionsInfo(serviceName, Some(config.distributionName), Some(version))
          .map(_.map(_ => ClientDesiredVersion(serviceName, version)))
      }).map(_.flatten)
    } yield {
      val toUpdate = clientDesiredVersions.filterNot { case (serviceName, _) =>
        existingVersions.exists(_.serviceName == serviceName)
      }
      ClientDesiredVersions.fromMap(toUpdate)
    }
  }

  def installClientUpdates(desiredVersions: Seq[ClientDesiredVersion])(implicit log: Logger): TaskId = {
    // TODO
    null
  }

  private def getBusyVersions(distributionName: DistributionName, serviceName: ServiceName)(implicit log: Logger): Future[Set[ClientVersion]] = {
    getClientDesiredVersion(serviceName).map(_.toSet.filter(_.distributionName == distributionName).map(_.version))
  }
}
