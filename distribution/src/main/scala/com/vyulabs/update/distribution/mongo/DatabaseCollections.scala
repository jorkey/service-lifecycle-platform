package com.vyulabs.update.distribution.mongo

import akka.actor.ActorSystem
import akka.event.Logging
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.{IndexOptions, _}
import com.vyulabs.update.common.config.{BuildServiceConfig, GitConfig, NamedStringValue, Repository}
import com.vyulabs.update.common.info.{DistributionProviderInfo, _}
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.common.accounts.{ConsumerAccountProperties, PasswordHash, ServerAccountInfo, UserAccountProperties}
import com.vyulabs.update.distribution.graphql.utils.{TaskInfo, TaskParameter}
import org.bson.BsonDocument
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.IterableCodecProvider
import org.mongodb.scala.bson.codecs.Macros._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class DatabaseCollections(db: MongoDb, logsDb: MongoDb,
                          serviceStatesExpireTimeout: FiniteDuration,
                          taskLogExpirationTimeout: FiniteDuration,
                          createIndices: Boolean)
                         (implicit system: ActorSystem, executionContext: ExecutionContext) {
  private implicit val log = Logging(system, this.getClass)

  private implicit def codecRegistry = fromRegistries(fromProviders(MongoClientSettings.getDefaultCodecRegistry(),
    IterableCodecProvider.apply,
    classOf[UserAccountProperties],
    classOf[ConsumerAccountProperties],
    classOf[ServerAccountInfo],
    classOf[PasswordHash],
    classOf[GitConfig],
    classOf[Repository],
    classOf[ClientVersion],
    classOf[ClientDistributionVersion],
    classOf[DeveloperVersion],
    classOf[DeveloperDistributionVersion],
    classOf[BuildInfo],
    classOf[ClientVersionInfo],
    classOf[DeveloperDesiredVersion],
    classOf[DeveloperDesiredVersions],
    classOf[DeveloperVersionInfo],
    classOf[ClientDesiredVersion],
    classOf[ClientDesiredVersions],
    classOf[InstalledDesiredVersions],
    classOf[InstallInfo],
    classOf[DistributionProviderInfo],
    classOf[ServicesProfile],
    classOf[DistributionInstanceState],
    classOf[AddressedInstanceState],
    classOf[ServerBuildServiceState],
    classOf[InstanceState],
    classOf[UpdateError],
    classOf[ServiceLogLine],
    classOf[DistributionFaultReport],
    classOf[TestedVersions],
    classOf[LogLine],
    classOf[UploadStatus],
    classOf[UploadStatusDocument],
    classOf[FileInfo],
    classOf[FaultInfo],
    classOf[TaskInfo],
    classOf[TaskParameter],
    classOf[ServiceFaultReport],
    classOf[BuildServiceConfig],
    classOf[NamedStringValue],
    fromCodecs(FiniteDurationCodec, URLCodec)
  ))

  val Accounts = new SequencedCollection[ServerAccountInfo]("accounts",
    for {
      collection <- db.getOrCreateCollection[BsonDocument]("accounts")
    } yield {
      if (createIndices) {
        collection.createIndex(Indexes.ascending("type", "_archiveTime"))
        collection.createIndex(Indexes.ascending("account", "_archiveTime"),
          new IndexOptions().unique(true))
      } else Future()
      collection
    }, createIndices = createIndices)

  val Developer_Services = new SequencedCollection[BuildServiceConfig]("developer.services", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.services")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("service", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
    collection
  }, createIndices = createIndices)

  val Developer_Versions = new SequencedCollection[DeveloperVersionInfo]("developer.versions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.versions")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("service", "version", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
    collection
  }, createIndices = createIndices)

  val Developer_DesiredVersions = new SequencedCollection[DeveloperDesiredVersions]("developer.desiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.desiredVersions")
  } yield collection, createIndices = createIndices)

  val Developer_ServiceProfiles = new SequencedCollection[ServicesProfile]("developer.serviceProfiles", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.serviceProfiles")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("profile", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
    collection
  }, createIndices = createIndices)

  val Developer_TestedVersions = new SequencedCollection[TestedVersions]("developer.testedVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.testedVersions")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("profile")) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("consumerDistribution", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
    collection
  }, createIndices = createIndices)

  val Client_Services = new SequencedCollection[BuildServiceConfig]("client.services", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.services")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("service", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
    collection
  }, createIndices = createIndices)

  val Client_Versions = new SequencedCollection[ClientVersionInfo]("client.versions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.versions")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("service", "version", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
    collection
  }, createIndices = createIndices)

  val Client_DesiredVersions = new SequencedCollection[ClientDesiredVersions]("client.desiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.desiredVersions")
  } yield collection, createIndices = createIndices)

  val Client_ProvidersInfo = new SequencedCollection[DistributionProviderInfo]("client.providers", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.providers")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("distribution", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
    collection
  }, createIndices = createIndices)

  val Consumers_InstalledDesiredVersions = new SequencedCollection[InstalledDesiredVersions]("consumers.installedDesiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("consumers.installedDesiredVersions")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
    collection
  }, createIndices = createIndices)

  val State_Builds = new SequencedCollection[ServerBuildServiceState]("state.builds", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.builds")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("service", "targets")) else Future()
    collection
  }, createIndices = createIndices)

  val State_Instances = new SequencedCollection[DistributionInstanceState]("state.instances", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.instances")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("instance")) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("service")) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("directory")) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("state.time"), new IndexOptions()
      .expireAfter(serviceStatesExpireTimeout.length, serviceStatesExpireTimeout.unit)) else Future()
    collection.dropItems()
    collection
  }, createIndices = createIndices)

  val State_UploadStatus = for {
    collection <- db.getOrCreateCollection[UploadStatusDocument]("state.uploadStatus")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("component", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
    collection
  }

  val Log_Lines = new SequencedCollection[ServiceLogLine]("log.lines", for {
    collection <- logsDb.getOrCreateCollection[BsonDocument]("log.lines")
  } yield {
    if (createIndices) collection.createIndex(Indexes.compoundIndex(
      Indexes.ascending("service"), Indexes.ascending("instance"), Indexes.ascending("directory"),
      Indexes.ascending("process"), Indexes.ascending("level"),
      Indexes.ascending("_sequence"), Indexes.ascending("time"))) else Future()
    if (createIndices) collection.createIndex(Indexes.compoundIndex(
      Indexes.ascending("service"), Indexes.ascending("instance"), Indexes.ascending("directory"),
      Indexes.ascending("process"),
      Indexes.ascending("_sequence"), Indexes.ascending("time"))) else Future()
    if (createIndices) collection.createIndex(Indexes.compoundIndex(
      Indexes.ascending("service"), Indexes.ascending("instance"), Indexes.ascending("directory"),
      Indexes.ascending("_sequence"), Indexes.ascending("time"))) else Future()
    if (createIndices) collection.createIndex(Indexes.compoundIndex(
      Indexes.ascending("service"), Indexes.ascending("instance"),
      Indexes.ascending("_sequence"), Indexes.ascending("time"))) else Future()
    if (createIndices) collection.createIndex(Indexes.compoundIndex(
      Indexes.ascending("service"),
      Indexes.ascending("_sequence"), Indexes.ascending("time"))) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("task")) else Future()
    if (createIndices) collection.createIndex(Indexes.compoundIndex(
      Indexes.ascending("service"), Indexes.text("message"),
      Indexes.ascending("_sequence"), Indexes.ascending("time"))) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("expireTime"), new IndexOptions()
      .expireAfter(0, TimeUnit.SECONDS)) else Future()
    if (createIndices) collection.createIndex(Indexes.descending("_sequence")) else Future()
    collection
  }, modifiable = false, createIndices = false)

  val Tasks_Info = new SequencedCollection[TaskInfo]("tasks.info", for {
    collection <- db.getOrCreateCollection[BsonDocument]("tasks.info")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("task")) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("type")) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("services")) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("creationTime"), new IndexOptions()
      .expireAfter(taskLogExpirationTimeout.length, taskLogExpirationTimeout.unit)) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("terminationStatus")) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("_sequence")) else Future()
    collection
  }, createIndices = false)

  val Faults_ReportsInfo = new SequencedCollection[DistributionFaultReport]("faults.reports", for {
    collection <- db.getOrCreateCollection[BsonDocument]("faults.reports")
  } yield {
    if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("fault", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("info.service")) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("info.time")) else Future()
    if (createIndices) collection.createIndex(Indexes.ascending("_sequence")) else Future()
    collection
  }, createIndices = createIndices)

  def init()(implicit executionContext: ExecutionContext): Future[Unit] = {
    val filters = Filters.eq("account", "admin")
    for {
      adminRecords <- Accounts.find(filters)
    } yield {
      if (adminRecords.isEmpty) {
        Accounts.insert(ServerAccountInfo(ServerAccountInfo.TypeUser, "admin", "Administrator",
          AccountRole.Administrator.toString, Some(PasswordHash("admin")),
          Some(UserAccountProperties(None, Seq.empty)), None))
      } else {
        Future()
      }
    }
  }
}
