package com.vyulabs.update.distribution.graphql.utils

import com.vyulabs.update.common.distribution.client.SyncDistributionClient
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.administratorQueries
import com.vyulabs.update.common.distribution.client.graphql.DistributionGraphqlCoder.{distributionMutations, distributionQueries}
import com.vyulabs.update.common.info.{ClientDesiredVersions, DeveloperDesiredVersions}
import org.slf4j.Logger

class TestedVersionsUtils {
  def signVersionsAsTested(distributionClient: SyncDistributionClient, developerDistribution: SyncDistributionClient)
                          (implicit log: Logger): Boolean = {
    val clientDesiredVersionsMap = distributionClient.graphqlRequest(administratorQueries.getClientDesiredVersions())
      .map(ClientDesiredVersions.toMap(_)).getOrElse {
      log.error("Error of getting client desired versions")
      return false
    }
    val developerDesiredVersionsMap = developerDistribution.graphqlRequest(distributionQueries.getDesiredVersions())
      .map(DeveloperDesiredVersions.toMap(_)).getOrElse {
      log.error("Error of getting developer desired versions")
      return false
    }
    if (!clientDesiredVersionsMap.filter(_._2.distributionName == developerDistribution.distributionName)
      .mapValues(_.original()).equals(developerDesiredVersionsMap)) {
      log.error("Client versions are different from developer versions:")
      clientDesiredVersionsMap foreach {
        case (serviceName, clientVersion) =>
          developerDesiredVersionsMap.get(serviceName) match {
            case Some(developerVersion) if developerVersion != clientVersion.original() =>
              log.info(s"  service ${serviceName} version ${clientVersion} != ${developerVersion}")
            case _ =>
          }
      }
      developerDesiredVersionsMap foreach {
        case (serviceName, developerVersion) =>
          if (!clientDesiredVersionsMap.get(serviceName).isDefined) {
            log.info(s"  service ${serviceName} version ${developerVersion} is not installed")
          }
      }
      clientDesiredVersionsMap foreach {
        case (serviceName, _) =>
          if (!developerDesiredVersionsMap.get(serviceName).isDefined) {
            log.info(s"  service ${serviceName} is not the developer service")
          }
      }
      return false
    }
    if (!developerDistribution.graphqlRequest(distributionMutations.setTestedVersions(
      DeveloperDesiredVersions.fromMap(clientDesiredVersionsMap.mapValues(_.original())))).getOrElse(false)) {
      log.error("Error of uploading desired versions to developer")
      return false
    }
    true
  }
}
