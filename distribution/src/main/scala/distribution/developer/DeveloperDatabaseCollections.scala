package distribution.developer

import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

import com.mongodb.client.model.{IndexOptions, Indexes}
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.config.{ClientInfo, InstallProfile}
import com.vyulabs.update.distribution.DistributionMain
import com.vyulabs.update.info.{ClientDesiredVersions, ClientFaultReport, ClientServiceState, DirectoryServiceState, TestedVersions}
import distribution.DatabaseCollections
import distribution.mongo.MongoDb
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

class DeveloperDatabaseCollections(db: MongoDb, instanceId: InstanceId, builderDirectory: String,
                                   instanceStateExpireSec: Int)
                                  (implicit executionContext: ExecutionContext) extends DatabaseCollections(db) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  val InstallProfile = db.getOrCreateCollection[InstallProfile]()
  val ClientInfo = db.getOrCreateCollection[ClientInfo]()
  val TestedVersions = db.getOrCreateCollection[TestedVersions]()
  val ClientDesiredVersions = db.getOrCreateCollection[ClientDesiredVersions]()
  val ClientInstalledVersions = db.getOrCreateCollection[ClientDesiredVersions](Some("Installed"))
  val ClientFaultReport = db.getOrCreateCollection[ClientFaultReport]()
  val ClientServiceStates = db.getOrCreateCollection[ClientServiceState]()

  ClientInfo.foreach { collection =>
    collection.listIndexes().foreach { indexes =>
      if (indexes.isEmpty) {
        collection.createIndex(Indexes.ascending("clientName"), new IndexOptions().unique(true))
      }
    }
  }

  TestedVersions.foreach { collection =>
    collection.listIndexes().foreach { indexes =>
      if (indexes.isEmpty) {
        collection.createIndex(Indexes.ascending("profileName"))
      }
    }
  }

  ClientDesiredVersions.foreach { collection =>
    collection.listIndexes().foreach { indexes =>
      if (indexes.isEmpty) {
        collection.createIndex(Indexes.ascending("clientName"))
      }
    }
  }

  ClientInstalledVersions.foreach { collection =>
    collection.listIndexes().foreach { indexes =>
      if (indexes.isEmpty) {
        collection.createIndex(Indexes.ascending("clientName"))
      }
    }
  }

  ClientFaultReport.foreach { collection =>
    collection.listIndexes().foreach { indexes =>
      if (indexes.isEmpty) {
        collection.createIndex(Indexes.ascending("clientName"))
        collection.createIndex(Indexes.ascending("serviceName"))
      }
    }
  }

  ClientServiceStates.foreach { collection =>
    collection.listIndexes().foreach { indexes =>
      if (indexes.isEmpty) {
        collection.createIndex(Indexes.ascending("clientName"))
        collection.createIndex(Indexes.ascending("instanceId"))
        collection.createIndex(Indexes.ascending("date"),
          new IndexOptions().expireAfter(instanceStateExpireSec, TimeUnit.SECONDS))
      }
    }
  }

  ClientServiceStates.foreach { collection => {
    collection.insert(
      ClientServiceState("distribution", instanceId,
        DirectoryServiceState.getOwnInstanceState(Common.DistributionServiceName, new Date(DistributionMain.executionStart))))
    collection.insert(
      ClientServiceState("distribution", instanceId,
        DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File("."))))
    collection.insert(
      ClientServiceState("distribution", instanceId,
        DirectoryServiceState.getServiceInstanceState(Common.BuilderServiceName, new File(builderDirectory))))
    collection.insert(
      ClientServiceState("distribution", instanceId,
        DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File(builderDirectory))))
  }}
}
