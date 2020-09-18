package distribution.client.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.client.{ClientDistributionDirectory, ClientDistributionWebPaths}
import com.vyulabs.update.info.DesiredVersions
import com.vyulabs.update.lock.SmartFilesLocker
import distribution.utils.{IoUtils, VersionUtils}

trait ClientVersionUtils extends IoUtils with VersionUtils with ClientDistributionWebPaths with SprayJsonSupport {
  def getClientDesiredVersion(serviceName: ServiceName, image: Boolean)
                                 (implicit system: ActorSystem, materializer: Materializer, filesLocker: SmartFilesLocker, dir: ClientDistributionDirectory): Route = {
    val future = parseJsonFileWithLock[DesiredVersions](dir.getDesiredVersionsFile())
    getDesiredVersion(serviceName, future, image)
  }
}
