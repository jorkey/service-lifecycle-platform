package distribution

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route.seal
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Route}
import akka.stream.Materializer
import com.vyulabs.update.common.Common
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info.DeveloperVersionsInfoJson._
import com.vyulabs.update.info.ProfiledServiceName
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.logs.ServiceLogs
import com.vyulabs.update.users.{UserInfo, UserRole, UsersCredentials}
import com.vyulabs.update.version.BuildVersion
import distribution.config.DistributionConfig
import distribution.graphql.Graphql
import distribution.uploaders.{ClientFaultUploader, StateUploader}
import distribution.graphql.utils.{GetUtils, PutUtils, ClientVersionUtils}

import scala.concurrent.ExecutionContext

/* TODO graphql - remove
class ClientDistribution(protected val dir: DistributionDirectory,
                         protected val collections: DatabaseCollections,
                         protected val config: DistributionConfig,
                         protected val usersCredentials: UsersCredentials,
                         protected val graphql: Graphql,
                         protected val stateUploader: ClientStateUploader,
                         protected val logUploader: ClientLogUploader,
                         protected val faultUploader: ClientFaultUploader)
                        (implicit protected val system: ActorSystem,
                         protected val materializer: Materializer,
                         protected val executionContext: ExecutionContext,
                         protected val filesLocker: SmartFilesLocker)
    extends Distribution(usersCredentials, graphql) with GetUtils with PutUtils with VersionUtils with CommonUtils
      with DistributionWebPaths with SprayJsonSupport {

  implicit val directory = dir

  protected val versionHistoryConfig = config.versionHistory

  private val prefix = "update"

  val route: Route =
    path(pingPath) {
      get {
        complete("pong")
      }
    } ~
    logRequest(requestLogger _) {
      logResult(resultLogger _) {
        handleExceptions(exceptionHandler) {
          extractRequestContext { ctx =>
            pathPrefix(prefix / apiPathPrefix) {
                    get {
                      path(versionImagePath / ".*".r / ".*".r) { (service, version) =>
                        getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                      } ~
                    } ~
                      post {
                        authorize(role == UserRole.Administrator) {
                          path(versionImagePath / ".*".r / ".*".r) { (service, version) =>
                            val buildVersion = BuildVersion.parse(version)
                            versionImageUpload(service, buildVersion)
                          } ~
                        } ~
                          authorize(role == UserRole.Service) {
                              path(serviceFaultPath / ".*".r) { (serviceName) =>
                                uploadFileToSource(serviceFaultName, (fileInfo, source) => {
                                  faultUploader.receiveFault(serviceName, fileInfo.getFileName, source)
                                })
                              }
                          }
                      }
                  }
                }
              }
            } ~
              get {
                  get {
                    path(prefix / downloadVersionPath / ".*".r / ".*".r) { (service, version) =>
                      getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                    } ~
        }
      }
    }
}
*/