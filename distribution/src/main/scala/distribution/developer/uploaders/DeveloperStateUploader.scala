package distribution.developer.uploaders

import java.io.IOException

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, failWith}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.vyulabs.update.common.Common.ClientName
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.state.InstancesState
import com.vyulabs.update.utils.IOUtils
import org.slf4j.{Logger, LoggerFactory}
import spray.json._

import com.vyulabs.update.state.InstancesStateJson._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 18.12.19.
  * Copyright FanDate, Inc.
  */
class DeveloperStateUploader(dir: DeveloperDistributionDirectory)
                            (implicit filesLocker: SmartFilesLocker, materializer: Materializer) extends Thread {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val expireDiedInstanceStateTime = 24 * 60 * 60 * 1000L

  def receiveInstancesState(clientName: ClientName, instancesState: InstancesState): Route = {
    // TODO
    log.info(s"Receive instances state of client ${clientName}")
    val statesFile = dir.getInstancesStateFile(clientName)
    val oldStates = IOUtils.readFileToJsonWithLock(statesFile).map(_.convertTo[InstancesState])
    for (oldStates <- oldStates) {
      val newDeadStates = oldStates.states.filterKeys(!instancesState.states.contains(_))
      val deadStatesFile = dir.getDeadInstancesStateFile(clientName)
      val deadStates = IOUtils.readFileToJsonWithLock(deadStatesFile).map(_.convertTo[InstancesState]) match {
        case Some(deadInstancesState) =>
          deadInstancesState.states
            .filterKeys(!instancesState.states.contains(_))
            .filter(state => (System.currentTimeMillis() - state._2.startDate.getTime) < expireDiedInstanceStateTime)
        case None =>
          Map.empty
      }
      if (!IOUtils.writeJsonToFileWithLock(deadStatesFile, InstancesState(deadStates ++ newDeadStates).toJson)) {
        log.error(s"Can't write ${deadStatesFile}")
      }
    }
    if (!IOUtils.writeJsonToFileWithLock(dir.getInstancesStateFile(clientName), instancesState.toJson)) {
      failWith(new IOException(s"Error of writing instances state"))
    } else {
      complete(StatusCodes.OK)
    }
  }

  override def run(): Unit = {

  }
}
