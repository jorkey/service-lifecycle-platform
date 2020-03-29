package distribution.developer.uploaders

import java.io.IOException

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, failWith}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.vyulabs.update.common.Common.ClientName
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectory
import com.vyulabs.update.state.InstancesState
import com.vyulabs.update.utils.UpdateUtils
import org.slf4j.{Logger, LoggerFactory}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 18.12.19.
  * Copyright FanDate, Inc.
  */
class DeveloperStateUploader(dir: DeveloperDistributionDirectory)(implicit materializer: Materializer) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val expireDiedInstanceStateTime = 24 * 60 * 60 * 1000L

  def receiveInstancesState(clientName: ClientName, instancesState: InstancesState): Route = {
    log.info(s"Receive instances state of client ${clientName}")
    val statesFile = dir.getInstancesStateFile(clientName)
    val oldStates = UpdateUtils.parseConfigFile(statesFile).map(InstancesState(_))
    for (oldStates <- oldStates) {
      val newDeadStates = oldStates.states.filterKeys(!instancesState.states.contains(_))
      val deadStatesFile = dir.getDeadInstancesStateFile(clientName)
      val deadStates = UpdateUtils.parseConfigFile(deadStatesFile).map(InstancesState(_)) match {
        case Some(deadInstancesState) =>
          deadInstancesState.states
            .filterKeys(!instancesState.states.contains(_))
            .filter(state => (System.currentTimeMillis() - state._2.startDate.getTime) < expireDiedInstanceStateTime)
        case None =>
          Map.empty
      }
      if (!UpdateUtils.writeConfigFile(deadStatesFile, InstancesState(deadStates ++ newDeadStates).toConfig())) {
        log.error(s"Can't write ${deadStatesFile}")
      }
    }
    if (!UpdateUtils.writeConfigFile(dir.getInstancesStateFile(clientName), instancesState.toConfig())) {
      failWith(new IOException(s"Error of writing instances state"))
    } else {
      complete(StatusCodes.OK)
    }
  }
}
