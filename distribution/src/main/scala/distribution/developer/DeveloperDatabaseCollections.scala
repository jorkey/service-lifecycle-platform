package distribution.developer

import com.mongodb.client.model.{IndexOptions, Indexes}
import com.vyulabs.update.config.{ClientInfo, InstallProfile}
import com.vyulabs.update.info.{ClientDesiredVersions, ClientFaultReport, ClientServiceState, TestedVersions}
import distribution.DatabaseCollections
import distribution.mongo.MongoDb

import scala.concurrent.ExecutionContext

class DeveloperDatabaseCollections(db: MongoDb)(implicit executionContext: ExecutionContext) extends DatabaseCollections(db) {
  val InstallProfile = db.getOrCreateCollection[InstallProfile]()
  val ClientInfo = db.getOrCreateCollection[ClientInfo]()
  val TestedVersions = db.getOrCreateCollection[TestedVersions]()
  val ClientDesiredVersions = db.getOrCreateCollection[ClientDesiredVersions]()
  val ClientInstalledVersions = db.getOrCreateCollection[ClientDesiredVersions](Some("Installed"))
  val ClientFaultReport = db.getOrCreateCollection[ClientFaultReport]()
  val ClientServiceState = db.getOrCreateCollection[ClientServiceState]()

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

  ClientServiceState.foreach { collection =>
    collection.listIndexes().foreach { indexes =>
      if (indexes.isEmpty) {
        collection.createIndex(Indexes.ascending("clientName"))
        collection.createIndex(Indexes.ascending("instanceId"))
      }
    }
  }
}
