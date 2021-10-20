package com.vyulabs.update.distribution.mongo

import akka.actor.ActorSystem
import akka.event.Logging
import com.mongodb.MongoClientSettings
import com.mongodb.client.model._
import com.vyulabs.update.common.config.{GitConfig, ServiceSourcesConfig, SourceConfig}
import com.vyulabs.update.common.info.{DistributionProviderInfo, _}
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.common.accounts.{ConsumerAccountProperties, PasswordHash, ServerAccountInfo, UserAccountProperties}
import com.vyulabs.update.distribution.graphql.utils.TaskInfo
import org.bson.BsonDocument
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.IterableCodecProvider
import org.mongodb.scala.bson.codecs.Macros._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class DatabaseCollections(db: MongoDb, instanceStateExpireTimeout: FiniteDuration, createIndices: Boolean)
                         (implicit system: ActorSystem, executionContext: ExecutionContext) {
  private implicit val log = Logging(system, this.getClass)

  private implicit def codecRegistry = fromRegistries(fromProviders(MongoClientSettings.getDefaultCodecRegistry(),
    IterableCodecProvider.apply,
    classOf[SequenceDocument],
    classOf[UserAccountProperties],
    classOf[ConsumerAccountProperties],
    classOf[ServerAccountInfo],
    classOf[PasswordHash],
    classOf[GitConfig],
    classOf[SourceConfig],
    classOf[ServiceSourcesConfig],
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
    classOf[DistributionServiceState],
    classOf[InstanceServiceState],
    classOf[ServiceState],
    classOf[UpdateError],
    classOf[ServiceLogLine],
    classOf[DistributionFaultReport],
    classOf[TestedVersions],
    classOf[LogLine],
    classOf[UploadStatus],
    classOf[UploadStatusDocument],
    classOf[FileInfo],
    classOf[FaultInfo],
    classOf[ServiceFaultReport],
    fromCodecs(FiniteDurationCodec, URLCodec)
  ))

  val Sequences = for {
    collection <- db.getOrCreateCollection[SequenceDocument]("collection.sequences")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true)) else Future()
  } yield collection

  val Accounts = new SequencedCollection[ServerAccountInfo]("accounts", for {
    collection <- db.getOrCreateCollection[BsonDocument]("accounts")
    _ <- if (createIndices) {
      collection.createIndex(Indexes.ascending("type", "_archiveTime"))
      collection.createIndex(Indexes.ascending("account", "_archiveTime"), new IndexOptions().unique(true))
    } else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val Sources = new SequencedCollection[ServiceSourcesConfig]("developer.sources", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.sources")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("service", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val Developer_Versions = new SequencedCollection[DeveloperVersionInfo]("developer.versions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.versions")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("service", "version", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val Developer_DesiredVersions = new SequencedCollection[DeveloperDesiredVersions]("developer.desiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.desiredVersions")
  } yield collection, Sequences, createIndex = createIndices)

  val Developer_ServiceProfiles = new SequencedCollection[ServicesProfile]("developer.serviceProfiles", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.serviceProfiles")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("profile", "_archiveTime"), new IndexOptions().unique(true)) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val Developer_TestedVersions = new SequencedCollection[TestedVersions]("developer.testedVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.testedVersions")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("profile")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("consumerDistribution", "_archiveTime"), new IndexOptions().unique(true)) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val Client_Versions = new SequencedCollection[ClientVersionInfo]("client.versions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.versions")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("service", "version", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val Client_DesiredVersions = new SequencedCollection[ClientDesiredVersions]("client.desiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.desiredVersions")
  } yield collection, Sequences, createIndex = createIndices)

  val Client_ProvidersInfo = new SequencedCollection[DistributionProviderInfo]("client.providers", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.providers")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution", "_archiveTime"), new IndexOptions().unique(true)) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val Consumers_InstalledDesiredVersions = new SequencedCollection[InstalledDesiredVersions]("consumers.installedDesiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("consumers.installedDesiredVersions")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val State_ServiceStates = new SequencedCollection[DistributionServiceState]("state.serviceStates", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.serviceStates")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("payload.instance")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("payload.service.date"), new IndexOptions()
      .expireAfter(instanceStateExpireTimeout.length, instanceStateExpireTimeout.unit)) else Future()
    _ <- collection.dropItems()
  } yield collection, Sequences, createIndex = createIndices)

  val State_UploadStatus = for {
    collection <- db.getOrCreateCollection[UploadStatusDocument]("state.uploadStatus")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("component"), new IndexOptions().unique(true)) else Future()
  } yield collection

  val Log_Lines = new SequencedCollection[ServiceLogLine]("log.lines", for {
    collection <- db.getOrCreateCollection[BsonDocument]("log.lines")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("service")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("instance")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("process")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("directory")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("payload.level")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("payload.time")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.descending("payload.time")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.text("payload.message")) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val Faults_ReportsInfo = new SequencedCollection[DistributionFaultReport]("faults.reports", for {
    collection <- db.getOrCreateCollection[BsonDocument]("faults.reports")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("payload.id")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("payload.info.service")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("payload.info.time")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.descending("payload.info.time")) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val Tasks_Info = new SequencedCollection[TaskInfo]("tasks.info", for {
    collection <- db.getOrCreateCollection[BsonDocument]("tasks.info")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("task")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("taskType")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("creationTime")) else Future()
  } yield collection, Sequences, createIndex = createIndices)

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
