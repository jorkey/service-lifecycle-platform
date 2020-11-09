package distribution.mongo

import java.io.File
import java.util.concurrent.TimeUnit

import com.mongodb.MongoClientSettings
import com.mongodb.client.model.{Filters, IndexOptions, Indexes, Updates}
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.config.ClientConfig
import com.vyulabs.update.info._
import com.vyulabs.update.version.BuildVersion
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import org.mongodb.scala.bson.codecs.IterableCodecProvider
import org.mongodb.scala.bson.codecs.Macros._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class DatabaseCollections(db: MongoDb, instanceId: InstanceId, homeDirectory: File, builderDirectory: Option[String],
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

  case class Sequence(name: String, sequence: Long)

  implicit def codecRegistry = fromRegistries(fromProviders(MongoClientSettings.getDefaultCodecRegistry(),
    IterableCodecProvider.apply,
    classOf[Sequence],
    classOf[BuildInfo],
    classOf[DeveloperVersionInfoDocument],
    classOf[InstalledVersionInfoDocument],
    classOf[DesiredVersion],
    classOf[DesiredVersionsDocument],
    classOf[PersonalDesiredVersionsDocument],
    classOf[InstalledDesiredVersionsDocument],
    classOf[InstallInfo],
    classOf[ClientProfileDocument],
    classOf[ClientConfig],
    classOf[ClientInfoDocument],
    classOf[ServiceState],
    classOf[ServiceStateDocument],
    classOf[ServiceLogLineDocument],
    classOf[FaultReportDocument],
    classOf[TestedDesiredVersionsDocument],
    classOf[TestSignature],
    classOf[LogLine],
    classOf[FaultInfo],
    classOf[UploadStatusDocument],
    fromCodecs(new BuildVersionCodec())))

  val Developer_VersionsInfo = db.getOrCreateCollection[DeveloperVersionInfoDocument]("developer.versionsInfo")
  val Developer_DesiredVersions = db.getOrCreateCollection[DesiredVersionsDocument]("developer.desiredVersions")
  val Developer_PersonalDesiredVersions = db.getOrCreateCollection[PersonalDesiredVersionsDocument]("developer.personalDesiredVersions")
  val Developer_ClientsInfo = db.getOrCreateCollection[ClientInfoDocument]("developer.clientsInfo")
  val Developer_ClientsProfiles = db.getOrCreateCollection[ClientProfileDocument]("developer.clientsProfiles")

  val Client_VersionsInfo = db.getOrCreateCollection[InstalledVersionInfoDocument]("client.installedVersionsInfo")
  val Client_DesiredVersions = db.getOrCreateCollection[DesiredVersionsDocument]("client.desiredVersions")

  val State_InstalledDesiredVersions = db.getOrCreateCollection[InstalledDesiredVersionsDocument]("state.installedDesiredVersions")
  val State_TestedVersions = db.getOrCreateCollection[TestedDesiredVersionsDocument]("state.testedDesiredVersions")
  val State_ServiceStates = db.getOrCreateCollection[ServiceStateDocument]("state.serviceStates")
  val State_ServiceLogs = db.getOrCreateCollection[ServiceLogLineDocument]("state.serviceLogs")
  val State_FaultReports = db.getOrCreateCollection[FaultReportDocument]("state.faultReports")
  val State_UploadStatus = db.getOrCreateCollection[UploadStatusDocument]("state.uploadStatus")

  val Sequences = db.getOrCreateCollection[Sequence]("sequences")

  val result = for {
    _ <- Developer_VersionsInfo.map(_.createIndex(Indexes.ascending("serviceName", "clientName", "version"), new IndexOptions().unique(true)))
    _ <- Developer_DesiredVersions
    _ <- Developer_PersonalDesiredVersions.map(_.createIndex(Indexes.ascending("clientName")))
    _ <- Developer_ClientsInfo.map(_.createIndex(Indexes.ascending("clientName"), new IndexOptions().unique(true)))
    _ <- Developer_ClientsProfiles.map(_.createIndex(Indexes.ascending("profileName"), new IndexOptions().unique(true)))

    _ <- Client_VersionsInfo.map(_.createIndex(Indexes.ascending("serviceName")))
    _ <- Client_DesiredVersions

    _ <- State_InstalledDesiredVersions.map(_.createIndex(Indexes.ascending("clientName")))
    _ <- State_TestedVersions.map(_.createIndex(Indexes.ascending("profileName")))
    serviceStates <- State_ServiceStates
    _ <- {
      Future.sequence(Seq(
        serviceStates.createIndex(Indexes.ascending("clientName")),
        serviceStates.createIndex(Indexes.ascending("instanceId")),
        serviceStates.createIndex(Indexes.ascending("state.date"),
          new IndexOptions().expireAfter(instanceStateExpireSec, TimeUnit.SECONDS))))
    }
    _ <- State_FaultReports.map(_.createIndex(Indexes.ascending("clientName", "faultInfo.serviceName")))

    _ <- {
      Future.sequence(Seq(
          serviceStates.insert(
            ServiceStateDocument(ClientServiceState(Common.OwnClient, instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.DistributionServiceName, homeDirectory)))),
          serviceStates.insert(
            ServiceStateDocument(ClientServiceState(Common.OwnClient, instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, homeDirectory)))),
        ) ++ builderDirectory.map(builderDirectory => Seq(
          serviceStates.insert(
            ServiceStateDocument(ClientServiceState(Common.OwnClient, instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.BuilderServiceName, new File(homeDirectory, builderDirectory))))),
          serviceStates.insert(
            ServiceStateDocument(ClientServiceState(Common.OwnClient, instanceId,
              DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File(homeDirectory, builderDirectory))))))).getOrElse(Seq.empty)
      )
    }
  } yield ()

  result.foreach(_ => log.info("Collections are ready"))

  def getNextSequence(sequenceName: String, increment: Int = 1): Future[Long] = {
    (for {
      sequences <- Sequences
      sequence <- sequences.findOneAndUpdate(
        Filters.eq("name", sequenceName), Updates.inc("sequence", increment))
    } yield sequence).map(_.map(_.sequence).headOption.getOrElse(0))
  }
}
