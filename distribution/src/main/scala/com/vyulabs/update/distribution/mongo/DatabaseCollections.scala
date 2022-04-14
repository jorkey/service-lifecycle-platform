package com.vyulabs.update.distribution.mongo

import akka.actor.ActorSystem
import akka.event.Logging
import com.mongodb.MongoClientSettings
import com.mongodb.client.model._
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

class DatabaseCollections(db: MongoDb,
                          serviceStatesExpireTimeout: FiniteDuration,
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
    classOf[TaskInfo],
    classOf[TaskParameter],
    classOf[ServiceFaultReport],
    classOf[BuildServiceConfig],
    classOf[NamedStringValue],
    fromCodecs(FiniteDurationCodec, URLCodec)
  ))

  val Accounts = new SequencedCollection[ServerAccountInfo]("accounts", for {
    collection <- db.getOrCreateCollection[BsonDocument]("accounts")
    _ <- if (createIndices) {
      collection.createIndex(Indexes.ascending("type", "_archiveTime"))
      collection.createIndex(Indexes.ascending("account", "_archiveTime"),
        new IndexOptions().unique(true))
    } else Future()
  } yield collection, createIndex = createIndices)

  val Developer_BuildServices = new SequencedCollection[BuildServiceConfig]("developer.buildServices", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.services")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("service", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
  } yield collection, createIndex = createIndices)

  val Developer_Versions = new SequencedCollection[DeveloperVersionInfo]("developer.versions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.versions")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("service", "version", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
  } yield collection, createIndex = createIndices)

  val Developer_DesiredVersions = new SequencedCollection[DeveloperDesiredVersions]("developer.desiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.desiredVersions")
  } yield collection, createIndex = createIndices)

  val Developer_ServiceProfiles = new SequencedCollection[ServicesProfile]("developer.serviceProfiles", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.serviceProfiles")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("profile", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
  } yield collection, createIndex = createIndices)

  val Developer_TestedVersions = new SequencedCollection[TestedVersions]("developer.testedVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.testedVersions")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("profile")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("consumerDistribution", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
  } yield collection, createIndex = createIndices)

  val Client_BuildServices = new SequencedCollection[BuildServiceConfig]("client.buildServices", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.services")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("service", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
  } yield collection, createIndex = createIndices)

  val Client_Versions = new SequencedCollection[ClientVersionInfo]("client.versions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.versions")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("service", "version", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
  } yield collection, createIndex = createIndices)

  val Client_DesiredVersions = new SequencedCollection[ClientDesiredVersions]("client.desiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.desiredVersions")
  } yield collection, createIndex = createIndices)

  val Client_ProvidersInfo = new SequencedCollection[DistributionProviderInfo]("client.providers", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.providers")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
  } yield collection, createIndex = createIndices)

  val Consumers_InstalledDesiredVersions = new SequencedCollection[InstalledDesiredVersions]("consumers.installedDesiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("consumers.installedDesiredVersions")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
  } yield collection, createIndex = createIndices)

  val State_ServiceStates = new SequencedCollection[DistributionServiceState]("state.serviceStates", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.serviceStates")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("instance")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("service")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("directory")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("state.time"), new IndexOptions()
      .expireAfter(serviceStatesExpireTimeout.length, serviceStatesExpireTimeout.unit)) else Future()
    _ <- collection.dropItems()
  } yield collection, createIndex = createIndices)

  val State_UploadStatus = for {
    collection <- db.getOrCreateCollection[UploadStatusDocument]("state.uploadStatus")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("component", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
  } yield collection

  val Log_Lines = new SequencedCollection[ServiceLogLine]("log.lines", for {
    collection <- db.getOrCreateCollection[BsonDocument]("log.lines")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("service")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("instance")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("directory")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("process")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("task")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("time")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.descending("time")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("level")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("unit")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.compoundIndex(
      Indexes.ascending("service"), Indexes.ascending("_sequence"),
      Indexes.ascending("instance"), Indexes.ascending("directory"), Indexes.ascending("process"),
      Indexes.ascending("time"), Indexes.ascending("level"), Indexes.ascending("unit"))) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.compoundIndex(
      Indexes.ascending("_sequence"), Indexes.ascending("_sequence"), Indexes.ascending("task"),
      Indexes.ascending("time"), Indexes.ascending("level"), Indexes.ascending("unit"))) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.text("message")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("expireTime"), new IndexOptions()
      .expireAfter(0, TimeUnit.SECONDS)) else Future()
  } yield collection, createIndex = createIndices)

  val Faults_ReportsInfo = new SequencedCollection[DistributionFaultReport]("faults.reports", for {
    collection <- db.getOrCreateCollection[BsonDocument]("faults.reports")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("fault")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("info.service")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("info.time")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.descending("info.time")) else Future()
  } yield collection, createIndex = createIndices)

  val Tasks_Info = new SequencedCollection[TaskInfo]("tasks.info", for {
    collection <- db.getOrCreateCollection[BsonDocument]("tasks.info")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("task", "_archiveTime"),
      new IndexOptions().unique(true)) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("type")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("services")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("creationTime")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.descending("creationTime")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("terminationTime")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("terminationStatus")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("expireTime"), new IndexOptions()
      .expireAfter(0, TimeUnit.SECONDS)) else Future()
  } yield collection, createIndex = createIndices)

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
