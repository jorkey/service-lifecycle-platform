package com.vyulabs.update.distribution.graphql.utils

import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.DistributionGraphqlCoder.{distributionMutations, distributionQueries}
import com.vyulabs.update.common.info.{ClientDesiredVersions, DeveloperDesiredVersions}
import com.vyulabs.update.distribution.client.AkkaHttpClient.AkkaSource
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}

trait TestedVersionsUtils {
  val config: DistributionConfig

  protected implicit val executionContext: ExecutionContext

  protected val clientVersionUtils: ClientVersionUtils

  def signVersionsAsTested(developerDistributionClient: DistributionClient[AkkaSource])
                          (implicit log: Logger): Future[Boolean] = {
    for {
      clientDesiredVersions <- clientVersionUtils.getClientDesiredVersions().map(ClientDesiredVersions.toMap(_))
      developerDesiredVersions <- developerDistributionClient.graphqlRequest(distributionQueries.getDeveloperDesiredVersions()).map(DeveloperDesiredVersions.toMap(_))
      result <- {
        if (!clientDesiredVersions.filter(_._2.distribution == config.distribution)
          .mapValues(_.original).equals(developerDesiredVersions)) {
          log.error("Client versions are different from developer versions:")
          clientDesiredVersions foreach {
            case (service, clientVersion) =>
              developerDesiredVersions.get(service) match {
                case Some(developerVersion) if developerVersion != clientVersion.original =>
                  log.info(s"  service ${service} version ${clientVersion} != ${developerVersion}")
                case _ =>
              }
          }
          developerDesiredVersions foreach {
            case (service, developerVersion) =>
              if (!clientDesiredVersions.get(service).isDefined) {
                log.info(s"  service ${service} version ${developerVersion} is not installed")
              }
          }
          clientDesiredVersions foreach {
            case (service, _) =>
              if (!developerDesiredVersions.get(service).isDefined) {
                log.info(s"  service ${service} is not the developer service")
              }
          }
          Future(false)
        } else {
          developerDistributionClient.graphqlRequest(distributionMutations.setTestedVersions(
            DeveloperDesiredVersions.fromMap(clientDesiredVersions.mapValues(_.original))))
        }
      }
    } yield result
  }
}
