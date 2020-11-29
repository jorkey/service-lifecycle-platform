package com.vyulabs.update.distribution.client

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{DistributionName, FaultId, ServiceName}
import com.vyulabs.update.distribution.DistributionWebPaths._
import com.vyulabs.update.utils.ZipUtils
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.LoggerFactory
import AdministratorGraphqlCoder._
import spray.json.JsonReader
import spray.json.DefaultJsonProtocol._

class DistributionClient(distributionName: DistributionName, client: HttpJavaClient)  {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def graphqlQuery[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Option[Response]= {
    client.graphqlRequest(request)
  }

  def graphqlMutation(request: GraphqlRequest[Boolean]): Boolean = {
    client.graphqlRequest(request).getOrElse(false)
  }

  def getDistributionVersion(): Option[ClientDistributionVersion] = {
    val state = client.graphqlRequest(administratorQueries.getServiceStates(Some(distributionName), Some(Common.DistributionServiceName), None, None))
      .getOrElse { return None }
      .headOption.getOrElse { return None }
    state.instance.service.version
  }

  def waitForServerUpdated(desiredVersion: ClientDistributionVersion, waitingTimeoutSec: Int): Boolean = {
    log.info(s"Wait for distribution server updated")
    for (_ <- 0 until waitingTimeoutSec) {
      if (client.exists(client.makeUrl(pingPath))) {
        getDistributionVersion() match {
          case Some(version) =>
            if (version == desiredVersion) {
              log.info(s"Distribution server is updated")
              return true
            }
          case None =>
            return false
        }
      }
      Thread.sleep(1000)
    }
    log.error(s"Timeout of waiting for distribution server become available")
    false
  }

  def downloadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File): Boolean = {
    client.download(client.makeUrl(loadPathPrefix, developerVersionImagePath, serviceName, version.toString), file)
  }

  def downloadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File): Boolean = {
    client.download(client.makeUrl(loadPathPrefix, clientVersionImagePath, serviceName, version.toString), file)
  }

  def uploadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, buildDir: File): Boolean = {
    uploadVersionImageFromDirectory(client.makeUrl(loadPathPrefix, developerVersionImagePath, serviceName, version.toString), buildDir)
  }

  def uploadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, buildDir: File): Boolean = {
    uploadVersionImageFromDirectory(client.makeUrl(loadPathPrefix, clientVersionImagePath, serviceName, version.toString), buildDir)
  }

  def uploadFaultReport(faultId: FaultId, faultReportFile: File): Boolean = {
    client.upload(client.makeUrl(loadPathPrefix, faultReportPath, faultId), versionImageField, faultReportFile)
  }

  private def uploadVersionImageFromDirectory(url: URL, buildDir: File): Boolean = {
    val imageTmpFile = File.createTempFile("build", ".zip")
    try {
      if (!ZipUtils.zip(imageTmpFile, buildDir)) {
        log.error("Can't zip build directory")
        return false
      }
      client.upload(url, versionImageField, imageTmpFile)
    } finally {
      imageTmpFile.delete()
    }
  }
}
