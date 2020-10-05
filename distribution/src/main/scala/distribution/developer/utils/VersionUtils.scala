package distribution.developer.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.{DesiredVersions, TestedVersions}
import com.vyulabs.update.version.BuildVersion
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

trait VersionUtils extends ClientsUtils with distribution.utils.VersionUtils with DeveloperDistributionWebPaths with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected implicit val executionContext: ExecutionContext
  protected implicit val dir: DeveloperDistributionDirectory

  override protected def getBusyVersions(serviceName: ServiceName): Future[Set[BuildVersion]] = {
    val desiredVersion = parseJsonFileWithLock[DesiredVersions](dir.getDesiredVersionsFile()).map(
      versions => versions.map(_.desiredVersions.get(serviceName))).map(version => version.getOrElse(None))
    val clientDesiredVersions = dir.getClients().map { clientName =>
      parseJsonFileWithLock[DesiredVersions](dir.getDesiredVersionsFile(clientName)).map(
        versions => versions.map(_.desiredVersions.get(serviceName))).map(version => version.getOrElse(None))
    }
    val testedVersions = dir.getProfiles().map { profileName =>
      parseJsonFileWithLock[TestedVersions](dir.getTestedVersionsFile(profileName)).map(
        versions => versions.map(_.testedVersions.get(serviceName))).map(version => version.getOrElse(None))
    }
    val promise = Promise.apply[Set[BuildVersion]]()
    Future.sequence(Set(desiredVersion) ++ clientDesiredVersions ++ testedVersions).onComplete {
      case Success(versions) =>
        promise.success(versions.flatten)
      case Failure(ex) =>
        promise.failure(ex)
    }
    promise.future
  }
}
