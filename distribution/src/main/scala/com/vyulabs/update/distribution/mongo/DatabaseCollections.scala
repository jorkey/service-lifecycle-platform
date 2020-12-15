package com.vyulabs.update.distribution.mongo

import java.util.concurrent.TimeUnit

import com.mongodb.MongoClientSettings
import com.mongodb.client.model.{Filters, FindOneAndUpdateOptions, IndexOptions, Indexes, ReturnDocument, Updates}
import com.vyulabs.update.common.config.{DistributionClientConfig, DistributionClientInfo, DistributionClientProfile}
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.IterableCodecProvider
import org.mongodb.scala.bson.codecs.Macros._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class DatabaseCollections(db: MongoDb, instanceStateExpireSec: Int)(implicit executionContext: ExecutionContext) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit def codecRegistry = fromRegistries(fromProviders(MongoClientSettings.getDefaultCodecRegistry(),
    IterableCodecProvider.apply,
    classOf[SequenceDocument],
    classOf[ClientVersion],
    classOf[ClientDistributionVersion],
    classOf[DeveloperVersion],
    classOf[DeveloperDistributionVersion],
    classOf[BuildInfo],
    classOf[DeveloperVersionInfoDocument],
    classOf[ClientVersionInfo],
    classOf[ClientVersionInfoDocument],
    classOf[DeveloperDesiredVersion],
    classOf[DeveloperVersionInfo],
    classOf[DeveloperDesiredVersionsDocument],
    classOf[ClientDesiredVersion],
    classOf[ClientDesiredVersionsDocument],
    classOf[InstalledDesiredVersionsDocument],
    classOf[InstallInfo],
    classOf[DistributionClientProfile],
    classOf[DistributionClientProfileDocument],
    classOf[DistributionClientConfig],
    classOf[DistributionClientInfo],
    classOf[DistributionClientInfoDocument],
    classOf[DistributionServiceState],
    classOf[InstanceServiceState],
    classOf[ServiceState],
    classOf[ServiceStateDocument],
    classOf[ServiceLogLine],
    classOf[ServiceLogLineDocument],
    classOf[DistributionFaultReport],
    classOf[FaultReportDocument],
    classOf[TestedDesiredVersions],
    classOf[TestedDesiredVersionsDocument],
    classOf[TestSignature],
    classOf[LogLine],
    classOf[FaultInfo],
    classOf[UploadStatus],
    classOf[UploadStatusDocument],
    classOf[ServiceFaultReport]))

  val Sequences = for {
    collection <- db.getOrCreateCollection[SequenceDocument]("_.sequences")
    _ <- collection.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true))
  } yield collection

  val Developer_VersionsInfo = for {
    collection <- db.getOrCreateCollection[DeveloperVersionInfoDocument]("developer.versionsInfo")
    _ <- collection.createIndex(Indexes.ascending("info.serviceName", "info.version"), new IndexOptions().unique(true))
  } yield collection

  val Client_VersionsInfo = for {
    collection <- db.getOrCreateCollection[ClientVersionInfoDocument]("client.versionsInfo")
    _ <- collection.createIndex(Indexes.ascending("info.serviceName", "info.version"), new IndexOptions().unique(true))
  } yield collection

  val Developer_DesiredVersions = db.getOrCreateCollection[DeveloperDesiredVersionsDocument]("developer.desiredVersions")
  val Client_DesiredVersions = db.getOrCreateCollection[ClientDesiredVersionsDocument]("client.desiredVersions")

  val Developer_DistributionClientsInfo = for {
    collection <- db.getOrCreateCollection[DistributionClientInfoDocument]("developer.distributionClientsInfo")
    _ <- collection.createIndex(Indexes.ascending("info.distributionName"), new IndexOptions().unique(true))
  } yield collection

  val Developer_DistributionClientsProfiles = for {
    collection <- db.getOrCreateCollection[DistributionClientProfileDocument]("developer.distributionClientsProfiles")
    _ <- collection.createIndex(Indexes.ascending("profile.profileName"), new IndexOptions().unique(true))
  } yield collection

  val State_InstalledDesiredVersions = for {
    collection <- db.getOrCreateCollection[InstalledDesiredVersionsDocument]("state.installedDesiredVersions")
    _ <- collection.createIndex(Indexes.ascending("distributionName"))
  } yield collection

  val State_TestedVersions = for {
    collection <- db.getOrCreateCollection[TestedDesiredVersionsDocument]("state.testedDesiredVersions")
    _ <- collection.createIndex(Indexes.ascending("versions.profileName"))
  } yield collection

  val State_ServiceStates = for {
    collection <- db.getOrCreateCollection[ServiceStateDocument]("state.serviceStates")
    _ <- collection.createIndex(Indexes.ascending("sequence"), new IndexOptions().unique(true))
    _ <- collection.createIndex(Indexes.ascending("state.distributionName"))
    _ <- collection.createIndex(Indexes.ascending("state.instance.instanceId"))
    _ <- collection.createIndex(Indexes.ascending("state.instance.service.date"), new IndexOptions().expireAfter(instanceStateExpireSec, TimeUnit.SECONDS))
    _ <- collection.dropItems()
  } yield collection

  val State_ServiceLogs = for {
    collection <- db.getOrCreateCollection[ServiceLogLineDocument]("state.serviceLogs")
    _ <- collection.createIndex(Indexes.ascending("line.distributionName"))
  } yield collection

  val State_FaultReportsInfo = for {
    collection <- db.getOrCreateCollection[FaultReportDocument]("state.faultReportsInfo")
    _ <- collection.createIndex(Indexes.ascending("fault.distributionName"))
    _ <- collection.createIndex(Indexes.ascending("fault.report.faultId"))
    _ <- collection.createIndex(Indexes.ascending("fault.report.info.serviceName"))
  } yield collection

  val State_UploadStatus = for {
    collection <- db.getOrCreateCollection[UploadStatusDocument]("state.uploadStatus")
     _ <- collection.createIndex(Indexes.ascending("component"), new IndexOptions().unique(true))
  } yield collection

  def getNextSequence(sequenceName: String, increment: Int = 1): Future[Long] = {
    (for {
      sequences <- Sequences
      sequence <- { sequences.findOneAndUpdate(
        Filters.eq("name", sequenceName), Updates.inc("sequence", increment),
        new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)) }
    } yield sequence).map(_.map(_.sequence).head)
  }
}
