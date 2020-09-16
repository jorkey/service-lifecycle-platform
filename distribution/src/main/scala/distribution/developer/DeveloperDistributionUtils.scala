package distribution.developer

import java.io.{File, IOException}
import java.util.Date

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, failWith, onComplete, onSuccess}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{ClientName, InstallProfileName, InstanceId, ServiceDirectory, ServiceName}
import com.vyulabs.update.config.{ClientConfig, ClientInfo, InstallProfile}
import com.vyulabs.update.distribution.DistributionUtils
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.{DesiredVersions, InstanceVersions, InstancesState, ServicesState, ServicesVersions, TestSignature, TestedVersions}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.UsersCredentials
import distribution.developer.config.DeveloperDistributionConfig
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

import spray.json._
import com.vyulabs.update.utils.JsUtils._
import com.vyulabs.update.config.ClientConfig._
import com.vyulabs.update.info.InstancesState._
import com.vyulabs.update.info.InstanceVersions._
import com.vyulabs.update.info.DesiredVersions._
import com.vyulabs.update.config.InstallProfile._
import com.vyulabs.update.info.ServicesVersions._

import ExecutionContext.Implicits.global

class DeveloperDistributionUtils(dir: DeveloperDistributionDirectory, config: DeveloperDistributionConfig, usersCredentials: UsersCredentials)
                           (implicit filesLocker: SmartFilesLocker, system: ActorSystem, materializer: Materializer)
      extends DistributionUtils(dir, usersCredentials) with DeveloperDistributionWebPaths with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected def getClientsInfo(): Source[ClientInfo, NotUsed] = {
    Source.future(
      Source(dir.getClientsDir().list().toList)
        .map(clientName => getClientConfig(clientName).map(config => ClientInfo(clientName, config.installProfile, config.testClientMatch)))
        .flatMapConcat(config => Source.future(config))
        .runFold(Seq.empty[ClientInfo])((seq, info) => seq :+ info))
      .flatMapConcat(clients => Source.fromIterator(() => clients.iterator))
  }

  protected def getServicesState(): ServicesState = {
    ServicesState.getOwnInstanceState(Common.DistributionServiceName)
      .merge(ServicesState.getServiceInstanceState(new File("."), Common.ScriptsServiceName))
      .merge(ServicesState.getServiceInstanceState(new File(config.builderDirectory), Common.BuilderServiceName))
      .merge(ServicesState.getServiceInstanceState(new File(config.builderDirectory), Common.ScriptsServiceName))
  }

  protected def getInstancesState(): InstancesState = {
    InstancesState.empty.addState(config.instanceId, getServicesState())
  }

  protected def getInstanceVersions(): InstanceVersions = {
    InstanceVersions.empty.addVersions(config.instanceId, getServicesState())
  }

  protected def getInstanceVersions(clientName: ClientName): Route = {
    onSuccess(getClientInstancesState(clientName).collect {
      case Some(state) =>
        var versions = InstanceVersions.empty
        state.instances.foreach { case (instanceId, servicesStates) =>
          versions = versions.addVersions(instanceId, servicesStates)
        }
        versions
      case None =>
        InstanceVersions.empty
    }) { state => complete(state) }
  }

  protected def getServiceState(clientName: ClientName, instanceId: InstanceId,
                              directory: ServiceDirectory, serviceName: ServiceName): Route = {
    onSuccess(getClientInstancesState(clientName)) {
      case Some(instancesState) =>
        instancesState.instances.get(instanceId) match {
          case Some(servicesState) =>
            servicesState.directories.get(directory) match {
              case Some(directoryState) =>
                directoryState.get(serviceName) match {
                  case Some(state) =>
                    complete(state)
                  case None =>
                    log.debug(s"Service ${serviceName} is not found")
                    complete(StatusCodes.NotFound)
                }
              case None =>
                log.debug(s"Directory ${directory} is not found")
                complete(StatusCodes.NotFound)
            }
          case None =>
            log.debug(s"Instance ${instanceId} is not found")
            complete(StatusCodes.NotFound)
        }
      case None =>
        log.debug(s"Client ${clientName} state is not found")
        complete(StatusCodes.NotFound)
    }
  }

  protected def getClientConfig(clientName: ClientName): Future[ClientConfig] = {
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

  protected def getClientInstancesState(clientName: ClientName): Future[Option[InstancesState]] = {
    val promise = Promise[Option[InstancesState]]()
    if (config.selfDistributionClient.contains(clientName)) {
      promise.success(Some(getInstancesState()))
    } else {
      getFileContentWithLock(dir.getInstancesStateFile(clientName)).onComplete {
        case Success(bytes) =>
          bytes match {
            case Some(bytes) =>
              val instancesState = bytes.decodeString("utf8").parseJson.convertTo[InstancesState]
              promise.success(Some(instancesState))
            case None =>
              promise.success(None)
          }
        case Failure(ex) =>
          promise.failure(ex)
      }
    }
    promise.future
  }

  protected def getClientInstallProfile(clientName: ClientName): Future[InstallProfile] = {
    val promise = Promise[InstallProfile]()
    getClientConfig(clientName).onComplete {
      case Success(clientConfig) =>
        getInstallProfile(clientConfig.installProfile).onComplete { promise.complete(_) }
      case Failure(ex) =>
        promise.failure(ex)
    }
    promise.future
  }

  protected def getInstallProfile(profileName: InstallProfileName): Future[InstallProfile] = {
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

  protected def getDesiredVersions(clientName: Option[ClientName]): Future[Option[DesiredVersions]] = {
    parseJsonFileWithLock[DesiredVersions](dir.getDesiredVersionsFile(clientName))
  }

  protected def getClientDesiredVersions(clientName: ClientName): Future[Option[DesiredVersions]] = {
    filterDesiredVersionsByProfile(clientName, getMergedDesiredVersions(clientName))
  }

  protected def getTestedVersions(clientName: ClientName): Future[Option[TestedVersions]] = {
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

  protected def filterDesiredVersionsByProfile(clientName: ClientName,
                                               future: Future[Option[DesiredVersions]]): Future[Option[DesiredVersions]] = {
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

  protected def getMergedDesiredVersions(clientName: ClientName): Future[Option[DesiredVersions]] = {
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

  protected def uploadTestedVersions(clientName: ClientName): Route = {
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
      promise.future
    })
  }
}
