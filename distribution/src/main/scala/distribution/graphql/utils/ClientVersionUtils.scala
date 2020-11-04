package distribution.graphql.utils

import java.io.{File, IOException}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives.{failWith, _}
import akka.http.scaladsl.server.Route
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info.{BuildInfo, DesiredVersion, DesiredVersions, InstallInfo, InstalledVersionInfo}
import com.vyulabs.update.version.BuildVersion
import distribution.DatabaseCollections
import distribution.config.{VersionHistoryConfig}
import org.bson.BsonDocument
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait ClientVersionUtils extends ClientsUtils with GetUtils with PutUtils with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val versionHistoryConfig: VersionHistoryConfig
  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections

  protected implicit val executionContext: ExecutionContext

  def clientVersionImageUpload(serviceName: ServiceName, buildVersion: BuildVersion): Route = {
    val imageFile = dir.getClientVersionImageFile(serviceName, buildVersion)
    val directory = new File(imageFile.getParent)
    if (directory.exists() || directory.mkdir()) {
      fileUploadWithLock("version", imageFile)
    } else {
      failWith(new IOException(s"Can't make directory ${directory}"))
    }
  }

  def addClientVersionInfo(serviceName: ServiceName, version: BuildVersion, buildInfo: BuildInfo, installInfo: InstallInfo): Future[InstalledVersionInfo] = {
    log.info(s"Add client service ${serviceName} version ${version} build info ${buildInfo} install info ${installInfo}")
    for {
      collection <- collections.Client_VersionsInfo
      versionInfo <- {
        val versionInfo = InstalledVersionInfo(serviceName, version, buildInfo, installInfo)
        collection.insert(versionInfo).map(_ => versionInfo)
      }
      _ <- removeObsoleteVersions(serviceName)
    } yield versionInfo
  }

  def getClientVersionsInfo(serviceName: ServiceName, version: Option[BuildVersion] = None): Future[Seq[InstalledVersionInfo]] = {
    val serviceArg = Filters.eq("serviceName", serviceName)
    val versionArg = version.map { version => Filters.eq("version", version.toString) }
    val filters = Filters.and((Seq(serviceArg) ++ versionArg).asJava)
    for {
      collection <- collections.Client_VersionsInfo
      info <- collection.find(filters)
    } yield info
  }

  private def removeObsoleteVersions(serviceName: ServiceName): Future[Unit] = {
    for {
      versions <- getClientVersionsInfo(serviceName)
      busyVersions <- getBusyVersions(serviceName)
      complete <- {
        val notUsedVersions = versions.filterNot(version => busyVersions.contains(version.version))
          .sortBy(_.buildInfo.date.getTime).map(_.version)
        if (notUsedVersions.size > versionHistoryConfig.maxSize) {
          Future.sequence(notUsedVersions.take(notUsedVersions.size - versionHistoryConfig.maxSize).map { version =>
            removeClientVersion(serviceName, version)
          })
        } else {
          Future()
        }
      }
    } yield complete
  }

  def removeClientVersion(serviceName: ServiceName, version: BuildVersion): Future[Boolean] = {
    log.info(s"Remove client version ${version} of service ${serviceName}")
    val filters = Filters.and(
      Filters.eq("serviceName", serviceName),
      Filters.eq("version", version.toString))
    dir.getClientVersionImageFile(serviceName, version).delete()
    for {
      collection <- collections.Client_VersionsInfo
      profile <- {
        collection.delete(filters).map(_.getDeletedCount > 0)
      }
    } yield profile
  }

  def getClientDesiredVersions(serviceNames: Set[ServiceName] = Set.empty): Future[Seq[DesiredVersion]] = {
    val filters = new BsonDocument()
    for {
      collection <- collections.Client_DesiredVersions
      profile <- collection.find(filters).map(_.headOption.map(_.versions).getOrElse(Seq.empty[DesiredVersion]))
        .map(_.filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)).sortBy(_.serviceName))
    } yield profile
  }

  def setClientDesiredVersions(desiredVersions: Seq[DesiredVersion]): Future[Boolean] = {
    for {
      collection <- collections.Client_DesiredVersions
      result <- collection.replace(new BsonDocument(), DesiredVersions(desiredVersions)).map(_ => true)
    } yield result
  }

  def getClientDesiredVersion(serviceName: ServiceName): Future[Option[BuildVersion]] = {
    getClientDesiredVersions(Set(serviceName)).map(_.headOption.map(_.buildVersion))
  }

  def getClientDesiredVersions(clientName: ClientName): Future[Seq[DesiredVersion]] = {
    val clientArg = Filters.eq("clientName", clientName)
    for {
      collection <- collections.Client_DesiredVersions
      profile <- collection.find(clientArg).map(_.headOption.map(_.versions).getOrElse(Seq.empty[DesiredVersion]))
    } yield profile
  }

  private def getBusyVersions(serviceName: ServiceName): Future[Set[BuildVersion]] = {
    getClientDesiredVersion(serviceName).map(_.toSet)
  }
}
