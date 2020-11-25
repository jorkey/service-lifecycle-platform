package com.vyulabs.update.distribution.client

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{DistributionName, FaultId, ServiceName}
import com.vyulabs.update.distribution.DistributionWebPaths._
import com.vyulabs.update.utils.ZipUtils
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.LoggerFactory

class AdministratorClient(distributionName: DistributionName, httpClient: HttpClient) extends AdministratorQueriesClient with AdministratorMutationsClient {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  protected override val client: GraphqlClient = new GraphqlClient {
    override protected val client: HttpClient = httpClient
  }

  def getDistributionVersion(): Option[ClientDistributionVersion] = {
    val state = getServicesState(Some(distributionName), Some(Common.DistributionServiceName), None, None)
      .getOrElse { return None }
      .headOption.getOrElse { return None }
    state.instance.service.version
  }

  def waitForServerUpdated(desiredVersion: DeveloperDistributionVersion): Boolean = {
    log.info(s"Wait for distribution server updated")
    Thread.sleep(5000)
    for (_ <- 0 until 25) {
      if (httpClient.exists(httpClient.makeUrl())) {
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
    httpClient.download(httpClient.makeUrl(developerVersionImagePath, serviceName, version.toString), file)
  }

  def downloadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File): Boolean = {
    httpClient.download(httpClient.makeUrl(clientVersionImagePath, serviceName, version.toString), file)
  }

  def uploadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, buildDir: File): Boolean = {
    uploadVersionImageFromDirectory(httpClient.makeUrl(developerVersionImagePath, serviceName, version.toString), buildDir)
  }

  def uploadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, buildDir: File): Boolean = {
    uploadVersionImageFromDirectory(httpClient.makeUrl(clientVersionImagePath, serviceName, version.toString), buildDir)
  }

  private def uploadVersionImageFromDirectory(url: URL, buildDir: File): Boolean = {
    val imageTmpFile = File.createTempFile("build", ".zip")
    try {
      if (!ZipUtils.zip(imageTmpFile, buildDir)) {
        log.error("Can't zip build directory")
        return false
      }
      httpClient.upload(url, versionImageField, imageTmpFile)
    } finally {
      imageTmpFile.delete()
    }
  }
}

class DistributionClient(httpClient: HttpClient) extends DistributionQueriesClient with DistributionMutationsClient {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  protected override val client: GraphqlClient = new GraphqlClient {
    override protected val client: HttpClient = httpClient
  }

  def downloadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File): Boolean = {
    httpClient.download(httpClient.makeUrl(developerVersionImagePath, serviceName, version.toString), file)
  }
}

class ServiceClient(httpClient: HttpClient) extends ServiceQueriesClient with ServiceMutationsClient {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  protected override val client: GraphqlClient = new GraphqlClient {
    override protected val client: HttpClient = httpClient
  }

  def downloadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File): Boolean = {
    httpClient.download(httpClient.makeUrl(clientVersionImagePath, serviceName, version.toString), file)
  }

  def uploadFaultReport(faultId: FaultId, faultReportFile: File): Boolean = {
    httpClient.upload(httpClient.makeUrl(faultReportPath, faultId), versionImageField, faultReportFile)
  }
}
