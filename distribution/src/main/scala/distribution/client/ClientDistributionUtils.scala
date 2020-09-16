package distribution.client

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.DistributionUtils
import com.vyulabs.update.distribution.client.{ClientDistributionDirectory, ClientDistributionWebPaths}
import com.vyulabs.update.info.DesiredVersions
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.UsersCredentials

class ClientDistributionUtils(dir: ClientDistributionDirectory, usersCredentials: UsersCredentials)
                              (implicit filesLocker: SmartFilesLocker, system: ActorSystem, materializer: Materializer)
    extends DistributionUtils(dir, usersCredentials) with ClientDistributionWebPaths with SprayJsonSupport {

  protected def getDesiredVersion(serviceName: ServiceName, image: Boolean): Route = {
    val future = parseJsonFileWithLock[DesiredVersions](dir.getDesiredVersionsFile())
    getDesiredVersion(serviceName, future, image)
  }
}
