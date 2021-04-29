package com.vyulabs.update.distribution.mongo

import akka.actor.ActorSystem
import akka.event.Logging
import com.mongodb.MongoClientSettings
import com.mongodb.client.model._
import com.vyulabs.update.common.info.{DistributionProviderInfo, _}
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.users.{PasswordHash, ServerUserInfo, UserCredentials}
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
    classOf[ServerUserInfo],
    classOf[UserCredentials],
    classOf[PasswordHash],
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
    classOf[DistributionConsumerInfo],
    classOf[DistributionServiceState],
    classOf[InstanceServiceState],
    classOf[ServiceState],
    classOf[UpdateError],
    classOf[ServiceLogLine],
    classOf[DistributionFaultReport],
    classOf[TestedDesiredVersions],
    classOf[TestSignature],
    classOf[LogLine],
    classOf[FaultInfo],
    classOf[UploadStatus],
    classOf[UploadStatusDocument],
    classOf[ServiceFaultReport],
    fromCodecs(FiniteDurationCodec, URLCodec)
  ))

  val Sequences = for {
    collection <- db.getOrCreateCollection[SequenceDocument]("collection.sequences")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true)) else Future()
  } yield collection

  val Users = new SequencedCollection[ServerUserInfo]("users", for {
    collection <- db.getOrCreateCollection[BsonDocument]("users")
    _ <- if (createIndices) {
      collection.createIndex(Indexes.ascending("user", "_archiveTime"), new IndexOptions().unique(true))
      collection.createIndex(Indexes.ascending("human"))
    } else Future()
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

  val Developer_ConsumersInfo = new SequencedCollection[DistributionConsumerInfo]("developer.consumers", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.consumers")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution", "_archiveTime"), new IndexOptions().unique(true)) else Future()
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

  val State_InstalledDesiredVersions = new SequencedCollection[InstalledDesiredVersions]("state.installedDesiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.installedDesiredVersions")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val State_TestedVersions = new SequencedCollection[TestedDesiredVersions]("state.testedDesiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.testedDesiredVersions")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("servicesProfile")) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val State_ServiceStates = new SequencedCollection[DistributionServiceState]("state.serviceStates", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.serviceStates")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("instance.instance")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("instance.service.date"), new IndexOptions()
      .expireAfter(instanceStateExpireTimeout.length, instanceStateExpireTimeout.unit)) else Future()
    _ <- collection.dropItems()
  } yield collection, Sequences, createIndex = createIndices)

  val State_ServiceLogs = new SequencedCollection[ServiceLogLine]("state.serviceLogs", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.serviceLogs")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("line.distribution")) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val State_FaultReportsInfo = new SequencedCollection[DistributionFaultReport]("state.faultReportsInfo", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.faultReportsInfo")
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("distribution")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("report.faultId")) else Future()
    _ <- if (createIndices) collection.createIndex(Indexes.ascending("report.info.service")) else Future()
  } yield collection, Sequences, createIndex = createIndices)

  val State_UploadStatus = for {
    collection <- db.getOrCreateCollection[UploadStatusDocument]("state.uploadStatus")
     _ <- if (createIndices) collection.createIndex(Indexes.ascending("component"), new IndexOptions().unique(true)) else Future()
  } yield collection

  def init()(implicit executionContext: ExecutionContext): Future[Unit] = {
    val filters = Filters.eq("user", "admin")
    for {
      adminRecords <- Users.find(filters)
    } yield {
      if (adminRecords.isEmpty) {
        Users.insert(ServerUserInfo("admin", true, "Administrator", PasswordHash("admin"),
          Seq(UserRole.Administrator.toString), None, Seq.empty))
      } else {
        Future()
      }
    }
  }
}
