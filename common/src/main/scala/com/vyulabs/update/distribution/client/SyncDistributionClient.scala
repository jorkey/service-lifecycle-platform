package com.vyulabs.update.distribution.client

import com.vyulabs.update.common.Common.{DistributionName, FaultId, ServiceName}
import com.vyulabs.update.distribution.client.graphql.GraphqlRequest
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.JsonReader

import java.io.File
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}

class SyncDistributionClient(client: DistributionClient, waitDuration: FiniteDuration)(implicit executionContext: ExecutionContext) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def distributionName = client.distributionName

  def getDistributionVersion(): Option[ClientDistributionVersion] = {
    result(client.getDistributionVersion()).flatten
  }

  def graphqlRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Option[Response]= {
    result(client.graphqlRequest(request))
  }

  def downloadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File): Boolean = {
    result(client.downloadDeveloperVersionImage(serviceName, version, file))
    true
  }

  def downloadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File): Boolean = {
    result(client.downloadClientVersionImage(serviceName, version, file))
    true
  }

  def uploadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File): Boolean = {
    result(client.uploadDeveloperVersionImage(serviceName, version, file))
    true
  }

  def uploadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File): Boolean = {
    result(client.uploadClientVersionImage(serviceName, version, file))
    true
  }

  def uploadFaultReport(faultId: FaultId, faultReportFile: File): Boolean = {
    result(client.uploadFaultReport(faultId, faultReportFile))
    true
  }

  private def result[T](awaitable: Awaitable[T])(implicit log: Logger): Option[T] = {
    try {
      Some(Await.result(awaitable, waitDuration))
    } catch {
      case e: Exception =>
        log.error(e.getMessage)
        None
    }
  }
}
