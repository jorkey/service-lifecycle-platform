package distribution.developer.uploaders

import akka.stream.Materializer
import com.vyulabs.update.common.Common.ClientName
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.state.VmInstancesState
import com.vyulabs.update.utils.IOUtils
import com.vyulabs.update.state.VmInstancesStateJson._

import org.slf4j.LoggerFactory
import spray.json._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 18.12.19.
  * Copyright FanDate, Inc.
  */
class DeveloperStateUploader(dir: DeveloperDistributionDirectory)
                            (implicit filesLocker: SmartFilesLocker, materializer: Materializer) extends Thread { self =>
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private var client2instancesState = Map.empty[ClientName, VmInstancesState]
  private var stopping = false

  private val expireDiedInstanceStateTime = 24 * 60 * 60 * 1000L

  def close(): Unit = {
    self.synchronized {
      stopping = true
      notify()
    }
    join()
  }

  def receiveInstancesState(clientName: ClientName, instancesState: VmInstancesState): Unit = {
    log.info(s"Receive instances state of client ${clientName}")
    self.synchronized {
      client2instancesState += (clientName -> instancesState)
      notify()
    }
  }

  override def run(): Unit = {
    log.info("State uploader started")
    try {
      while (true) {
        val states = self.synchronized {
          if (stopping) {
            return
          }
          wait()
          if (stopping) {
            return
          }
          client2instancesState.foldLeft(Map.empty[ClientName, VmInstancesState])((m, e) => m + (e._1 -> e._2))
        }
        states.foreach { case (clientName, instancesState) => {
            log.info(s"Process instances state of client ${clientName}")
            try {
              val statesFile = dir.getInstancesStateFile(clientName)
              val oldStates = IOUtils.readFileToJsonWithLock(statesFile).map(_.convertTo[VmInstancesState])
              for (oldStates <- oldStates) {
                val newDeadStates = oldStates.state.filterKeys(!instancesState.state.contains(_))
                val deadStatesFile = dir.getDeadInstancesStateFile(clientName)
                val deadStates = IOUtils.readFileToJsonWithLock(deadStatesFile).map(_.convertTo[VmInstancesState]) match {
                  case Some(deadInstancesState) =>
                    deadInstancesState.state
                      .filterKeys(!instancesState.state.contains(_))
                      .mapValues(_.filter { case (_, updaterState) =>
                        (System.currentTimeMillis() - updaterState.date.getTime) < expireDiedInstanceStateTime
                      })
                      .filter(!_._2.isEmpty)
                  case None =>
                    Map.empty
                }
                if (!IOUtils.writeJsonToFileWithLock(deadStatesFile, VmInstancesState(deadStates ++ newDeadStates).toJson)) {
                  log.error(s"Can't write ${deadStatesFile}")
                }
              }
              if (!IOUtils.writeJsonToFileWithLock(dir.getInstancesStateFile(clientName), instancesState.toJson)) {
                log.error("Error of writing instances state")
              }
            } catch {
              case ex: Exception =>
                log.error(s"Process instances state if client ${clientName} is failed", ex)
            }
          }
        }
      }
    } catch {
      case ex: Exception =>
        log.error(s"Uploader thread is failed", ex)
    }
  }
}
