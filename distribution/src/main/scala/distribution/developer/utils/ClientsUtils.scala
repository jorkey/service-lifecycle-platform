package distribution.developer.utils

import java.io.{IOException}
import java.util.Date

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.vyulabs.update.common.Common._
import com.vyulabs.update.config.{ClientConfig, ClientInfo, InstallProfile}
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.DesiredVersions._
import com.vyulabs.update.info._
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.JsUtils._
import distribution.utils.IoUtils
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

trait ClientsUtils extends IoUtils with DeveloperDistributionWebPaths with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val system: ActorSystem
  implicit val materializer: Materializer

  val filesLocker: SmartFilesLocker
  val dir: DeveloperDistributionDirectory

  def getClientsInfo(): Source[ClientInfo, NotUsed] = {
    Source.future(
      Source(dir.getClientsDir().list().toList)
        .map(clientName => getClientConfig(clientName).map(config => ClientInfo(clientName, config.installProfile, config.testClientMatch)))
        .flatMapConcat(config => Source.future(config))
        .runFold(Seq.empty[ClientInfo])((seq, info) => seq :+ info))
      .flatMapConcat(clients => Source.fromIterator(() => clients.iterator))
  }

  def getClientConfig(clientName: ClientName): Future[ClientConfig] = {
    val promise = Promise[ClientConfig]()
    getFileContentWithLock(dir.getClientConfigFile(clientName)).onComplete {
      case Success(bytes) =>
        bytes match {
          case Some(bytes) =>
            val clientConfig = bytes.decodeString("utf8").parseJson.convertTo[ClientConfig]
            promise.success(clientConfig)
          case None =>
            promise.failure(new IOException(s"Can't find config of client ${clientName}"))
        }
      case Failure(ex) =>
        promise.failure(ex)
    }
    promise.future
  }

  def getClientInstallProfile(clientName: ClientName): Future[InstallProfile] = {
    val promise = Promise[InstallProfile]()
    getClientConfig(clientName).onComplete {
      case Success(clientConfig) =>
        getInstallProfile(clientConfig.installProfile).onComplete { promise.complete(_) }
      case Failure(ex) =>
        promise.failure(ex)
    }
    promise.future
  }

  def getInstallProfile(profileName: InstallProfileName): Future[InstallProfile] = {
    val promise = Promise[InstallProfile]()
    val file = dir.getInstallProfileFile(profileName)
    getFileContentWithLock(file).onComplete {
      case Success(bytes) =>
        bytes match {
          case Some(bytes) =>
            val installProfile = bytes.decodeString("utf8").parseJson.convertTo[InstallProfile]
            promise.success(installProfile)
          case None =>
            promise.failure(new IOException(s"Can't find profile ${profileName}"))
        }
      case Failure(ex) =>
        promise.failure(ex)
    }
    promise.future
  }

  def getDesiredVersions(clientName: Option[ClientName]): Future[Option[DesiredVersions]] = {
    parseJsonFileWithLock[DesiredVersions](dir.getDesiredVersionsFile(clientName))
  }

  def getClientDesiredVersions(clientName: ClientName): Future[Option[DesiredVersions]] = {
    filterDesiredVersionsByProfile(clientName, getMergedDesiredVersions(clientName))
  }

  def getTestedVersions(clientName: ClientName): Future[Option[TestedVersions]] = {
    val promise = Promise[Option[TestedVersions]]()
    getClientConfig(clientName).onComplete {
      case Success(config) =>
        parseJsonFileWithLock[TestedVersions](dir.getTestedVersionsFile(config.installProfile)).onComplete {
          promise.complete(_)
        }
      case Failure(ex) =>
        promise.failure(ex)
    }
    promise.future
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
    val future = getDesiredVersions(None)
    future.onComplete {
      case Success(commonDesiredVersions) =>
        val future = getDesiredVersions(Some(clientName))
        future.onComplete {
          case Success(clientDesiredVersions) =>
            val commonJson = commonDesiredVersions.map(_.toJson)
            val clientJson = clientDesiredVersions.map(_.toJson)
            val mergedJson = (commonJson, clientJson) match {
              case (Some(commonJson), Some(clientJson)) =>
                Some(commonJson.merge(clientJson))
              case (Some(commonConfig), None) =>
                Some(commonConfig)
              case (None, Some(clientConfig)) =>
                Some(clientConfig)
              case (None, None) =>
                None
            }
            promise.success(mergedJson.map(_.convertTo[DesiredVersions]))
            null
          case Failure(e) =>
            promise.failure(e)
        }
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
            val testedVersions = content.map(_.decodeString("utf8").parseJson.convertTo[TestedVersions])
            val versions = json.convertTo[ServicesVersions]
            val testRecord = TestSignature(clientName, new Date())
            val testSignatures = testedVersions match {
              case Some(testedVersions) =>
                if (testedVersions.testedVersions.equals(versions.servicesVersions)) {
                  testedVersions.testSignatures :+ testRecord
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
}
