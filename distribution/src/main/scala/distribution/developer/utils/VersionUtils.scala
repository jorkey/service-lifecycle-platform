package distribution.developer.utils

import java.util.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.util.ByteString
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common.{ClientName, ProfileName, ServiceName}
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.{ClientDesiredVersions, DesiredVersions, ProfileTestedVersions, ServicesVersions, TestSignature, TestedVersions}
import com.vyulabs.update.utils.JsUtils.MergedJsObject
import com.vyulabs.update.version.BuildVersion
import distribution.utils.{GetUtils, PutUtils}
import org.bson.BsonDocument
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

  def getDesiredVersions(clientName: Option[ClientName], summary: Boolean): Future[Option[DesiredVersions]] = {
    clientName match {
      case Some(clientName) =>
        if (summary) {
          getSummaryDesiredVersions(clientName)
        } else {
          getClientDesiredVersions(clientName)
        }
      case None =>
        getCommonDesiredVersions()
    }
  }

  def getCommonDesiredVersions(): Future[Option[DesiredVersions]] = {
    for {
      collection <- mongoDb.getOrCreateCollection[DesiredVersions]()
      profile <- collection.find(new BsonDocument()).map(_.headOption)
    } yield profile
  }

  def getClientDesiredVersions(clientName: ClientName): Future[Option[DesiredVersions]] = {
    val clientArg = Filters.eq("clientName", clientName)
    for {
      collection <- mongoDb.getOrCreateCollection[ClientDesiredVersions]()
      profile <- collection.find(clientArg).map(_.headOption.map(_.desiredVersions))
    } yield profile
  }

  def getInstalledDesiredVersions(clientName: ClientName): Future[Option[DesiredVersions]] = {
    val clientArg = Filters.eq("clientName", clientName)
    for {
      collection <- mongoDb.getOrCreateCollection[ClientDesiredVersions](Some(s"Installed"))
      profile <- collection.find(clientArg).map(_.headOption.map(_.desiredVersions))
    } yield profile
  }

  def getSummaryDesiredVersions(clientName: ClientName): Future[Option[DesiredVersions]] = {
    filterDesiredVersionsByProfile(clientName, getMergedDesiredVersions(clientName))
  }

  def getTestedVersionsByProfile(profileName: ProfileName): Future[Option[TestedVersions]] = {
    val profileArg = Filters.eq("profileName", profileName)
    for {
      collection <- mongoDb.getOrCreateCollection[ProfileTestedVersions]()
      profile <- collection.find(profileArg).map(_.headOption.map(_.testedVersions))
    } yield profile
  }

  def getTestedVersionsByClient(clientName: ClientName): Future[Option[TestedVersions]] = {
    for  {
      config <- getClientConfig(clientName)
      testedVersions <- getTestedVersionsByProfile(config.installProfile)
    } yield testedVersions
  }

  def filterDesiredVersionsByProfile(clientName: ClientName, future: Future[Option[DesiredVersions]]): Future[Option[DesiredVersions]] = {
    val promise = Promise[Option[DesiredVersions]]()
    future.onComplete {
      case Success(desiredVersions) =>
        desiredVersions match {
          case Some(desiredVersions) =>
            getClientInstallProfile(clientName).onComplete {
              case Success(installProfile) =>
                val filteredVersions = desiredVersions.desiredVersions.filterKeys(installProfile.services.contains(_))
                promise.success(Some(DesiredVersions(filteredVersions)))
              case Failure(ex) =>
                promise.failure(ex)
            }
          case None =>
            promise.success(None)
        }
      case Failure(ex) =>
        promise.failure(ex)
    }
    promise.future
  }

  def getMergedDesiredVersions(clientName: ClientName): Future[Option[DesiredVersions]] = {
    val promise = Promise[Option[DesiredVersions]]()
    getClientConfig(clientName).onComplete {
      case Success(config) =>
        (config.testClientMatch match {
          case Some(testClientMatch) =>
            val promise = Promise[Option[Map[ServiceName, BuildVersion]]]()
            getTestedVersionsByProfile(config.installProfile).onComplete {
              case Success(testedVersions) =>
                testedVersions match {
                  case Some(testedVersions) =>
                    val regexp = testClientMatch.r
                    val testCondition = testedVersions.signatures.exists(signature =>
                      signature.clientName match {
                        case regexp() =>
                          true
                        case _ =>
                          false
                      })
                    if (testCondition) {
                      promise.success(Some(testedVersions.versions))
                    } else {
                      log.info(s"Desired versions for client ${clientName} are not tested")
                      promise.success(None)
                    }
                  case None =>
                    log.info(s"Desired versions for client ${clientName} are not found")
                    promise.success(None)
                }
              case Failure(ex) =>
                promise.failure(ex)
            }
            promise.future
          case None =>
            getCommonDesiredVersions().map(_.map(_.desiredVersions))
        }).onComplete {
          case Success(developerVersions) =>
            getClientDesiredVersions(clientName).map(_.map(_.desiredVersions)).onComplete {
              case Success(clientDesiredVersions) =>
                val developerJson = developerVersions.map(_.toJson)
                val clientJson = clientDesiredVersions.map(_.toJson)
                val mergedJson = (developerJson, clientJson) match {
                  case (Some(commonJson), Some(clientJson)) =>
                    Some(commonJson.merge(clientJson))
                  case (Some(commonConfig), None) =>
                    Some(commonConfig)
                  case (None, Some(clientConfig)) =>
                    Some(clientConfig)
                  case (None, None) =>
                    None
                }
                val mergedVersions = mergedJson.map(_.convertTo[Map[ServiceName, BuildVersion]])
                promise.success(mergedVersions.map(DesiredVersions(_)))
              case Failure(e) =>
                promise.failure(e)
            }
          case Failure(e) =>
            promise.failure(e)
        }
        promise.future
      case Failure(e) =>
        promise.failure(e)
    }
    promise.future
  }

  def uploadTestedVersions(clientName: ClientName): Route = {
    uploadFileToJson(testedVersionsName, (json) => {
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
    })
  }

  override protected def getBusyVersions(serviceName: ServiceName): Future[Set[BuildVersion]] = {
    val desiredVersion = getCommonDesiredVersions().map(
      versions => versions.map(_.desiredVersions.get(serviceName))).map(version => version.getOrElse(None))
    val clientDesiredVersions = dir.getClients().map { clientName =>
      getClientDesiredVersions(clientName).map(
        versions => versions.map(_.desiredVersions.get(serviceName))).map(version => version.getOrElse(None))
    }
    val testedVersions = dir.getProfiles().map { profileName =>
      getTestedVersionsByProfile(profileName).map(
        versions => versions.map(_.versions.get(serviceName))).map(version => version.getOrElse(None))
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
