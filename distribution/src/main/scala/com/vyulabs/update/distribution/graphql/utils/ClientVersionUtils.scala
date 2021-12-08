package com.vyulabs.update.distribution.graphql.utils

import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{AccountId, DistributionId, ServiceId, TaskId}
import com.vyulabs.update.common.common.Misc
import com.vyulabs.update.common.config.{DistributionConfig, EnvironmentVariable}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.{ClientDesiredVersion, ClientDesiredVersionDelta, ClientDesiredVersions, ClientVersionInfo, DeveloperDesiredVersion, DeveloperDesiredVersionDelta, TimedClientDesiredVersions, TimedDeveloperDesiredVersions}
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion}
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.task.TaskManager
import org.bson.BsonDocument
import org.slf4j.Logger

import java.io.IOException
import java.util.Date
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait ClientVersionUtils {
  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val config: DistributionConfig
  protected val taskManager: TaskManager

  protected val distributionProvidersUtils: DistributionProvidersUtils
  protected val runBuilderUtils: RunBuilderUtils
  protected val tasksUtils: TasksUtils

  protected implicit val executionContext: ExecutionContext

  def buildClientVersions(versions: Seq[DeveloperDesiredVersion], author: AccountId)
                         (implicit log: Logger): TaskId = {
    var cancels = Seq.empty[() => Unit]
    tasksUtils.createTask(
      "BuildClientVersions",
      Seq(TaskParameter("author", author),
          TaskParameter("versions", Misc.seqToCommaSeparatedString(versions))),
      () => if (!tasksUtils.getActiveTasks(taskType = Some("BuildClientVersions")).isEmpty) {
          throw new IOException(s"Build of client versions is already in process")
      },
      (task, logger) => {
        implicit val log = logger
        (for {
          _ <- Future.sequence(versions
            .map(version =>
              if (version.version.distribution != config.distribution) {
                distributionProvidersUtils.downloadProviderVersion(
                  version.version.distribution, version.service, version.version)
              } else {
                Future()
              }))
          _ <- {
            var clientVersions = Seq.empty[ClientDesiredVersionDelta]
            val results = versions
              .map(version => for {
                clientVersion <- getClientVersionsInfo(Some(version.service), Some(version.version.distribution)).map(versions =>
                                  versions.map(_.version)
                                    .sorted(ClientDistributionVersion.ordering)
                                    .reverse
                                    .find(v => DeveloperDistributionVersion.from(v) == version.version)
                                    .map(version => new ClientDistributionVersion(version.distribution, version.developerBuild, version.clientBuild+1))
                                    .getOrElse(ClientDistributionVersion.from(version.version, 0)))
                } yield {
                  clientVersions :+= ClientDesiredVersionDelta(version.service, Some(clientVersion))
                  buildClientVersion(task, version.service, clientVersion, author)
                }
              )
            results.foreach(_.foreach(_._2.foreach(cancel => cancels :+= cancel)))
            Future.sequence(results).map(results => Future.sequence(results.map(_._1))).flatten
              .flatMap(_ => setClientDesiredVersions(clientVersions, author))
          }
        } yield {}, Some(() => cancels.foreach(cancel => cancel())))
      }).id
  }

  private def buildClientVersion(task: TaskId, service: ServiceId,
                                 version: ClientDistributionVersion, author: String)
                                (implicit log: Logger): (Future[Unit], Option[() => Unit]) = {
    val arguments = Seq("buildClientVersion",
      s"distribution=${config.distribution}", s"service=${service}",
      s"version=${version.toString}", s"author=${author}")
    runBuilderUtils.runClientBuilder(task, service, arguments)
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

  def setClientDesiredVersions(deltas: Seq[ClientDesiredVersionDelta], author: AccountId)
                              (implicit log: Logger): Future[Unit] = {
    if (!deltas.isEmpty) {
      log.info(s"Set client desired versions ${deltas}")
      collections.Client_DesiredVersions.update(new BsonDocument(), { desiredVersions =>
        val desiredVersionsMap = ClientDesiredVersions.toMap(desiredVersions.map(_.versions).getOrElse(Seq.empty))
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
        Some(ClientDesiredVersions(author, ClientDesiredVersions.fromMap(newVersions)))
      }).map(_ => ())
    } else {
      Future()
    }
  }

  def getClientDesiredVersions(services: Set[ServiceId] = Set.empty)
                              (implicit log: Logger): Future[Seq[ClientDesiredVersion]] = {
    collections.Client_DesiredVersions.find(new BsonDocument()).map(_.map(_.versions).headOption.getOrElse(Seq.empty[ClientDesiredVersion])
      .filter(v => services.isEmpty || services.contains(v.service)).sortBy(_.service))
  }

  def getClientDesiredVersionsHistory(limit: Int)
                                     (implicit log: Logger): Future[Seq[TimedClientDesiredVersions]] = {
    for {
      history <- collections.Client_DesiredVersions.history(new BsonDocument(), Some(limit))
        .map(_.map(v => TimedClientDesiredVersions(v.time, v.document.author, v.document.versions)))
    } yield history
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
