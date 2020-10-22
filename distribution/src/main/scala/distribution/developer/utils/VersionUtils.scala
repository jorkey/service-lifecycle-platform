package distribution.developer.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common.{ClientName, ProfileName, ServiceName}
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.{ClientDesiredVersions, DesiredVersions, TestedVersions}
import com.vyulabs.update.utils.JsUtils.MergedJsObject
import com.vyulabs.update.version.BuildVersion
import distribution.graphql.{InvalidConfigException, NotFoundException}
import distribution.utils.{GetUtils, PutUtils}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
import spray.json._
import spray.json.DefaultJsonProtocol._

trait VersionUtils extends distribution.utils.VersionUtils
    with ClientsUtils with GetUtils with PutUtils with DeveloperDistributionWebPaths with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected implicit val executionContext: ExecutionContext
  protected implicit val dir: DeveloperDistributionDirectory

  def getDesiredVersions(clientName: Option[ClientName], merged: Boolean): Future[DesiredVersions] = {
    clientName match {
      case Some(clientName) =>
        filterDesiredVersionsByProfile(clientName, if (merged) {
          getMergedDesiredVersions(clientName)
        } else {
          getClientDesiredVersions(clientName)
        })
      case None =>
        getDesiredVersions()
    }
  }

  def getClientDesiredVersions(clientName: ClientName): Future[DesiredVersions] = {
    val clientArg = Filters.eq("clientName", clientName)
    for {
      collection <- collections.ClientDesiredVersions
      profile <- collection.find(clientArg).map(_.headOption.map(_.desiredVersions)
        .getOrElse(throw NotFoundException(s"No personal desired versions for client ${clientName}")))
    } yield profile
  }

  def getInstalledVersions(clientName: ClientName): Future[DesiredVersions] = {
    val clientArg = Filters.eq("clientName", clientName)
    for {
      collection <- collections.ClientDesiredVersions
      profile <- collection.find(clientArg).map(_.headOption.map(_.desiredVersions)
        .getOrElse(throw NotFoundException(s"No installed desired versions for client ${clientName}")))
    } yield profile
  }

  def getTestedVersionsByProfile(profileName: ProfileName): Future[TestedVersions] = {
    val profileArg = Filters.eq("profileName", profileName)
    for {
      collection <- collections.TestedVersions
      profile <- collection.find(profileArg).map(_.headOption
        .getOrElse(throw NotFoundException(s"No tested versions for profile ${profileName}")))
    } yield profile
  }

  def getTestedVersionsByClient(clientName: ClientName): Future[TestedVersions] = {
    for {
      config <- getClientConfig(clientName)
      testedVersions <- getTestedVersionsByProfile(config.installProfile)
    } yield testedVersions
  }

  def filterDesiredVersionsByProfile(clientName: ClientName, future: Future[DesiredVersions]): Future[DesiredVersions] = {
    for {
      desiredVersions <- future
      installProfile <- getClientInstallProfile(clientName)
      versions <- Future(DesiredVersions(desiredVersions.versions.filterKeys(installProfile.services.contains(_))))
    } yield versions
  }

  def getMergedDesiredVersions(clientName: ClientName): Future[DesiredVersions] = {
    for {
      clientConfig <- getClientConfig(clientName)
      developerVersions <- { clientConfig.testClientMatch match {
          case Some(testClientMatch) =>
            for {
              testedVersions <- getTestedVersionsByProfile(clientConfig.installProfile).map(testedVersions => {
                  val regexp = testClientMatch.r
                  val testCondition = testedVersions.signatures.exists(signature =>
                    signature.clientName match {
                      case regexp() =>
                        true
                      case _ =>
                        false
                    })
                  if (testCondition) {
                    testedVersions.versions
                  } else {
                    throw NotFoundException(s"Desired versions for client ${clientName} are not tested")
                  }
              })
            } yield testedVersions
          case None =>
            getDesiredVersions().map(_.versions)
        }}
      clientDesiredVersions <- getClientDesiredVersions(clientName).map(v => Some(v.versions)).recover{ case e => None }
      versions <- Future {
        if (clientConfig.testClientMatch.isDefined && clientDesiredVersions.isDefined) {
          throw InvalidConfigException("Client required preliminary testing shouldn't have personal desired versions")
        }
        val developerJson = developerVersions.toJson
        val clientJson = clientDesiredVersions.map(_.toJson)
        val mergedJson = (developerJson, clientJson) match {
          case (commonJson, Some(clientJson)) =>
            commonJson.merge(clientJson)
          case (commonConfig, None) =>
            commonConfig
        }
        val mergedVersions = mergedJson.convertTo[Map[ServiceName, BuildVersion]]
        DesiredVersions(mergedVersions)
      }
    } yield versions
  }

  def uploadTestedVersions(clientName: ClientName): Route = {
    // TODO graphql
    /*uploadFileToJson(testedVersionsName, (json) => {
      val promise = Promise[Unit]()
      getClientConfig(clientName).onComplete {
        case Success(config) =>
          overwriteFileContentWithLock(dir.getTestedVersionsFile(config.installProfile), content => {
            val testedVersions =
              try { content.map(_.decodeString("utf8").parseJson.convertTo[TestedVersions]) }
              catch { case ex: Exception => log.error("Exception", ex); None }
            val versions = json.convertTo[ServicesVersions]
            val testRecord = TestSignature(clientName, new Date())
            val testSignatures = testedVersions match {
              case Some(testedVersions) =>
                if (testedVersions.versions.equals(versions.servicesVersions)) {
                  testedVersions.signatures :+ testRecord
                } else {
                  Seq(testRecord)
                }
              case None =>
                Seq(testRecord)
            }
            val newTestedVersions = TestedVersions(versions.servicesVersions, testSignatures)
            ByteString(newTestedVersions.toJson.sortedPrint.getBytes("utf8"))
          }).onComplete { promise.complete(_) }
        case Failure(e) =>
          promise.failure(e)
      }
      onSuccess(promise.future)(complete(StatusCodes.OK))
    })*/

    complete(StatusCodes.OK)
  }

  override protected def getBusyVersions(serviceName: ServiceName): Future[Set[BuildVersion]] = {
    val desiredVersion = getDesiredVersions().map(_.versions.get(serviceName))
    val clientDesiredVersions = dir.getClients().map { clientName =>
      getClientDesiredVersions(clientName).map(_.versions.get(serviceName))
    }
    val testedVersions = dir.getProfiles().map { profileName =>
      getTestedVersionsByProfile(profileName).map(_.versions.get(serviceName))
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
