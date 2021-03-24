package com.vyulabs.update.common.distribution.client

import com.vyulabs.update.common.common.Common.{DistributionName, FaultId, ServiceName}
import com.vyulabs.update.common.distribution.client.graphql.GraphqlRequest
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.JsonReader

import java.io.File
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}

class SyncDistributionClient[Source[_]](client: DistributionClient[Source], waitDuration: FiniteDuration)(implicit executionContext: ExecutionContext) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  def available(): Boolean = {
    result(client.ping()).isDefined
  }

  def login(): Boolean = {
    result(client.login()).isDefined
  }

  def graphqlRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Option[Response] = {
    result(client.graphqlRequest(request))
  }

  def graphqlSubRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Option[Source[Response]] = {
    result(client.graphqlSubRequest(request))
  }

  def downloadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File): Boolean = {
    result(client.downloadDeveloperVersionImage(serviceName, version, file)).isDefined
  }

  def downloadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File): Boolean = {
    result(client.downloadClientVersionImage(serviceName, version, file)).isDefined
  }

  def uploadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File): Boolean = {
    result(client.uploadDeveloperVersionImage(serviceName, version, file)).isDefined
  }

  def uploadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File): Boolean = {
    result(client.uploadClientVersionImage(serviceName, version, file)).isDefined
  }

  def uploadFaultReport(faultId: FaultId, faultReportFile: File): Boolean = {
    result(client.uploadFaultReport(faultId, faultReportFile)).isDefined
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