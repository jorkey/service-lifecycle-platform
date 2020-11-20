package distribution.graphql.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common.{DistributionName, ServiceName}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info.{BuildInfo, ClientDesiredVersion, ClientVersionInfo, DeveloperDesiredVersion, InstallInfo}
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import distribution.config.VersionHistoryConfig
import distribution.mongo.{ClientDesiredVersionsDocument, ClientVersionInfoDocument, DatabaseCollections, DeveloperDesiredVersionsDocument}
import org.bson.BsonDocument
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait ClientVersionUtils extends DistributionClientsUtils with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val versionHistoryConfig: VersionHistoryConfig
  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections

  protected implicit val executionContext: ExecutionContext

  def addClientVersionInfo(versionInfo: ClientVersionInfo): Future[Boolean] = {
    log.info(s"Add client version info ${versionInfo}")
    for {
      collection <- collections.Client_VersionsInfo
      id <- collections.getNextSequence(collection.getName())
      result <- {
        val document = ClientVersionInfoDocument(id, versionInfo)
        collection.insert(document).map(_ => true)
      }
      _ <- removeObsoleteVersions(versionInfo.serviceName)
    } yield result
  }

  def getClientVersionsInfo(serviceName: ServiceName, version: Option[DeveloperDistributionVersion] = None): Future[Seq[ClientVersionInfo]] = {
    val serviceArg = Filters.eq("info.serviceName", serviceName)
    val versionArg = version.map { version => Filters.eq("info.version", version.toString) }
    val filters = Filters.and((Seq(serviceArg) ++ versionArg).asJava)
    for {
      collection <- collections.Client_VersionsInfo
      info <- collection.find(filters).map(_.map(_.info))
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

  def removeClientVersion(serviceName: ServiceName, version: ClientDistributionVersion): Future[Boolean] = {
    log.info(s"Remove client version ${version} of service ${serviceName}")
    val filters = Filters.and(
      Filters.eq("info.serviceName", serviceName),
      Filters.eq("info.version", version.toString))
    dir.getClientVersionImageFile(serviceName, version).delete()
    for {
      collection <- collections.Client_VersionsInfo
      profile <- {
        collection.delete(filters).map(_.getDeletedCount > 0)
      }
    } yield profile
  }

  def getClientDesiredVersions(serviceNames: Set[ServiceName] = Set.empty): Future[Seq[ClientDesiredVersion]] = {
    val filters = new BsonDocument()
    for {
      collection <- collections.Client_DesiredVersions
      profile <- collection.find(filters).map(_.headOption.map(_.versions).getOrElse(Seq.empty[ClientDesiredVersion]))
        .map(_.filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)).sortBy(_.serviceName))
    } yield profile
  }

  def setClientDesiredVersions(desiredVersions: Seq[ClientDesiredVersion]): Future[Boolean] = {
    for {
      collection <- collections.Client_DesiredVersions
      result <- collection.replace(new BsonDocument(), ClientDesiredVersionsDocument(desiredVersions)).map(_ => true)
    } yield result
  }

  def getClientDesiredVersion(serviceName: ServiceName): Future[Option[ClientDistributionVersion]] = {
    getClientDesiredVersions(Set(serviceName)).map(_.headOption.map(_.version))
  }

  def getDistributionClientDesiredVersions(distributionName: DistributionName): Future[Seq[ClientDesiredVersion]] = {
    val clientArg = Filters.eq("versions.distributionName", distributionName)
    for {
      collection <- collections.Client_DesiredVersions
      profile <- collection.find(clientArg).map(_.headOption.map(_.versions).getOrElse(Seq.empty[ClientDesiredVersion]))
    } yield profile
  }

  private def getBusyVersions(serviceName: ServiceName): Future[Set[ClientDistributionVersion]] = {
    getClientDesiredVersion(serviceName).map(_.toSet)
  }
}
