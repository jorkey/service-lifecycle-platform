package distribution.developer

import com.vyulabs.update.config.{ClientInfo, InstallProfile}
import com.vyulabs.update.info.{ClientDesiredVersions, ClientFaultReport, ClientServiceState, TestedVersions}
import distribution.DatabaseCollections
import distribution.mongo.MongoDb

import scala.concurrent.ExecutionContext

class DeveloperDatabaseCollections(db: MongoDb)(implicit executionContext: ExecutionContext) extends DatabaseCollections(db) {
  val ClientInfo = db.getOrCreateCollection[ClientInfo]()
  val TestedVersions = db.getOrCreateCollection[TestedVersions]()
  val InstallProfile = db.getOrCreateCollection[InstallProfile]()
  val ClientDesiredVersions = db.getOrCreateCollection[ClientDesiredVersions]()
  val ClientInstalledVersions = db.getOrCreateCollection[ClientDesiredVersions](Some("Installed"))
  val ClientFaultReport = db.getOrCreateCollection[ClientFaultReport]()
  val ClientServiceState = db.getOrCreateCollection[ClientServiceState]()
}
