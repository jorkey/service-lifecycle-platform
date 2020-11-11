package distribution.mongo

import java.io.File
import java.util.concurrent.TimeUnit

import com.mongodb.MongoClientSettings
import com.mongodb.client.model.{Filters, FindOneAndUpdateOptions, IndexOptions, Indexes, ReturnDocument, Updates}
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.config.{ClientConfig, ClientInfo, ClientProfile}
import com.vyulabs.update.info._
import com.vyulabs.update.version.BuildVersion
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import org.mongodb.scala.bson.codecs.IterableCodecProvider
import org.mongodb.scala.bson.codecs.Macros._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class DatabaseCollections(db: MongoDb, instanceId: InstanceId,
                          homeDirectory: File, builderDirectory: Option[String], installerDirectory: Option[String],
                          instanceStateExpireSec: Int)(implicit executionContext: ExecutionContext) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  class BuildVersionCodec extends Codec[BuildVersion] {
    override def encode(writer: BsonWriter, value: BuildVersion, encoderContext: EncoderContext): Unit = {
      writer.writeString(value.toString)
    }

    override def decode(reader: BsonReader, decoderContext: DecoderContext): BuildVersion = {
      BuildVersion.parse(reader.readString())
    }

    override def getEncoderClass: Class[BuildVersion] = classOf[BuildVersion]
  }

  implicit def codecRegistry = fromRegistries(fromProviders(MongoClientSettings.getDefaultCodecRegistry(),
    IterableCodecProvider.apply,
    classOf[SequenceDocument],
    classOf[BuildInfo],
    classOf[DeveloperVersionInfoDocument],
    classOf[InstalledVersionInfo],
    classOf[InstalledVersionInfoDocument],
    classOf[DesiredVersion],
    classOf[DeveloperVersionInfo],
    classOf[DesiredVersionsDocument],
    classOf[PersonalDesiredVersionsDocument],
    classOf[InstalledDesiredVersionsDocument],
    classOf[InstallInfo],
    classOf[ClientProfile],
    classOf[ClientProfileDocument],
    classOf[ClientConfig],
    classOf[ClientInfo],
    classOf[ClientInfoDocument],
    classOf[ClientServiceState],
    classOf[InstanceServiceState],
    classOf[ServiceState],
    classOf[ServiceStateDocument],
    classOf[ServiceLogLine],
    classOf[ClientServiceLogLine],
    classOf[ServiceLogLineDocument],
    classOf[ClientFaultReport],
    classOf[FaultReportDocument],
    classOf[TestedDesiredVersions],
    classOf[TestedDesiredVersionsDocument],
    classOf[TestSignature],
    classOf[LogLine],
    classOf[FaultInfo],
    classOf[UploadStatusDocument],
    fromCodecs(new BuildVersionCodec())))

  val Sequences = for {
    collection <- db.getOrCreateCollection[SequenceDocument]("_.sequences")
    _ <- collection.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true))
  } yield collection

  val Developer_VersionsInfo = for {
    collection <- db.getOrCreateCollection[DeveloperVersionInfoDocument]("developer.versionsInfo")
    _ <- collection.createIndex(Indexes.ascending("info.serviceName", "info.clientName", "info.version"), new IndexOptions().unique(true))
  } yield collection

  val Client_VersionsInfo = for {
    collection <- db.getOrCreateCollection[InstalledVersionInfoDocument]("client.installedVersionsInfo")
    _ <- collection.createIndex(Indexes.ascending("info.serviceName", "info.version"), new IndexOptions().unique(true))
  } yield collection

  val Developer_DesiredVersions = db.getOrCreateCollection[DesiredVersionsDocument]("developer.desiredVersions")

  val Developer_PersonalDesiredVersions = db.getOrCreateCollection[PersonalDesiredVersionsDocument]("developer.personalDesiredVersions")

  val Client_DesiredVersions = for {
    collection <- db.getOrCreateCollection[DesiredVersionsDocument]("client.desiredVersions")
    _ <- collection.createIndex(Indexes.ascending("versions.clientName"))
  } yield collection

  val Developer_ClientsInfo = for {
    collection <- db.getOrCreateCollection[ClientInfoDocument]("developer.clientsInfo")
    _ <- collection.createIndex(Indexes.ascending("info.clientName"), new IndexOptions().unique(true))
  } yield collection

  val Developer_ClientsProfiles = for {
    collection <- db.getOrCreateCollection[ClientProfileDocument]("developer.clientsProfiles")
    _ <- collection.createIndex(Indexes.ascending("profile.profileName"), new IndexOptions().unique(true))
  } yield collection

  val State_InstalledDesiredVersions = for {
    collection <- db.getOrCreateCollection[InstalledDesiredVersionsDocument]("state.installedDesiredVersions")
    _ <- collection.createIndex(Indexes.ascending("clientName"))
  } yield collection

  val State_TestedVersions = for {
    collection <- db.getOrCreateCollection[TestedDesiredVersionsDocument]("state.testedDesiredVersions")
    _ <- collection.createIndex(Indexes.ascending("versions.profileName"))
  } yield collection

  val State_ServiceStates = for {
    collection <- db.getOrCreateCollection[ServiceStateDocument]("state.serviceStates")
    _ <- collection.createIndex(Indexes.ascending("sequence"), new IndexOptions().unique(true))
    _ <- collection.createIndex(Indexes.ascending("state.clientName"))
    _ <- collection.createIndex(Indexes.ascending("state.instance.instanceId"))
    _ <- collection.createIndex(Indexes.ascending("state.instance.service.date"), new IndexOptions().expireAfter(instanceStateExpireSec, TimeUnit.SECONDS))
    _ <- collection.dropItems()
    _ <- getNextSequence(collection.getName())
    _ <- {
      def addState(state: ClientServiceState): Future[com.mongodb.reactivestreams.client.Success] = {
        for {
          id <- getNextSequence(collection.getName())
          result <- collection.insert(ServiceStateDocument(id, state))
        } yield result
      }
      Future.sequence(Seq(
        addState(ClientServiceState(Common.OwnClient, instanceId,
          DirectoryServiceState.getServiceInstanceState(Common.DistributionServiceName, homeDirectory))),
        addState(ClientServiceState(Common.OwnClient, instanceId,
          DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, homeDirectory))),
      ) ++ builderDirectory.map(builderDirectory => Seq(
        addState(ClientServiceState(Common.OwnClient, instanceId,
          DirectoryServiceState.getServiceInstanceState(Common.BuilderServiceName, new File(homeDirectory, builderDirectory)))),
        addState(ClientServiceState(Common.OwnClient, instanceId,
          DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File(homeDirectory, builderDirectory)))))).getOrElse(Seq.empty)
        ++ installerDirectory.map(installerDirectory => Seq(
        addState(ClientServiceState(Common.OwnClient, instanceId,
          DirectoryServiceState.getServiceInstanceState(Common.InstallerServiceName, new File(homeDirectory, installerDirectory)))),
        addState(ClientServiceState(Common.OwnClient, instanceId,
          DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File(homeDirectory, installerDirectory)))))).getOrElse(Seq.empty)
      )
    }
  } yield collection

  val State_ServiceLogs = for {
    collection <- db.getOrCreateCollection[ServiceLogLineDocument]("state.serviceLogs")
    _ <- collection.createIndex(Indexes.ascending("log.clientName"))
  } yield collection

  val State_FaultReports = for {
    collection <- db.getOrCreateCollection[FaultReportDocument]("state.faultReports")
    _ <- collection.createIndex(Indexes.ascending("report.clientName"))
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
