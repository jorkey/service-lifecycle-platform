package distribution.client.uploaders

import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FutureDirectives
import akka.stream.Materializer
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{InstanceId, ServiceDirectory}
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectoryClient, DeveloperDistributionWebPaths}
import com.vyulabs.update.state.{InstancesState, ProfiledServiceName, ServiceState, ServicesState}
import com.vyulabs.update.distribution.client.ClientDistributionDirectory
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import ServicesState._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 22.05.19.
  * Copyright FanDate, Inc.
  */
class ClientStateUploader(dir: ClientDistributionDirectory, developerDirectoryUrl: URL, instanceId: InstanceId)
                         (implicit system: ActorSystem, materializer: Materializer)
      extends Thread with DeveloperDistributionWebPaths with FutureDirectives with SprayJsonSupport { self =>
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val developerDirectory = new DeveloperDistributionDirectoryClient(developerDirectoryUrl)

  private var instancesStates = Map.empty[InstanceId, Map[ServiceDirectory, Map[ProfiledServiceName, ServiceState]]]
  private var statesToUpload = Map.empty[InstanceId, Map[ServiceDirectory, Map[ProfiledServiceName, ServiceState]]]

  private val expirationPeriod = FiniteDuration.apply(1, TimeUnit.MINUTES).toMillis
  private val uploadInterval = 10000

  private var lastStatesUploadTime = 0L
  private var desiredVersionsLmt = 0L

  private var stopping = false

  def receiveState(instanceId: InstanceId, servicesState: ServicesState): Route = {
    self.synchronized {
      instancesStates += (instanceId -> (instancesStates.getOrElse(instanceId, Map.empty) ++ servicesState.state))
      statesToUpload += (instanceId -> (statesToUpload.getOrElse(instanceId, Map.empty) ++ servicesState.state))
    }
    complete(StatusCodes.OK)
  }

  def getInstanceState(instanceId: InstanceId): Route = {
    self.synchronized {
      instancesStates.get(instanceId) match {
        case Some(state) =>
          complete(ServicesState(state))
        case None =>
          complete(StatusCodes.NotFound)
      }
    }
  }

  def close(): Unit = {
    self.synchronized {
      stopping = true
      notify()
    }
    join()
  }

  override def run(): Unit = {
    log.info("State uploader started")
    try {
      while (true) {
        val pause = lastStatesUploadTime + uploadInterval - System.currentTimeMillis()
        if (pause > 0) {
          self.synchronized {
            if (stopping) {
              return
            }
            wait(pause)
            if (stopping) {
              return
            }
          }
        }
        if (!stopping) {
          try {
            removeOldStates()
            mayBeUploadInstalledDesiredVersions()

            val states = self.synchronized {
              val states = statesToUpload
              statesToUpload = Map.empty
              InstancesState(states.mapValues(ServicesState(_)))
            }.merge(InstancesState(Map.empty + (instanceId ->
              ServicesState.getOwnInstanceState(Common.DistributionServiceName)
                .merge(ServicesState.getServiceInstanceState(new File("."), Common.ScriptsServiceName))
                .merge(ServicesState.getServiceInstanceState(new File("../install"), Common.InstallerServiceName)))))
            log.debug("Upload instances state to developer distribution server")
            if (!developerDirectory.uploadInstancesState(states)) {
              log.error("Can't upload instances state")
            }
          } catch {
            case ex: Exception =>
              log.error("State uploader exception", ex)
          }
          lastStatesUploadTime = System.currentTimeMillis()
        }
      }
    } catch {
      case ex: Exception =>
        log.error(s"State uploader thread is failed", ex)
    }
  }

  private def removeOldStates(): Unit = {
    self.synchronized {
      instancesStates = instancesStates.map { case (instanceId, state) =>
        (instanceId, state.mapValues ( state =>
          state.filter { case (_, state) =>
            System.currentTimeMillis() - state.date.getTime < expirationPeriod
          }))
      }.filter { case (_, state) =>
        !state.isEmpty
      }
    }
  }

  private def mayBeUploadInstalledDesiredVersions(): Unit = {
    val file = dir.getDesiredVersionsFile()
    val lmt = file.lastModified()
    if (lmt != desiredVersionsLmt) {
      if (developerDirectory.uploadInstalledDesiredVersions(file)) {
        desiredVersionsLmt = lmt
      }
    }
  }
}