package distribution.utils

import java.io.{File, IOException}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives.{failWith, _}
import akka.http.scaladsl.server.Route
import com.mongodb.client.model.{Filters}
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.distribution.{DistributionDirectory, DistributionWebPaths}
import com.vyulabs.update.info.{BuildVersionInfo, DesiredVersion, DesiredVersions, VersionInfo}
import com.vyulabs.update.utils.{IoUtils, Utils}
import com.vyulabs.update.version.BuildVersion
import distribution.DatabaseCollections
import distribution.config.DistributionConfig
import distribution.graphql.NotFoundException
import org.bson.BsonDocument
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait VersionUtils extends GetUtils with PutUtils with DistributionWebPaths with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val config: DistributionConfig
  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections

  protected implicit val executionContext: ExecutionContext

  def versionImageUpload(serviceName: ServiceName, buildVersion: BuildVersion): Route = {
    val imageFile = dir.getVersionImageFile(serviceName, buildVersion)
    val directory = new File(imageFile.getParent)
    if (directory.exists() || directory.mkdir()) {
      fileUploadWithLock(versionName, imageFile)
    } else {
      failWith(new IOException(s"Can't make directory ${directory}"))
    }
  }

  def addVersionInfo(serviceName: ServiceName, version: BuildVersion, buildInfo: BuildVersionInfo): Future[VersionInfo] = {
    log.info(s"Add service ${serviceName} version ${version} info ${buildInfo} ")
    for {
      collection <- collections.VersionInfo
      versionInfo <- {
        val versionInfo = VersionInfo(serviceName, version.client, version, buildInfo)
        collection.insert(versionInfo).map(_ => versionInfo)
      }
      _ <- removeObsoleteVersions(serviceName, version.client)
    } yield versionInfo
  }

  private def removeObsoleteVersions(serviceName: ServiceName, client: Option[ClientName]): Future[Unit] = {
    for {
      versions <- getVersionsInfo(serviceName, client)
      busyVersions <- getBusyVersions(serviceName)
      complete <- {
        val notUsedVersions = versions.filterNot(version => busyVersions.contains(version.version))
          .sortBy(_.buildInfo.date.getTime).map(_.version)
        if (notUsedVersions.size > config.versionsHistorySize) {
          Future.sequence(notUsedVersions.take(notUsedVersions.size - config.versionsHistorySize).map { version =>
            removeVersion(serviceName, version)
          })
        } else {
          Future()
        }
      }
    } yield complete
  }

  def removeVersion(serviceName: ServiceName, version: BuildVersion): Future[Boolean] = {
    log.info(s"Remove version ${version} of service ${serviceName}")
    val filters = Filters.and(
      Filters.eq("serviceName", serviceName),
      Filters.eq("version", version.toString))
    dir.removeVersion(serviceName, version)
    for {
      collection <- collections.VersionInfo
      profile <- {
        collection.delete(filters).map(_.getDeletedCount > 0)
      }
    } yield profile
  }

  protected def getBusyVersions(serviceName: ServiceName): Future[Set[BuildVersion]] = {
    getDesiredVersions(Set(serviceName)).map(_.map(_.buildVersion).toSet)
  }

  def getVersionsInfo(serviceName: ServiceName, clientName: Option[ClientName] = None,
                      version: Option[BuildVersion] = None): Future[Seq[VersionInfo]] = {
    val serviceArg = Filters.eq("serviceName", serviceName)
    val clientArg = clientName.map { clientName => Filters.eq("clientName", clientName) }
    val versionArg = version.map { version => Filters.eq("version", version.toString) }
    val filters = Filters.and((Seq(serviceArg) ++ clientArg ++ versionArg).asJava)
    for {
      collection <- collections.VersionInfo
      info <- collection.find(filters)
    } yield info
  }

  def setDesiredVersions(desiredVersions: Seq[DesiredVersion]): Future[Boolean] = {
    log.info(s"Set desired versions ${desiredVersions}")
    for {
      collection <- collections.DesiredVersions
      result <- collection.replace(new BsonDocument(), DesiredVersions(desiredVersions)).map(_ => true)
    } yield result
  }

  def getDesiredVersions(serviceNames: Set[ServiceName] = Set.empty): Future[Seq[DesiredVersion]] = {
    for {
      collection <- collections.DesiredVersions
      profile <- collection.find(new BsonDocument()).map(_.headOption.map(_.versions).getOrElse(Seq.empty[DesiredVersion]))
        .map(_.filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)).sortBy(_.serviceName))
    } yield profile
  }

  def getDesiredVersion(serviceName: ServiceName): Future[Option[BuildVersion]] = {
    getDesiredVersions(Set(serviceName)).map(_.headOption.map(_.buildVersion))
  }

  def getVersion(): Option[BuildVersion] = {
    Utils.getManifestBuildVersion(Common.DistributionServiceName)
  }

  def getServiceVersion(serviceName: ServiceName, directory: File): BuildVersion = {
    IoUtils.readServiceVersion(serviceName, directory).getOrElse(throw NotFoundException(s"Can't found service ${serviceName} version file")) // TODO move to async
  }
}
