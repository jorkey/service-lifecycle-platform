package distribution.developer

import com.mongodb.client.model.{IndexOptions, Indexes}
import com.vyulabs.update.config.{ClientInfo, InstallProfile}
import com.vyulabs.update.info.{ClientDesiredVersion, ClientFaultReport, ClientServiceState, TestedVersion}
import distribution.DatabaseCollections
import distribution.mongo.MongoDb

import scala.concurrent.ExecutionContext

class DeveloperDatabaseCollections(db: MongoDb)(implicit executionContext: ExecutionContext) extends DatabaseCollections(db) {
  val InstallProfile = db.getOrCreateCollection[InstallProfile]()
  val ClientInfo = db.getOrCreateCollection[ClientInfo]()
  val TestedVersion = db.getOrCreateCollection[TestedVersion]()
  val ClientDesiredVersion = db.getOrCreateCollection[ClientDesiredVersion]()
  val ClientInstalledVersion = db.getOrCreateCollection[ClientDesiredVersion](Some("Installed"))
  val ClientFaultReport = db.getOrCreateCollection[ClientFaultReport]()
  val ClientServiceState = db.getOrCreateCollection[ClientServiceState]()

  ClientInfo.foreach { collection =>
    collection.listIndexes().foreach { indexes =>
      if (indexes.isEmpty) {
        collection.createIndex(Indexes.ascending("clientName"), new IndexOptions().unique(true))
      }
    }
  }

  TestedVersion.foreach { collection =>
    collection.listIndexes().foreach { indexes =>
      if (indexes.isEmpty) {
        collection.createIndex(Indexes.ascending("profileName"))
        collection.createIndex(Indexes.ascending("serviceName"))
      }
    }
  }

  ClientDesiredVersion.foreach { collection =>
    collection.listIndexes().foreach { indexes =>
      if (indexes.isEmpty) {
        collection.createIndex(Indexes.ascending("clientName"))
        collection.createIndex(Indexes.ascending("serviceName"))
      }
    }
  }

  ClientInstalledVersion.foreach { collection =>
    collection.listIndexes().foreach { indexes =>
      if (indexes.isEmpty) {
        collection.createIndex(Indexes.ascending("clientName"))
        collection.createIndex(Indexes.ascending("serviceName"))
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
