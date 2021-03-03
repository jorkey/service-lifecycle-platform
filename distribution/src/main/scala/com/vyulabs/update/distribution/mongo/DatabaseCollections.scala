package com.vyulabs.update.distribution.mongo

import akka.actor.ActorSystem
import akka.event.Logging
import com.mongodb.MongoClientSettings
import com.mongodb.client.model._
import com.vyulabs.update.common.config.{DistributionClientConfig, DistributionClientInfo, DistributionClientProfile}
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.users.{PasswordHash, ServerUserInfo, UserCredentials}
import org.bson.BsonDocument
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.IterableCodecProvider
import org.mongodb.scala.bson.codecs.Macros._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class DatabaseCollections(db: MongoDb, instanceStateExpireTimeout: FiniteDuration)
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
    classOf[DistributionClientProfile],
    classOf[DistributionClientConfig],
    classOf[DistributionClientInfo],
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
    classOf[ServiceFaultReport]))

  val Sequences = for {
    collection <- db.getOrCreateCollection[SequenceDocument]("sequences")
    _ <- collection.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true))
  } yield collection

  val Users_Info = new SequencedCollection[ServerUserInfo]("usersInfo", for {
    collection <- db.getOrCreateCollection[BsonDocument]("usersInfo")
    _ <- collection.createIndex(Indexes.ascending("userName", "_archiveTime"), new IndexOptions().unique(true))
  } yield collection, Sequences)

  val Developer_VersionsInfo = new SequencedCollection[DeveloperVersionInfo]("developer.versionsInfo", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.versionsInfo")
    _ <- collection.createIndex(Indexes.ascending("serviceName", "version", "_archiveTime"), new IndexOptions().unique(true))
  } yield collection, Sequences)

  val Client_VersionsInfo = new SequencedCollection[ClientVersionInfo]("client.versionsInfo", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.versionsInfo")
    _ <- collection.createIndex(Indexes.ascending("serviceName", "version", "_archiveTime"), new IndexOptions().unique(true))
  } yield collection, Sequences)

  val Developer_DesiredVersions = new SequencedCollection[DeveloperDesiredVersions]("developer.desiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.desiredVersions")
  } yield collection, Sequences)

  val Client_DesiredVersions = new SequencedCollection[ClientDesiredVersions]("client.desiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("client.desiredVersions")
  } yield collection, Sequences)

  val Developer_DistributionClientsInfo = new SequencedCollection[DistributionClientInfo]("developer.distributionClientsInfo", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.distributionClientsInfo")
    _ <- collection.createIndex(Indexes.ascending("distributionName", "_archiveTime"), new IndexOptions().unique(true))
  } yield collection, Sequences)

  val Developer_DistributionClientsProfiles = new SequencedCollection[DistributionClientProfile]("developer.distributionClientsProfiles", for {
    collection <- db.getOrCreateCollection[BsonDocument]("developer.distributionClientsProfiles")
    _ <- collection.createIndex(Indexes.ascending("profileName", "_archiveTime"), new IndexOptions().unique(true))
  } yield collection, Sequences)

  val State_InstalledDesiredVersions = new SequencedCollection[InstalledDesiredVersions]("state.installedDesiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.installedDesiredVersions")
    _ <- collection.createIndex(Indexes.ascending("distributionName"))
  } yield collection, Sequences)

  val State_TestedVersions = new SequencedCollection[TestedDesiredVersions]("state.testedDesiredVersions", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.testedDesiredVersions")
    _ <- collection.createIndex(Indexes.ascending("profileName"))
  } yield collection, Sequences)

  val State_ServiceStates = new SequencedCollection[DistributionServiceState]("state.serviceStates", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.serviceStates")
    _ <- collection.createIndex(Indexes.ascending("distributionName"))
    _ <- collection.createIndex(Indexes.ascending("instance.instanceId"))
    _ <- collection.createIndex(Indexes.ascending("instance.service.date"), new IndexOptions()
      .expireAfter(instanceStateExpireTimeout.length, instanceStateExpireTimeout.unit))
    _ <- collection.dropItems()
  } yield collection, Sequences)

  val State_ServiceLogs = new SequencedCollection[ServiceLogLine]("state.serviceLogs", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.serviceLogs")
    _ <- collection.createIndex(Indexes.ascending("line.distributionName"))
  } yield collection, Sequences)

  val State_FaultReportsInfo = new SequencedCollection[DistributionFaultReport]("state.faultReportsInfo", for {
    collection <- db.getOrCreateCollection[BsonDocument]("state.faultReportsInfo")
    _ <- collection.createIndex(Indexes.ascending("distributionName"))
    _ <- collection.createIndex(Indexes.ascending("report.faultId"))
    _ <- collection.createIndex(Indexes.ascending("report.info.serviceName"))
  } yield collection, Sequences)

  val State_UploadStatus = for {
    collection <- db.getOrCreateCollection[UploadStatusDocument]("state.uploadStatus")
     _ <- collection.createIndex(Indexes.ascending("component"), new IndexOptions().unique(true))
  } yield collection

  def init()(implicit executionContext: ExecutionContext): Future[Unit] = {
    val filters = Filters.eq("userName", "admin")
    for {
      adminRecords <- Users_Info.find(filters)
    } yield {
      if (adminRecords.isEmpty) {
        Users_Info.insert(ServerUserInfo("admin", UserRole.Administrator.toString, PasswordHash("admin")))
      } else {
        Future()
      }
    }
  }
}
